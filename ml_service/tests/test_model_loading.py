import pytest
from pathlib import Path
from app.services.model_registry import ModelRegistry

def test_model_registry_missing_artifact(tmp_path):
    registry = ModelRegistry()
    missing_model = tmp_path / "non_existent_model.joblib"
    missing_prep = tmp_path / "non_existent_prep.joblib"

    with pytest.raises(FileNotFoundError):
        registry.load(model_path=missing_model, preprocessor_path=missing_prep)

    assert not registry.is_loaded

def test_unloaded_registry_access():
    registry = ModelRegistry()
    registry.unload()

    with pytest.raises(RuntimeError, match="Model is not loaded"):
        _ = registry.model

    with pytest.raises(RuntimeError, match="Preprocessor is not loaded"):
        _ = registry.preprocessor

def test_successful_loading(tmp_path):
    from app.training.train import train_and_persist_model
    artifacts_dir = tmp_path / "artifacts"
    train_and_persist_model(n_samples=200, random_seed=42, output_dir=artifacts_dir)

    registry = ModelRegistry()
    loaded = registry.load(
        model_path=artifacts_dir / "model.joblib",
        preprocessor_path=artifacts_dir / "preprocessor.joblib",
        metadata_path=artifacts_dir / "model_metadata.json"
    )
    assert loaded is True
    assert registry.is_loaded
    assert registry.model is not None
    assert registry.preprocessor is not None
    assert "model_version" in registry.metadata

    # Teardown
    registry.unload()
    assert not registry.is_loaded
