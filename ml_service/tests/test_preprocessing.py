import pytest
import numpy as np
import pandas as pd
from app.services.preprocessing import CarePathPreprocessor
from app.models.feature_vector import FeatureVector, FEATURE_COLUMNS

@pytest.fixture
def sample_raw_data():
    return pd.DataFrame({
        "age": [45.0, 60.0, 30.0],
        "biological_sex": ["FEMALE", "MALE", "FEMALE"],
        "bmi": [24.5, 31.0, 22.0],
        "smoking_status": ["NEVER", "FORMER", "CURRENT"],
        "systolic_bp_current": [118.0, 142.0, 110.0],
        "diastolic_bp_current": [78.0, 90.0, 72.0],
        "heart_rate_current": [68.0, 82.0, 64.0],
        "fasting_glucose_current": [92.0, 115.0, 88.0],
        "hba1c_current": [5.2, np.nan, 5.0],  # test missing value
        "cholesterol_total": [185.0, 220.0, np.nan],
        "cholesterol_hdl": [55.0, 42.0, 60.0],
        "cholesterol_ldl": [105.0, 145.0, 95.0],
        "triglycerides": [125.0, 165.0, 90.0],
        "sleep_hours_current": [7.5, 6.0, 8.0],
        "systolic_bp_ewma_delta": [0.0, 4.5, -2.0],
        "fasting_glucose_ewma_delta": [0.0, 6.0, -1.5],
        "systolic_bp_slope_14d": [0.0, 0.35, -0.1],
        "fasting_glucose_slope_14d": [0.0, 0.40, -0.05]
    })[FEATURE_COLUMNS]

def test_preprocessing_fit_transform(sample_raw_data):
    preprocessor = CarePathPreprocessor()
    preprocessor.fit(sample_raw_data)
    transformed = preprocessor.transform(sample_raw_data)

    assert isinstance(transformed, np.ndarray)
    assert transformed.shape == (3, 18)
    assert transformed.dtype == np.float64
    # Assert no NaNs remain after transformation
    assert not np.isnan(transformed).any()

def test_preprocessing_categorical_encoding():
    df = pd.DataFrame([{
        "age": 50.0,
        "biological_sex": "MALE",
        "bmi": 28.0,
        "smoking_status": "CURRENT",
        "systolic_bp_current": 130.0,
        "diastolic_bp_current": 85.0,
        "heart_rate_current": 75.0,
        "fasting_glucose_current": 100.0,
        "hba1c_current": 5.6,
        "cholesterol_total": 200.0,
        "cholesterol_hdl": 45.0,
        "cholesterol_ldl": 125.0,
        "triglycerides": 150.0,
        "sleep_hours_current": 7.0,
        "systolic_bp_ewma_delta": 0.0,
        "fasting_glucose_ewma_delta": 0.0,
        "systolic_bp_slope_14d": 0.0,
        "fasting_glucose_slope_14d": 0.0
    }])
    preprocessor = CarePathPreprocessor()
    preprocessor.fit(df)
    transformed = preprocessor.transform(df)

    # biological_sex is index 1 -> MALE should be 1.0
    assert transformed[0, 1] == 1.0
    # smoking_status is index 3 -> CURRENT should be 2.0
    assert transformed[0, 3] == 2.0

def test_preprocessing_pydantic_feature_vector():
    vec = FeatureVector(
        age=48.0,
        biological_sex="FEMALE",
        bmi=26.5,
        smoking_status="NEVER",
        systolic_bp_current=122.0,
        diastolic_bp_current=80.0,
        heart_rate_current=70.0,
        fasting_glucose_current=96.0,
        hba1c_current=None,  # Missing lab
        cholesterol_total=None,
        cholesterol_hdl=None,
        cholesterol_ldl=None,
        triglycerides=None
    )
    preprocessor = CarePathPreprocessor()
    # Dummy fit
    preprocessor.fit([vec.model_dump()])
    out = preprocessor.transform(vec)
    assert out.shape == (1, 18)
    assert not np.isnan(out).any()
    # biological_sex FEMALE -> 0.0
    assert out[0, 1] == 0.0
    # smoking_status NEVER -> 0.0
    assert out[0, 3] == 0.0

def test_feature_ordering_integrity():
    preprocessor = CarePathPreprocessor()
    assert preprocessor.feature_columns == FEATURE_COLUMNS
