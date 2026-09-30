from fastapi import APIRouter, status
from app.core.config import settings
from app.models.prediction_result import HealthResponse
from app.services.model_registry import model_registry

router = APIRouter(tags=["Health & Metadata"])

@router.get(
    "/ml/v1/health",
    response_model=HealthResponse,
    status_code=status.HTTP_200_OK,
    summary="Service Health & Model Status",
    description="Returns liveness status, active model version, and loaded feature dimension count."
)
@router.get(
    "/health",
    response_model=HealthResponse,
    status_code=status.HTTP_200_OK,
    include_in_schema=False
)
def get_health() -> HealthResponse:
    loaded = model_registry.is_loaded
    meta = model_registry.metadata
    model_ver = meta.get("model_version", settings.MODEL_VERSION if loaded else "NONE")

    return HealthResponse(
        status="HEALTHY" if loaded else "DEGRADED",
        service_name=settings.SERVICE_NAME,
        service_version=settings.SERVICE_VERSION,
        modelVersion=model_ver,
        model_loaded=loaded,
        calibrated=meta.get("calibrated", True if loaded else False),
        shapExplainerType="TreeExplainer" if loaded else "NONE",
        featuresSupported=18
    )

@router.get(
    "/ml/v1/health/ready",
    status_code=status.HTTP_200_OK,
    summary="Readiness Probe",
    description="Returns 200 if ML model is ready to serve inference, 503 otherwise."
)
@router.get(
    "/health/ready",
    status_code=status.HTTP_200_OK,
    include_in_schema=False
)
def get_readiness():
    from fastapi import HTTPException
    loaded = model_registry.is_loaded
    if not loaded:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail={
                "status": "DOWN",
                "ready": False,
                "service": settings.SERVICE_NAME,
                "message": "Model artifacts are not loaded or initialized."
            }
        )
    return {
        "status": "UP",
        "ready": True,
        "service": settings.SERVICE_NAME,
        "modelVersion": model_registry.metadata.get("model_version", settings.MODEL_VERSION),
        "featuresSupported": 18
    }
