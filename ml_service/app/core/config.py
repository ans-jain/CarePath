from pathlib import Path
import os

BASE_DIR = Path(__file__).resolve().parent.parent
DEFAULT_ARTIFACTS_DIR = BASE_DIR / "artifacts"

class Settings:
    SERVICE_NAME: str = "CarePath ML Risk Service"
    SERVICE_VERSION: str = "1.0.0"
    MODEL_VERSION: str = "carepath-gbm-v1.0.0"
    HOST: str = os.getenv("ML_HOST", "0.0.0.0")
    PORT: int = int(os.getenv("ML_PORT", "8000"))
    
    ARTIFACTS_DIR: Path = Path(os.getenv("ARTIFACTS_DIR", str(DEFAULT_ARTIFACTS_DIR)))
    MODEL_PATH: Path = ARTIFACTS_DIR / "model.joblib"
    PREPROCESSOR_PATH: Path = ARTIFACTS_DIR / "preprocessor.joblib"
    METADATA_PATH: Path = ARTIFACTS_DIR / "model_metadata.json"
    
    REGULATORY_DISCLAIMER: str = (
        "CarePath provides decision-support risk signals, not clinical diagnoses. "
        "Consult a physician."
    )

settings = Settings()
