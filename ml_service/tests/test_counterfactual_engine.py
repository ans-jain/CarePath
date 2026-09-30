import pytest

from app.models.feature_vector import FeatureVector
from app.services.model_registry import model_registry
from app.services.inference_engine import inference_engine
from app.services.counterfactual_engine import counterfactual_engine

@pytest.fixture(scope="module", autouse=True)
def ensure_model_loaded():
    if not model_registry.is_loaded:
        from app.training.train import train_and_persist_model
        train_and_persist_model(n_samples=500, random_seed=42)
        model_registry.load()

@pytest.fixture
def high_risk_patient():
    return FeatureVector(
        age=56.0,
        biological_sex="MALE",
        bmi=32.5,
        smoking_status="CURRENT",
        systolic_bp_current=152.0,
        diastolic_bp_current=95.0,
        heart_rate_current=86.0,
        fasting_glucose_current=145.0,
        hba1c_current=7.2,
        cholesterol_total=240.0,
        cholesterol_hdl=36.0,
        cholesterol_ldl=160.0,
        triglycerides=230.0,
        sleep_hours_current=5.5,
        systolic_bp_ewma_delta=10.0,
        fasting_glucose_ewma_delta=15.0,
        systolic_bp_slope_14d=0.85,
        fasting_glucose_slope_14d=0.95
    )

@pytest.fixture
def optimal_low_risk_patient():
    return FeatureVector(
        age=28.0,
        biological_sex="FEMALE",
        bmi=21.0,
        smoking_status="NEVER",
        systolic_bp_current=112.0,
        diastolic_bp_current=72.0,
        heart_rate_current=62.0,
        fasting_glucose_current=88.0,
        hba1c_current=5.0,
        cholesterol_total=170.0,
        cholesterol_hdl=65.0,
        cholesterol_ldl=90.0,
        triglycerides=95.0,
        sleep_hours_current=7.5,
        systolic_bp_ewma_delta=0.0,
        fasting_glucose_ewma_delta=0.0,
        systolic_bp_slope_14d=0.0,
        fasting_glucose_slope_14d=0.0
    )

def test_counterfactual_generation_for_elevated_risk(high_risk_patient):
    """Verify that counterfactuals are produced and reduce risk."""
    pred = inference_engine.predict(high_risk_patient)
    assert pred.overall_risk_score > 0.40

    result = counterfactual_engine.generate_counterfactuals(
        features=high_risk_patient,
        current_prediction=pred
    )

    assert result.counterfactual_available is True
    assert result.status == "SUCCESS"
    assert len(result.plans) >= 1

    for plan in result.plans:
        assert plan.simulated_risk_score < pred.overall_risk_score
        assert plan.risk_reduction > 0
        assert len(plan.changed_features) in [1, 2]
        for cf in plan.changed_features:
            assert cf.modifiable is True
            assert cf.feature not in [
                "age", "biological_sex", "systolic_bp_ewma_delta",
                "fasting_glucose_ewma_delta", "systolic_bp_slope_14d", "fasting_glucose_slope_14d"
            ]
            assert cf.allowed_range[0] <= cf.target_value <= cf.allowed_range[1]

def test_counterfactual_immutable_features_are_never_altered(high_risk_patient):
    """Verify that immutable attributes (age, sex, history) are strictly protected."""
    pred = inference_engine.predict(high_risk_patient)
    result = counterfactual_engine.generate_counterfactuals(high_risk_patient, pred)

    immutable_keys = {
        "age", "biological_sex", "systolic_bp_ewma_delta",
        "fasting_glucose_ewma_delta", "systolic_bp_slope_14d", "fasting_glucose_slope_14d"
    }

    for plan in result.plans:
        changed_keys = {f.feature for f in plan.changed_features}
        overlap = changed_keys.intersection(immutable_keys)
        assert len(overlap) == 0, f"Immutable features were modified: {overlap}"

def test_counterfactual_low_risk_patient_produces_structured_empty_result(optimal_low_risk_patient):
    """Verify that a patient who is already at low risk returns NO_VALID_COUNTERFACTUAL_FOUND."""
    pred = inference_engine.predict(optimal_low_risk_patient)
    assert pred.overall_risk_score < 0.25

    result = counterfactual_engine.generate_counterfactuals(
        features=optimal_low_risk_patient,
        current_prediction=pred
    )

    assert result.counterfactual_available is False
    assert result.status == "NO_VALID_COUNTERFACTUAL_FOUND"
    assert len(result.plans) == 0
    assert "LOW risk category" in result.message or "No realistic" in result.message

def test_counterfactual_candidate_value_generation():
    """Verify candidate values obey physiological bounds and step sizes."""
    # Systolic BP: current 150 -> candidates should be <= 150 and >= 100
    candidates = counterfactual_engine._generate_candidate_values("systolic_bp_current", 150.0)
    assert len(candidates) > 0
    for val in candidates:
        assert val < 150.0
        assert val >= 100.0

    # Smoking status: CURRENT (2.0) -> ['FORMER', 'NEVER'] or [1.0, 0.0]
    smoke_cands = counterfactual_engine._generate_candidate_values("smoking_status", "CURRENT")
    assert "FORMER" in smoke_cands or 1.0 in smoke_cands
    assert "NEVER" in smoke_cands or 0.0 in smoke_cands
