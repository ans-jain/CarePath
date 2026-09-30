from fastapi import APIRouter, HTTPException, status
from app.models.explainability import ExplainRiskRequest, ExplainRiskResponse
from app.services.explainability_service import explainability_service
from app.services.model_registry import model_registry

router = APIRouter(tags=["Explainable AI & Counterfactuals"])

@router.post(
    "/ml/v1/risk/explain",
    response_model=ExplainRiskResponse,
    status_code=status.HTTP_200_OK,
    summary="Explain Cardiometabolic Risk & Generate Counterfactual Targets",
    description=(
        "Executes patient-level explainable AI: computes calibrated risk signal, "
        "generates exact TreeSHAP feature attributions separating risk drivers from protective factors, "
        "and computes actionable, physiologically constrained counterfactual lifestyle targets."
    )
)
@router.post(
    "/explain",
    response_model=ExplainRiskResponse,
    status_code=status.HTTP_200_OK,
    include_in_schema=False
)
def explain_risk(request: ExplainRiskRequest) -> ExplainRiskResponse:
    if not model_registry.is_loaded:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="ML inference model is not loaded. Ensure training pipeline has been executed."
        )

    try:
        response = explainability_service.explain_risk(request)
        return response
    except ValueError as ve:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=f"Invalid feature inputs: {str(ve)}"
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Explainability computation error: {str(e)}"
        )
