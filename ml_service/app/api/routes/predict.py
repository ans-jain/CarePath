from fastapi import APIRouter, HTTPException, status
from app.models.feature_vector import PredictRiskRequest
from app.models.prediction_result import PredictRiskResponse
from app.services.inference_engine import inference_engine
from app.services.model_registry import model_registry

router = APIRouter(tags=["Cardiometabolic Risk Inference"])

@router.post(
    "/ml/v1/risk/predict",
    response_model=PredictRiskResponse,
    status_code=status.HTTP_200_OK,
    summary="Evaluate Cardiometabolic Risk Signal",
    description=(
        "Consumes an 18-dimensional longitudinal biomarker feature vector and computes a "
        "well-calibrated multi-system cardiometabolic risk signal in [0.00, 1.00] mapped into "
        "decision-support tiers (LOW, MODERATE, ELEVATED, HIGH). "
        "NOTE: Outputs represent statistical risk signals, NOT medical diagnoses or clinical decisions."
    )
)
@router.post(
    "/predict",
    response_model=PredictRiskResponse,
    status_code=status.HTTP_200_OK,
    include_in_schema=False
)
def predict_risk(request: PredictRiskRequest) -> PredictRiskResponse:
    if not model_registry.is_loaded:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="ML inference model is not loaded. Ensure training pipeline has been executed."
        )

    try:
        response = inference_engine.predict(request)
        return response
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Inference computation error: {str(e)}"
        )
