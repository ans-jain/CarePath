from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import settings
from app.core.logging import get_logger
from app.services.model_registry import model_registry
from app.api.routes import health, predict, explain

logger = get_logger("carepath.ml_service")

@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Application lifecycle manager. Safely initializes model artifacts on startup.
    """
    logger.info("Initializing CarePath ML Service startup...")
    try:
        if settings.MODEL_PATH.exists() and settings.PREPROCESSOR_PATH.exists():
            model_registry.load()
            logger.info("Model artifacts loaded successfully on startup.")
        else:
            logger.warning(
                "Model artifacts not found at %s. Service running in degraded mode until trained.",
                settings.ARTIFACTS_DIR
            )
    except Exception as e:
        logger.error("Error loading model artifacts on startup: %s", str(e))

    yield

    logger.info("Shutting down CarePath ML Service...")
    model_registry.unload()

app = FastAPI(
    title="CarePath ML Risk Service",
    version=settings.SERVICE_VERSION,
    description=(
        "Internal machine learning microservice for CarePath. "
        "Computes continuous calibrated cardiometabolic risk signals and decision-support risk tiers. "
        "\n\n**CLINICAL GUARDRAIL**: Model outputs are strictly statistical risk signals, "
        "not clinical diagnoses or medical conclusions. Consult a physician."
    ),
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc"
)

# CORS Middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.middleware("http")
async def correlation_id_middleware(request, call_next):
    import uuid
    import time
    request_id = request.headers.get("X-Request-ID") or request.headers.get("X-Correlation-ID")
    if not request_id:
        request_id = str(uuid.uuid4())
    request.state.request_id = request_id

    start_time = time.perf_counter()
    response = await call_next(request)
    duration_ms = (time.perf_counter() - start_time) * 1000

    response.headers["X-Request-ID"] = request_id
    logger.info(
        "[%s] %s %s completed in %.2fms with status %d",
        request_id,
        request.method,
        request.url.path,
        duration_ms,
        response.status_code
    )
    return response

# Register Endpoints
app.include_router(health.router)
app.include_router(predict.router)
app.include_router(explain.router)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host=settings.HOST, port=settings.PORT, reload=False)
