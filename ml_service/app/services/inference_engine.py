from typing import Union, Dict, Any
import numpy as np

from app.core.config import settings
from app.core.logging import get_logger
from app.models.feature_vector import FeatureVector, PredictRiskRequest
from app.models.prediction_result import PredictRiskResponse
from app.services.model_registry import model_registry

logger = get_logger("carepath.inference_engine")

class InferenceEngine:
    """
    Core ML inference component executing calibrated probability estimation and risk tiering.
    """

    @staticmethod
    def categorize_risk(score: float) -> str:
        """
        Maps continuous risk signal in [0.0, 1.0] to clinical decision-support tiers (FR-7.2).
        """
        if score < 0.25:
            return "LOW"
        elif score < 0.50:
            return "MODERATE"
        elif score < 0.75:
            return "ELEVATED"
        else:
            return "HIGH"

    def predict(self, request: Union[PredictRiskRequest, FeatureVector, Dict[str, Any]]) -> PredictRiskResponse:
        """
        Executes end-to-end inference over validated feature input.
        """
        if not model_registry.is_loaded:
            logger.error("Inference requested but model is not loaded.")
            raise RuntimeError("Model is not loaded. Ensure training artifacts are present.")

        # Extract feature vector and evaluation mode
        if hasattr(request, "features") and isinstance(request.features, FeatureVector):
            features = request.features
            eval_mode = getattr(request, "evaluation_mode", "LONGITUDINAL") or "LONGITUDINAL"
        elif isinstance(request, FeatureVector):
            features = request
            eval_mode = "LONGITUDINAL"
        elif isinstance(request, dict):
            if "features" in request:
                features = FeatureVector(**request["features"])
                eval_mode = request.get("evaluation_mode", "LONGITUDINAL")
            else:
                features = FeatureVector(**request)
                eval_mode = "LONGITUDINAL"
        else:
            raise ValueError(f"Unsupported request type: {type(request)}")

        # 1. Preprocess input through persisted training pipeline
        preprocessor = model_registry.preprocessor
        model = model_registry.model
        metadata = model_registry.metadata

        X_proc = preprocessor.transform(features)

        # 2. Run Calibrated Probability Prediction
        probabilities = model.predict_proba(X_proc)
        # Class 1 probability represents adverse decompensation risk signal
        raw_prob = float(probabilities[0, 1]) if probabilities.shape[1] > 1 else float(probabilities[0, 0])
        risk_score = float(np.clip(raw_prob, 0.0, 1.0))

        # 3. Stratify Risk Tier
        category = self.categorize_risk(risk_score)
        prediction_flag = 1 if risk_score >= 0.50 else 0

        # 4. Confidence Level
        if eval_mode in ["INTAKE", "PRELIMINARY_INTAKE"]:
            confidence = "PRELIMINARY_INTAKE"
        else:
            confidence = "LONGITUDINAL_ROBUST"

        model_ver = metadata.get("model_version", settings.MODEL_VERSION)

        return PredictRiskResponse(
            model_version=model_ver,
            overall_risk_score=round(risk_score, 4),
            risk_category=category,
            prediction=prediction_flag,
            probability=round(risk_score, 4),
            calibrated=True,
            confidence_level=confidence,
            regulatory_disclaimer=settings.REGULATORY_DISCLAIMER
        )

inference_engine = InferenceEngine()
