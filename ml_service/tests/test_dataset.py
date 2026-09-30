import pytest
import pandas as pd
import numpy as np
from app.training.dataset_generator import generate_synthetic_clinical_cohort
from app.models.feature_vector import FEATURE_COLUMNS

def test_dataset_generation_shape():
    """Verify synthetic dataset produces the expected sample count and 18 features."""
    X, y = generate_synthetic_clinical_cohort(n_samples=500, random_seed=42)
    assert isinstance(X, pd.DataFrame)
    assert isinstance(y, pd.Series)
    assert len(X) == 500
    assert len(y) == 500
    assert X.shape[1] == 18

def test_dataset_schema_and_column_ordering():
    """Assert all 18 FEATURE_COLUMNS are present in the exact deterministic order."""
    X, y = generate_synthetic_clinical_cohort(n_samples=100, random_seed=42)
    assert list(X.columns) == FEATURE_COLUMNS

def test_dataset_reproducibility():
    """Verify identical random seeds produce identical cohorts."""
    X1, y1 = generate_synthetic_clinical_cohort(n_samples=200, random_seed=123)
    X2, y2 = generate_synthetic_clinical_cohort(n_samples=200, random_seed=123)
    pd.testing.assert_frame_equal(X1, X2)
    pd.testing.assert_series_equal(y1, y2)

def test_dataset_target_distribution():
    """Assert target values are binary (0 or 1) with realistic positive prevalence."""
    _, y = generate_synthetic_clinical_cohort(n_samples=1000, random_seed=42)
    unique_vals = set(y.unique())
    assert unique_vals.issubset({0, 1})
    pos_rate = y.mean()
    # Clinically realistic 3-year adverse cardiometabolic event prevalence between 15% and 35%
    assert 0.15 <= pos_rate <= 0.35

def test_dataset_feature_ranges():
    """Assert features fall within realistic physiological bounds."""
    X, _ = generate_synthetic_clinical_cohort(n_samples=500, random_seed=42)
    assert (X["age"] >= 18).all() and (X["age"] <= 100).all()
    assert (X["bmi"] >= 14.0).all() and (X["bmi"] <= 60.0).all()
    assert (X["systolic_bp_current"] >= 70.0).all() and (X["systolic_bp_current"] <= 250.0).all()
    assert (X["diastolic_bp_current"] >= 40.0).all() and (X["diastolic_bp_current"] <= 150.0).all()
