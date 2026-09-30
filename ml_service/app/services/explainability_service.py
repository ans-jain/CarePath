from typing import Union, Dict, Any

from app.core.config import settings
from app.core.logging import get_logger
from app.models.explainability import (
    ExplainRiskRequest,
    ExplainRiskResponse,
    CounterfactualResult
)
from app.services.model_registry import model_registry
from app.services.inference_engine import inference_engine
from app.services.shap_explainer import shap_explainer
from app.services.counterfactual_engine import counterfactual_engine

logger = get_logger("carepath.explainability_service")

class ExplainabilityService:
    """
    Unified Explainability Service coordinating:
    1. Calibrated risk prediction (via existing InferenceEngine)
    2. SHAP TreeExplainer feature attributions (via ShapExplainerService)
    3. Actionable constrained counterfactual targets (via CounterfactualEngine)
    """

    def explain_risk(self, request: ExplainRiskRequest) -> ExplainRiskResponse:
        """
        Executes end-to-end explainability pipeline for a patient feature vector.
        """
        if not model_registry.is_loaded:
            logger.error("Explainability requested but model is not loaded in registry.")
            raise RuntimeError("Model is not loaded. Ensure training artifacts are present.")

        # 1. Run core calibrated risk prediction (reusing existing inference engine)
        prediction = inference_engine.predict(request)

        # 2. Extract preprocessed feature representation for SHAP
        preprocessor = model_registry.preprocessor
        X_proc = preprocessor.transform(request.features)

        # 3. Compute SHAP explanations (TreeExplainer)
        top_k = request.top_k if request.top_k is not None else 5
        shap_explanation = shap_explainer.explain(
            X_proc=X_proc,
            raw_features=request.features,
            top_k=top_k
        )

        # 4. Generate constrained counterfactual targets
        if request.generate_counterfactuals:
            counterfactuals = counterfactual_engine.generate_counterfactuals(
                features=request.features,
                current_prediction=prediction,
                evaluation_mode=request.evaluation_mode or "LONGITUDINAL"
            )
        else:
            counterfactuals = CounterfactualResult(
                counterfactual_available=False,
                status="NO_VALID_COUNTERFACTUAL_FOUND",
                message="Counterfactual generation skipped by request option.",
                plans=[]
            )

        return ExplainRiskResponse(
            prediction=prediction,
            explanation=shap_explanation,
            counterfactuals=counterfactuals,
            regulatory_disclaimer=settings.REGULATORY_DISCLAIMER
        )

explainability_service = ExplainabilityService()
