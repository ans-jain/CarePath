import pytest
from pydantic import ValidationError
from app.models.feature_vector import FeatureVector, PredictRiskRequest
from app.services.inference_engine import InferenceEngine
from app.services.model_registry import model_registry

@pytest.fixture(scope="module", autouse=True)
def ensure_model_loaded():
    if not model_registry.is_loaded:
        from app.training.train import train_and_persist_model
        train_and_persist_model(n_samples=500, random_seed=42)
        model_registry.load()

def test_inference_valid_longitudinal_input():
    req = PredictRiskRequest(
        patient_uuid="test-patient-123",
        evaluation_mode="LONGITUDINAL",
        features=FeatureVector(
            age=52.0,
            biological_sex="MALE",
            bmi=28.4,
            smoking_status="NEVER",
            systolic_bp_current=135.0,
            diastolic_bp_current=85.0,
            heart_rate_current=72.0,
            fasting_glucose_current=105.0,
            hba1c_current=5.9,
            cholesterol_total=210.0,
            cholesterol_hdl=45.0,
            cholesterol_ldl=130.0,
            triglycerides=175.0,
            sleep_hours_current=6.5,
            systolic_bp_ewma_delta=3.2,
            fasting_glucose_ewma_delta=4.8,
            systolic_bp_slope_14d=0.25,
            fasting_glucose_slope_14d=0.30
        )
    )
    engine = InferenceEngine()
    resp = engine.predict(req)

    assert 0.0 <= resp.overall_risk_score <= 1.0
    assert resp.risk_category in ["LOW", "MODERATE", "ELEVATED", "HIGH"]
    assert resp.prediction in [0, 1]
    assert resp.probability == resp.overall_risk_score
    assert resp.calibrated is True
    assert resp.confidence_level == "LONGITUDINAL_ROBUST"
    assert "CarePath provides decision-support risk signals" in resp.regulatory_disclaimer

def test_inference_cold_start_intake_mode():
    req = PredictRiskRequest(
        patient_uuid="intake-patient-456",
        evaluation_mode="PRELIMINARY_INTAKE",
        features=FeatureVector(
            age=35.0,
            biological_sex="FEMALE",
            bmi=22.0,
            smoking_status="NEVER",
            systolic_bp_current=115.0,
            diastolic_bp_current=75.0,
            heart_rate_current=65.0,
            fasting_glucose_current=88.0,
            hba1c_current=None,  # pending lab
            cholesterol_total=None,
            cholesterol_hdl=None,
            cholesterol_ldl=None,
            triglycerides=None
        )
    )
    engine = InferenceEngine()
    resp = engine.predict(req)

    assert 0.0 <= resp.overall_risk_score <= 1.0
    assert resp.confidence_level == "PRELIMINARY_INTAKE"
    # Young, healthy biomarkers should result in Low or Moderate risk
    assert resp.risk_category in ["LOW", "MODERATE"]

def test_risk_category_mapping():
    assert InferenceEngine.categorize_risk(0.10) == "LOW"
    assert InferenceEngine.categorize_risk(0.2499) == "LOW"
    assert InferenceEngine.categorize_risk(0.25) == "MODERATE"
    assert InferenceEngine.categorize_risk(0.4999) == "MODERATE"
    assert InferenceEngine.categorize_risk(0.50) == "ELEVATED"
    assert InferenceEngine.categorize_risk(0.7499) == "ELEVATED"
    assert InferenceEngine.categorize_risk(0.75) == "HIGH"
    assert InferenceEngine.categorize_risk(0.95) == "HIGH"

def test_missing_required_feature_raises_validation_error():
    with pytest.raises(ValidationError):
        FeatureVector(
            age=45.0,
            # biological_sex is missing!
            bmi=25.0,
            smoking_status="NEVER",
            systolic_bp_current=120.0,
            diastolic_bp_current=80.0,
            heart_rate_current=70.0,
            fasting_glucose_current=95.0
        )

def test_invalid_feature_type_raises_validation_error():
    with pytest.raises(ValidationError):
        FeatureVector(
            age="not_a_valid_age",
            biological_sex="FEMALE",
            bmi=25.0,
            smoking_status="NEVER",
            systolic_bp_current=120.0,
            diastolic_bp_current=80.0,
            heart_rate_current=70.0,
            fasting_glucose_current=95.0
        )

def test_physiological_bounds_validation():
    # Systolic BP > 300 should fail
    with pytest.raises(ValidationError):
        FeatureVector(
            age=45.0,
            biological_sex="FEMALE",
            bmi=25.0,
            smoking_status="NEVER",
            systolic_bp_current=350.0,  # Out of bounds (>300)
            diastolic_bp_current=80.0,
            heart_rate_current=70.0,
            fasting_glucose_current=95.0
        )

    # Age < 18 should fail
    with pytest.raises(ValidationError):
        FeatureVector(
            age=10.0,  # Below 18
            biological_sex="FEMALE",
            bmi=25.0,
            smoking_status="NEVER",
            systolic_bp_current=120.0,
            diastolic_bp_current=80.0,
            heart_rate_current=70.0,
            fasting_glucose_current=95.0
        )

def test_unexpected_fields_rejected():
    with pytest.raises(ValidationError):
        FeatureVector(
            age=45.0,
            biological_sex="FEMALE",
            bmi=25.0,
            smoking_status="NEVER",
            systolic_bp_current=120.0,
            diastolic_bp_current=80.0,
            heart_rate_current=70.0,
            fasting_glucose_current=95.0,
            arbitrary_unauthorized_field=123.45  # Must be rejected by extra="forbid"
        )
