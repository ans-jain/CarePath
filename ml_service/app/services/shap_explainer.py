from typing import Dict, Any, List, Optional, Union
import numpy as np
import shap

from app.core.logging import get_logger
from app.models.feature_vector import FEATURE_COLUMNS, FeatureVector
from app.models.explainability import FeatureAttribution, ShapExplanation
from app.core.feature_metadata import (
    get_feature_display_name,
    get_feature_unit,
    format_feature_narrative,
)
from app.services.model_registry import model_registry

logger = get_logger("carepath.shap_explainer")

class ShapExplainerService:
    """
    Dedicated Explainability Service utilizing TreeExplainer on the trained
    HistGradientBoosting ensemble to generate exact Shapley feature attributions.
    """

    def __init__(self):
        self._explainers: Optional[List[shap.TreeExplainer]] = None
        self._base_value: Optional[float] = None
        self._cached_model_id: Optional[int] = None

    def _initialize_explainers(self) -> None:
        """
        Extracts underlying tree estimators from the model (including CalibratedClassifierCV
        cross-validation folds) and constructs cached TreeExplainer instances.
        """
        if not model_registry.is_loaded:
            raise RuntimeError("Model is not loaded in model_registry. Cannot initialize SHAP explainer.")

        model = model_registry.model
        current_model_id = id(model)

        if self._explainers is not None and self._cached_model_id == current_model_id:
            return

        logger.info("Initializing TreeExplainer instances for model ensemble...")
        estimators = []

        if hasattr(model, "calibrated_classifiers_"):
            # CalibratedClassifierCV fold models
            for cc in model.calibrated_classifiers_:
                est = getattr(cc, "estimator", getattr(cc, "base_estimator", None))
                if est is not None:
                    estimators.append(est)
        elif hasattr(model, "estimator"):
            estimators.append(model.estimator)
        else:
            estimators.append(model)

        if not estimators:
            raise ValueError(f"Could not extract tree estimators from model type: {type(model)}")

        try:
            explainers = []
            base_values = []
            for idx, est in enumerate(estimators):
                exp = shap.TreeExplainer(est)
                explainers.append(exp)
                bv = exp.expected_value
                if isinstance(bv, (list, np.ndarray)):
                    bv = float(bv[0])
                else:
                    bv = float(bv)
                base_values.append(bv)

            self._explainers = explainers
            self._base_value = float(np.mean(base_values))
            self._cached_model_id = current_model_id
            logger.info("Successfully initialized %d TreeExplainer instances. Mean base value: %.4f",
                        len(explainers), self._base_value)

        except Exception as e:
            logger.exception("Failed to initialize SHAP TreeExplainer: %s", str(e))
            self._explainers = None
            self._base_value = None
            self._cached_model_id = None
            raise RuntimeError(f"SHAP TreeExplainer initialization failed: {e}") from e

    def clear_cache(self) -> None:
        """Clears the cached explainers (called when model is unloaded)."""
        self._explainers = None
        self._base_value = None
        self._cached_model_id = None

    def explain(
        self,
        X_proc: np.ndarray,
        raw_features: Union[FeatureVector, Dict[str, Any]],
        top_k: int = 5
    ) -> ShapExplanation:
        """
        Computes SHAP values, maps feature directions, and returns ranked risk drivers
        and protective factors.
        """
        self._initialize_explainers()

        if self._explainers is None or self._base_value is None:
            raise RuntimeError("SHAP explainers are not initialized.")

        # Ensure X_proc is 2D
        if X_proc.ndim == 1:
            X_proc = X_proc.reshape(1, -1)

        # 1. Compute SHAP values across all fold estimators and average
        all_shap = []
        for exp in self._explainers:
            sv = exp.shap_values(X_proc)
            if isinstance(sv, list):
                # Binary classification format where class 1 is index 1
                sv = sv[1] if len(sv) > 1 else sv[0]
            all_shap.append(sv)

        # Mean attribution across folds: shape (1, 18) -> (18,)
        mean_shap = np.mean(all_shap, axis=0)[0]

        # Extract raw features dictionary for display
        if isinstance(raw_features, FeatureVector):
            raw_dict = raw_features.model_dump()
        elif isinstance(raw_features, dict):
            raw_dict = raw_features.get("features", raw_features)
        else:
            raw_dict = {}

        # 2. Map SHAP values to features
        shap_values_dict: Dict[str, float] = {}
        all_attributions: List[FeatureAttribution] = []

        for idx, col in enumerate(FEATURE_COLUMNS):
            phi = float(mean_shap[idx])
            shap_values_dict[col] = round(phi, 4)

            raw_val = raw_dict.get(col, None)
            display_name = get_feature_display_name(col)

            # Determine direction
            if phi > 1e-4:
                direction = "increases_risk"
            elif phi < -1e-4:
                direction = "decreases_risk"
            else:
                direction = "neutral"

            # Determine impact magnitude
            abs_phi = abs(phi)
            if abs_phi >= 0.20:
                impact = "high"
            elif abs_phi >= 0.08:
                impact = "medium"
            else:
                impact = "low"

            narrative = format_feature_narrative(col, raw_val, phi, direction)

            attribution = FeatureAttribution(
                feature=col,
                display_name=display_name,
                value=raw_val,
                shap_value=round(phi, 4),
                direction=direction,
                impact=impact,
                narrative=narrative
            )
            all_attributions.append(attribution)

        # 3. Categorize into risk drivers (phi > 0) and protective factors (phi < 0)
        risk_drivers = [a for a in all_attributions if a.direction == "increases_risk"]
        protective_factors = [a for a in all_attributions if a.direction == "decreases_risk"]

        # Sort drivers by SHAP magnitude descending
        risk_drivers.sort(key=lambda a: abs(a.shap_value), reverse=True)
        protective_factors.sort(key=lambda a: abs(a.shap_value), reverse=True)

        # Apply top-k limit
        k = max(1, min(top_k, len(FEATURE_COLUMNS)))
        top_drivers = risk_drivers[:k]
        top_protective = protective_factors[:k]

        # 4. Construct overall summary narrative
        summary_parts = []
        if top_drivers:
            top_d_names = [d.display_name for d in top_drivers[:2]]
            summary_parts.append(
                f"Modeled risk is predominantly influenced by {', '.join(top_d_names)}"
            )
        if top_protective:
            top_p_names = [p.display_name for p in top_protective[:2]]
            summary_parts.append(
                f"buffering factors include {', '.join(top_p_names)}"
            )

        if summary_parts:
            summary_narrative = (
                f"{'; '.join(summary_parts)}. "
                "Feature attributions explain model decision weighting and do not represent medical conclusions."
            )
        else:
            summary_narrative = "Biomarkers fall near baseline cohort expectations."

        return ShapExplanation(
            base_value=round(self._base_value, 4),
            shap_values=shap_values_dict,
            top_risk_drivers=top_drivers,
            protective_factors=top_protective,
            summary_narrative=summary_narrative
        )

shap_explainer = ShapExplainerService()
