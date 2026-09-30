import pytest
import json
from pathlib import Path
from app.training.train import train_and_persist_model
from app.models.feature_vector import FEATURE_COLUMNS

def test_training_pipeline_execution(tmp_path):
    """Assert training pipeline trains model, calibrates, evaluates, and persists artifacts."""
    output_dir = tmp_path / "artifacts"
    metadata = train_and_persist_model(n_samples=500, random_seed=42, output_dir=output_dir)

    assert (output_dir / "model.joblib").exists()
    assert (output_dir / "preprocessor.joblib").exists()
    assert (output_dir / "model_metadata.json").exists()

    # Verify metadata fields
    assert metadata["model_version"] == "carepath-gbm-v1.0.0"
    assert metadata["calibrated"] is True
    assert metadata["features_count"] == 18
    assert metadata["feature_columns"] == FEATURE_COLUMNS

    metrics = metadata["evaluation_metrics"]
    assert "roc_auc" in metrics
    assert "pr_auc" in metrics
    assert "brier_score" in metrics
    assert "accuracy" in metrics

    # Probabilistic Brier score must be <= 0.20
    assert metrics["brier_score"] <= 0.20
    assert 0.0 <= metrics["roc_auc"] <= 1.0

def test_metadata_file_content(tmp_path):
    """Verify saved metadata JSON matches in-memory dictionary."""
    output_dir = tmp_path / "artifacts"
    meta = train_and_persist_model(n_samples=300, random_seed=99, output_dir=output_dir)

    with open(output_dir / "model_metadata.json", "r", encoding="utf-8") as f:
        loaded_meta = json.load(f)

    assert loaded_meta["dataset_samples"] == 300
    assert loaded_meta["train_samples"] == 240
    assert loaded_meta["test_samples"] == 60
    assert loaded_meta["evaluation_metrics"]["brier_score"] == meta["evaluation_metrics"]["brier_score"]
