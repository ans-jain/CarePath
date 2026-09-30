import json
from datetime import datetime, timezone
from pathlib import Path
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.ensemble import HistGradientBoostingClassifier
from sklearn.calibration import CalibratedClassifierCV
from sklearn.metrics import (
    roc_auc_score,
    average_precision_score,
    brier_score_loss,
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    confusion_matrix
)
import joblib

from app.core.config import settings
from app.core.logging import get_logger
from app.models.feature_vector import FEATURE_COLUMNS
from app.services.preprocessing import CarePathPreprocessor
from app.training.dataset_generator import generate_synthetic_clinical_cohort

logger = get_logger("carepath.training")

def train_and_persist_model(
    n_samples: int = 5000,
    random_seed: int = 42,
    output_dir: Path = settings.ARTIFACTS_DIR
) -> dict:
    """
    Executes the deterministic end-to-end training and probability calibration pipeline.
    Persists model artifact, fitted preprocessor, and comprehensive model metadata.
    """
    logger.info("Initializing CarePath model training pipeline...")
    output_dir.mkdir(parents=True, exist_ok=True)

    # 1. Dataset Generation & Schema Validation
    logger.info("Generating synthetic clinical cohort (N=%d, seed=%d)...", n_samples, random_seed)
    X, y = generate_synthetic_clinical_cohort(n_samples=n_samples, random_seed=random_seed)

    # Assert schema integrity
    assert list(X.columns) == FEATURE_COLUMNS, "Generated dataset does not match FEATURE_COLUMNS"
    assert len(X) == n_samples, f"Expected {n_samples} samples, got {len(X)}"
    assert not y.isna().any(), "Target labels contain unexpected NaN values"

    pos_count = int(y.sum())
    neg_count = len(y) - pos_count
    pos_rate = pos_count / len(y)
    logger.info(
        "Cohort label distribution: %d negative (%.1f%%), %d positive (%.1f%%)",
        neg_count, (1 - pos_rate) * 100, pos_count, pos_rate * 100
    )

    # 2. Deterministic Train/Test Split (Stratified, 80/20)
    X_train, X_test, y_train, y_test = train_test_split(
        X, y,
        test_size=0.20,
        random_state=random_seed,
        stratify=y
    )
    logger.info("Stratified split: %d train samples, %d test samples", len(X_train), len(X_test))

    # 3. Fit Preprocessing Pipeline Strictly on Training Data
    logger.info("Fitting clinical preprocessor on training data...")
    preprocessor = CarePathPreprocessor()
    preprocessor.fit(X_train)

    X_train_proc = preprocessor.transform(X_train)
    X_test_proc = preprocessor.transform(X_test)

    # 4. Model Architecture & Training (HistGradientBoostingClassifier)
    logger.info("Training HistGradientBoostingClassifier base estimator...")
    base_model = HistGradientBoostingClassifier(
        max_iter=150,
        max_leaf_nodes=31,
        learning_rate=0.05,
        min_samples_leaf=20,
        class_weight="balanced",
        random_state=random_seed
    )

    # Probability Calibration via 5-Fold Sigmoid (Platt) Scaling
    logger.info("Applying CalibratedClassifierCV(method='sigmoid', cv=5)...")
    calibrated_model = CalibratedClassifierCV(
        estimator=base_model,
        method="sigmoid",
        cv=5
    )
    calibrated_model.fit(X_train_proc, y_train)

    # 5. Comprehensive Model Evaluation on Test Set
    logger.info("Evaluating calibrated model on held-out test data...")
    y_pred_proba = calibrated_model.predict_proba(X_test_proc)[:, 1]
    y_pred = (y_pred_proba >= 0.50).astype(int)

    roc_auc = float(roc_auc_score(y_test, y_pred_proba))
    pr_auc = float(average_precision_score(y_test, y_pred_proba))
    brier = float(brier_score_loss(y_test, y_pred_proba))
    accuracy = float(accuracy_score(y_test, y_pred))
    precision = float(precision_score(y_test, y_pred, zero_division=0))
    recall = float(recall_score(y_test, y_pred, zero_division=0))
    f1 = float(f1_score(y_test, y_pred, zero_division=0))
    cm = confusion_matrix(y_test, y_pred).tolist()

    logger.info("================ MODEL EVALUATION REPORT ================")
    logger.info("ROC-AUC Score:      %.4f (target >= 0.88)", roc_auc)
    logger.info("PR-AUC Score:       %.4f (target >= 0.75)", pr_auc)
    logger.info("Brier Score:        %.4f (target <= 0.12)", brier)
    logger.info("Test Accuracy:      %.4f", accuracy)
    logger.info("Precision:          %.4f", precision)
    logger.info("Recall:             %.4f", recall)
    logger.info("F1 Score:           %.4f", f1)
    logger.info("Confusion Matrix:   TN=%d, FP=%d, FN=%d, TP=%d", cm[0][0], cm[0][1], cm[1][0], cm[1][1])
    logger.info("=========================================================")

    # 6. Artifact Serialization
    model_path = output_dir / "model.joblib"
    preprocessor_path = output_dir / "preprocessor.joblib"
    metadata_path = output_dir / "model_metadata.json"

    logger.info("Saving calibrated model artifact to: %s", model_path)
    joblib.dump(calibrated_model, model_path)

    logger.info("Saving clinical preprocessor artifact to: %s", preprocessor_path)
    joblib.dump(preprocessor, preprocessor_path)

    # 7. Model Metadata Persistence
    metadata = {
        "model_name": "carepath-cardiometabolic-risk",
        "model_version": settings.MODEL_VERSION,
        "algorithm": "HistGradientBoostingClassifier + CalibratedClassifierCV(method=sigmoid, cv=5)",
        "training_timestamp": datetime.now(timezone.utc).isoformat(),
        "dataset_version": "synthetic-nhanes-framingham-v1",
        "dataset_samples": n_samples,
        "train_samples": len(X_train),
        "test_samples": len(X_test),
        "class_distribution": {
            "negative_samples": neg_count,
            "positive_samples": pos_count,
            "positive_rate": round(pos_rate, 4)
        },
        "feature_columns": FEATURE_COLUMNS,
        "features_count": len(FEATURE_COLUMNS),
        "calibrated": True,
        "evaluation_metrics": {
            "roc_auc": round(roc_auc, 4),
            "pr_auc": round(pr_auc, 4),
            "brier_score": round(brier, 4),
            "accuracy": round(accuracy, 4),
            "precision": round(precision, 4),
            "recall": round(recall, 4),
            "f1_score": round(f1, 4),
            "confusion_matrix": cm
        }
    }

    logger.info("Saving model metadata to: %s", metadata_path)
    with open(metadata_path, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)

    logger.info("Training pipeline completed successfully.")
    return metadata

if __name__ == "__main__":
    train_and_persist_model()
