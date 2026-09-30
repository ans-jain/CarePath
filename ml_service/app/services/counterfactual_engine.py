from typing import List, Dict, Any, Optional, Tuple
import copy
import numpy as np

from app.core.logging import get_logger
from app.models.feature_vector import FeatureVector
from app.models.prediction_result import PredictRiskResponse
from app.models.explainability import (
    CounterfactualFeatureTarget,
    CounterfactualPlan,
    CounterfactualResult
)
from app.core.feature_metadata import (
    FEATURE_METADATA,
    get_feature_display_name,
    get_feature_unit,
    is_feature_modifiable
)
from app.services.inference_engine import inference_engine

logger = get_logger("carepath.counterfactual_engine")

class CounterfactualEngine:
    """
    Constrained Counterfactual Recommendation Engine.
    Identifies realistic, actionable biomarker/lifestyle modifications that move
    the model's predicted cardiometabolic risk signal toward a lower risk category.
    """

    MIN_IMPROVEMENT_THRESHOLD: float = 0.01
    MAX_PLANS_TO_RETURN: int = 3

    def generate_counterfactuals(
        self,
        features: FeatureVector,
        current_prediction: PredictRiskResponse,
        evaluation_mode: str = "LONGITUDINAL"
    ) -> CounterfactualResult:
        """
        Executes constrained heuristic search over modifiable features to identify
        viable counterfactual plans.
        """
        orig_score = current_prediction.overall_risk_score
        orig_category = current_prediction.risk_category

        # Guardrail: If patient is already at optimal low risk, no counterfactual is required
        if orig_score < 0.20 or orig_category == "LOW":
            return CounterfactualResult(
                counterfactual_available=False,
                status="NO_VALID_COUNTERFACTUAL_FOUND",
                message=(
                    "Patient is currently in the LOW risk category. "
                    "No counterfactual risk reduction targets are required."
                ),
                plans=[]
            )

        feat_dict = features.model_dump()
        candidates: List[CounterfactualPlan] = []

        # 1. Evaluate single-feature modulations
        single_candidates = self._search_single_feature_modulations(
            feat_dict, orig_score, orig_category, evaluation_mode
        )
        candidates.extend(single_candidates)

        # 2. Evaluate dual-feature combined modulations (e.g. BP + Glucose, or Lifestyle: Sleep + BMI)
        dual_candidates = self._search_dual_feature_modulations(
            feat_dict, orig_score, orig_category, evaluation_mode
        )
        candidates.extend(dual_candidates)

        if not candidates:
            return CounterfactualResult(
                counterfactual_available=False,
                status="NO_VALID_COUNTERFACTUAL_FOUND",
                message=(
                    "No realistic biometric modifications within clinically safe bounds "
                    "yielded a significant reduction in the model's predicted risk signal."
                ),
                plans=[]
            )

        # 3. Rank candidates:
        # Priority 1: Achieves a risk tier transition (e.g. ELEVATED -> MODERATE or MODERATE -> LOW)
        # Priority 2: Greater absolute risk reduction
        # Priority 3: Fewer modified features (lower patient burden)
        candidates.sort(
            key=lambda p: (
                1 if p.tier_transition_achieved else 0,
                p.risk_reduction,
                -len(p.changed_features)
            ),
            reverse=True
        )

        # Select top distinct plans (avoid duplicate single-feature targets)
        selected_plans: List[CounterfactualPlan] = []
        seen_signatures = set()

        for c in candidates:
            sig = tuple(sorted([f.feature for f in c.changed_features]))
            if sig not in seen_signatures:
                selected_plans.append(c)
                seen_signatures.add(sig)
            if len(selected_plans) >= self.MAX_PLANS_TO_RETURN:
                break

        return CounterfactualResult(
            counterfactual_available=True,
            status="SUCCESS",
            message=f"Identified {len(selected_plans)} actionable counterfactual target plan(s).",
            plans=selected_plans
        )

    def _search_single_feature_modulations(
        self,
        base_dict: Dict[str, Any],
        orig_score: float,
        orig_category: str,
        evaluation_mode: str
    ) -> List[CounterfactualPlan]:
        candidates: List[CounterfactualPlan] = []

        # Modifiable priority candidates to test
        target_features = [
            "systolic_bp_current",
            "fasting_glucose_current",
            "bmi",
            "sleep_hours_current",
            "smoking_status",
            "diastolic_bp_current",
            "cholesterol_ldl",
            "triglycerides",
            "cholesterol_hdl"
        ]

        for feat_name in target_features:
            if not is_feature_modifiable(feat_name):
                continue

            current_val = base_dict.get(feat_name)
            if current_val is None:
                continue

            targets = self._generate_candidate_values(feat_name, current_val)
            for target_val in targets:
                # Construct simulated vector
                sim_dict = copy.deepcopy(base_dict)
                sim_dict[feat_name] = target_val

                try:
                    sim_features = FeatureVector(**sim_dict)
                    sim_pred = inference_engine.predict(sim_features)
                except Exception as e:
                    logger.debug("Candidate evaluation failed for %s=%s: %s", feat_name, target_val, str(e))
                    continue

                reduction = round(orig_score - sim_pred.overall_risk_score, 4)
                if reduction >= self.MIN_IMPROVEMENT_THRESHOLD:
                    tier_transition = sim_pred.risk_category != orig_category
                    disp_name = get_feature_display_name(feat_name)
                    unit = get_feature_unit(feat_name)
                    meta = FEATURE_METADATA.get(feat_name, {})
                    direction = meta.get("preferred_direction", "decrease")

                    # Calculate change delta if numerical
                    if isinstance(current_val, (int, float)) and isinstance(target_val, (int, float)):
                        change_delta = round(float(target_val) - float(current_val), 2)
                        curr_v = float(current_val)
                        targ_v = round(float(target_val), 2)
                    else:
                        change_delta = None
                        curr_v = str(current_val)
                        targ_v = str(target_val)

                    target_obj = CounterfactualFeatureTarget(
                        feature=feat_name,
                        display_name=disp_name,
                        current_value=curr_v,
                        target_value=targ_v,
                        unit=unit,
                        change_delta=change_delta,
                        allowed_range=[meta.get("min_bound", 0.0), meta.get("max_bound", 100.0)],
                        direction=direction,
                        modifiable=True
                    )

                    unit_str = f" {unit}" if unit else ""
                    if change_delta is not None:
                        delta_str = f"{abs(change_delta):.1f}{unit_str}"
                        dir_word = "Reducing" if change_delta < 0 else "Increasing"
                        action_str = f"{dir_word} modeled {disp_name} by {delta_str} (to {target_val}{unit_str})"
                    else:
                        action_str = f"Modifying modeled {disp_name} from {current_val} to {target_val}"

                    rationale = (
                        f"{action_str} is associated with a simulated risk score reduction of {reduction:.4f} "
                        f"(from {orig_score:.4f} to {sim_pred.overall_risk_score:.4f})."
                    )

                    plan = CounterfactualPlan(
                        plan_id=f"plan_single_{feat_name}",
                        title=f"{disp_name} Optimization",
                        simulated_risk_score=sim_pred.overall_risk_score,
                        simulated_risk_category=sim_pred.risk_category,
                        risk_reduction=reduction,
                        tier_transition_achieved=tier_transition,
                        changed_features=[target_obj],
                        rationale=rationale
                    )
                    candidates.append(plan)

        return candidates

    def _search_dual_feature_modulations(
        self,
        base_dict: Dict[str, Any],
        orig_score: float,
        orig_category: str,
        evaluation_mode: str
    ) -> List[CounterfactualPlan]:
        candidates: List[CounterfactualPlan] = []

        dual_pairs = [
            ("systolic_bp_current", "fasting_glucose_current", "Cardiometabolic Dual Target"),
            ("bmi", "sleep_hours_current", "Lifestyle & Rest Target"),
            ("systolic_bp_current", "diastolic_bp_current", "Comprehensive Blood Pressure Normalization")
        ]

        for f1, f2, title in dual_pairs:
            v1 = base_dict.get(f1)
            v2 = base_dict.get(f2)
            if v1 is None or v2 is None:
                continue

            t1_list = self._generate_candidate_values(f1, float(v1))
            t2_list = self._generate_candidate_values(f2, float(v2))

            if not t1_list or not t2_list:
                continue

            # Pick moderate target for each
            t1 = t1_list[-1]  # maximal safe step
            t2 = t2_list[-1]

            sim_dict = copy.deepcopy(base_dict)
            sim_dict[f1] = t1
            sim_dict[f2] = t2

            try:
                sim_features = FeatureVector(**sim_dict)
                sim_pred = inference_engine.predict(sim_features)
            except Exception:
                continue

            reduction = round(orig_score - sim_pred.overall_risk_score, 4)
            if reduction >= (self.MIN_IMPROVEMENT_THRESHOLD * 1.5):
                tier_transition = sim_pred.risk_category != orig_category

                m1 = FEATURE_METADATA.get(f1, {})
                m2 = FEATURE_METADATA.get(f2, {})

                t1_obj = CounterfactualFeatureTarget(
                    feature=f1,
                    display_name=get_feature_display_name(f1),
                    current_value=float(v1),
                    target_value=round(t1, 2),
                    unit=get_feature_unit(f1),
                    change_delta=round(t1 - float(v1), 2),
                    allowed_range=[m1.get("min_bound", 0.0), m1.get("max_bound", 100.0)],
                    direction=m1.get("preferred_direction", "decrease"),
                    modifiable=True
                )
                t2_obj = CounterfactualFeatureTarget(
                    feature=f2,
                    display_name=get_feature_display_name(f2),
                    current_value=float(v2),
                    target_value=round(t2, 2),
                    unit=get_feature_unit(f2),
                    change_delta=round(t2 - float(v2), 2),
                    allowed_range=[m2.get("min_bound", 0.0), m2.get("max_bound", 100.0)],
                    direction=m2.get("preferred_direction", "decrease"),
                    modifiable=True
                )

                rationale = (
                    f"Combined modeled optimization of {t1_obj.display_name} and {t2_obj.display_name} "
                    f"yields a simulated risk score reduction of {reduction:.4f} "
                    f"(from {orig_score:.4f} to {sim_pred.overall_risk_score:.4f})."
                )

                plan = CounterfactualPlan(
                    plan_id=f"plan_dual_{f1}_{f2}",
                    title=title,
                    simulated_risk_score=sim_pred.overall_risk_score,
                    simulated_risk_category=sim_pred.risk_category,
                    risk_reduction=reduction,
                    tier_transition_achieved=tier_transition,
                    changed_features=[t1_obj, t2_obj],
                    rationale=rationale
                )
                candidates.append(plan)

        return candidates

    def _generate_candidate_values(self, feature_name: str, current_val: Any) -> List[Any]:
        """
        Generates physiologically valid, non-harmful candidate step values for a modifiable feature.
        """
        meta = FEATURE_METADATA.get(feature_name)
        if not meta or not meta.get("is_modifiable", False):
            return []

        if feature_name == "smoking_status":
            val_str = str(current_val).strip().upper()
            if val_str in ["CURRENT", "2", "2.0"]:
                return ["FORMER", "NEVER"]
            elif val_str in ["FORMER", "1", "1.0"]:
                return ["NEVER"]
            return []

        try:
            curr_float = float(current_val)
        except (ValueError, TypeError):
            return []

        min_bound = meta.get("min_bound", 0.0)
        max_bound = meta.get("max_bound", 1000.0)
        step = meta.get("step_size", 1.0)
        max_delta = meta.get("max_delta", 20.0)
        preferred_direction = meta.get("preferred_direction", "decrease")

        candidates: List[float] = []

        if preferred_direction == "decrease":
            safe_min = max(min_bound, curr_float - max_delta)
            if curr_float > safe_min:
                # 1. Immediate step
                t1 = curr_float - step
                if t1 >= safe_min:
                    candidates.append(round(t1, 2))
                # 2. Stretch step
                t2 = curr_float - (2 * step)
                if t2 >= safe_min and t2 not in candidates:
                    candidates.append(round(t2, 2))
                # 3. Clinical normal target
                optimal = meta.get("optimal_target")
                if optimal is not None and curr_float > optimal:
                    t_opt = max(safe_min, optimal)
                    if t_opt not in candidates and t_opt < curr_float:
                        candidates.append(round(t_opt, 2))
                # 4. Safe minimum bound
                if safe_min not in candidates and safe_min < curr_float:
                    candidates.append(round(safe_min, 2))
        elif preferred_direction == "increase":
            safe_max = min(max_bound, curr_float + max_delta)
            if curr_float < safe_max:
                t1 = curr_float + step
                if t1 <= safe_max:
                    candidates.append(round(t1, 2))
                t2 = curr_float + (2 * step)
                if t2 <= safe_max and t2 not in candidates:
                    candidates.append(round(t2, 2))
                optimal = meta.get("optimal_target")
                if optimal is not None and curr_float < optimal:
                    t_opt = min(safe_max, optimal)
                    if t_opt not in candidates and t_opt > curr_float:
                        candidates.append(round(t_opt, 2))
                if safe_max not in candidates and safe_max > curr_float:
                    candidates.append(round(safe_max, 2))

        return candidates

counterfactual_engine = CounterfactualEngine()
