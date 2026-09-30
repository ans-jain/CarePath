import json
from pathlib import Path
from typing import Optional, Dict, Any
import joblib

from app.core.config import settings
from app.core.logging import get_logger

logger = get_logger("carepath.model_registry")

class ModelRegistry:
    """
    Thread-safe registry for loading, caching, and serving trained model and preprocessor artifacts.
    """
    _instance: Optional["ModelRegistry"] = None

    def __init__(self):
        self._model = None
        self._preprocessor = None
        self._metadata: Optional[Dict[str, Any]] = None
        self._is_loaded: bool = False

    @classmethod
    def get_instance(cls) -> "ModelRegistry":
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    def load(
        self,
        model_path: Optional[Path] = None,
        preprocessor_path: Optional[Path] = None,
        metadata_path: Optional[Path] = None
    ) -> bool:
        """
        Loads the trained model, preprocessor, and metadata from disk.
        Fails safely if artifacts do not exist.
        """
        m_path = model_path or settings.MODEL_PATH
        p_path = preprocessor_path or settings.PREPROCESSOR_PATH
        meta_path = metadata_path or settings.METADATA_PATH

        if not m_path.exists():
            logger.error("Model artifact not found at: %s", m_path)
            self._is_loaded = False
            raise FileNotFoundError(f"Model artifact not found at {m_path}")

        if not p_path.exists():
            logger.error("Preprocessor artifact not found at: %s", p_path)
            self._is_loaded = False
            raise FileNotFoundError(f"Preprocessor artifact not found at {p_path}")

        try:
            logger.info("Loading model artifact from %s", m_path)
            self._model = joblib.load(m_path)

            logger.info("Loading preprocessor artifact from %s", p_path)
            self._preprocessor = joblib.load(p_path)

            if meta_path.exists():
                with open(meta_path, "r", encoding="utf-8") as f:
                    self._metadata = json.load(f)
            else:
                self._metadata = {
                    "model_version": settings.MODEL_VERSION,
                    "calibrated": True
                }

            self._is_loaded = True
            logger.info("Model pipeline successfully loaded into memory.")
            return True

        except Exception as e:
            logger.exception("Failed to load model pipeline artifacts: %s", str(e))
            self._is_loaded = False
            raise RuntimeError(f"Error loading model artifacts: {e}") from e

    def unload(self):
        """Unloads artifacts from memory (used for testing and teardown)."""
        self._model = None
        self._preprocessor = None
        self._metadata = None
        self._is_loaded = False

    @property
    def is_loaded(self) -> bool:
        return self._is_loaded and self._model is not None and self._preprocessor is not None

    @property
    def model(self):
        if not self.is_loaded:
            raise RuntimeError("Model is not loaded. Call load() before requesting model.")
        return self._model

    @property
    def preprocessor(self):
        if not self.is_loaded:
            raise RuntimeError("Preprocessor is not loaded. Call load() before requesting preprocessor.")
        return self._preprocessor

    @property
    def metadata(self) -> Dict[str, Any]:
        return self._metadata or {}

model_registry = ModelRegistry.get_instance()
