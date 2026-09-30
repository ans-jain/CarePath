import pytest
from fastapi.testclient import TestClient
from unittest.mock import patch, PropertyMock

from app.main import app
from app.services.model_registry import model_registry, ModelRegistry
from app.models.feature_vector import FeatureVector

client = TestClient(app)

@pytest.fixture(scope="module", autouse=True)
def ensure_model_loaded():
    if not model_registry.is_loaded:
        model_registry.load()

def test_request_id_middleware_propagation():
    """Verify X-Request-ID header is propagated or created by correlation middleware."""
    custom_req_id = "test-req-correlation-12345"
    response = client.get("/health", headers={"X-Request-ID": custom_req_id})
    assert response.status_code == 200
    assert response.headers.get("X-Request-ID") == custom_req_id

    # Test auto-generation when omitted
    resp_no_header = client.get("/health")
    assert resp_no_header.status_code == 200
    assert "X-Request-ID" in resp_no_header.headers
    assert len(resp_no_header.headers["X-Request-ID"]) > 10

def test_readiness_probe_success():
    """Verify /health/ready returns 200 OK when model is loaded."""
    response = client.get("/health/ready")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "UP"
    assert data["ready"] is True
    assert data["featuresSupported"] == 18

def test_readiness_probe_unloaded_model_failure():
    """Verify /health/ready returns 503 Service Unavailable when model is not loaded."""
    with patch.object(ModelRegistry, "is_loaded", new_callable=PropertyMock) as mock_loaded:
        mock_loaded.return_value = False
        response = client.get("/health/ready")
        assert response.status_code == 503
        data = response.json()
        assert data["detail"]["status"] == "DOWN"
        assert data["detail"]["ready"] is False

def test_input_validation_boundary_extremes():
    """Verify model can process valid physiological boundary extreme values."""
    min_extreme_features = {
        "age": 18.0,
        "biological_sex": "FEMALE",
        "bmi": 15.0,
        "smoking_status": "NEVER",
        "systolic_bp_current": 80.0,
        "diastolic_bp_current": 50.0,
        "heart_rate_current": 45.0,
        "fasting_glucose_current": 60.0,
        "hba1c_current": 4.2,
        "cholesterol_total": 120.0,
        "cholesterol_hdl": 30.0,
        "cholesterol_ldl": 50.0,
        "triglycerides": 50.0,
        "sleep_hours_current": 4.0,
        "systolic_bp_ewma_delta": 0.0,
        "fasting_glucose_ewma_delta": 0.0,
        "systolic_bp_slope_14d": 0.0,
        "fasting_glucose_slope_14d": 0.0
    }

    response = client.post("/ml/v1/risk/predict", json={"features": min_extreme_features})
    assert response.status_code == 200
    pred = response.json()
    assert 0.0 <= pred["overall_risk_score"] <= 1.0
    assert pred["risk_category"] in ["LOW", "MODERATE", "ELEVATED", "HIGH"]

    max_extreme_features = min_extreme_features.copy()
    max_extreme_features.update({
        "age": 95.0,
        "bmi": 55.0,
        "smoking_status": "CURRENT",
        "systolic_bp_current": 210.0,
        "diastolic_bp_current": 125.0,
        "heart_rate_current": 130.0,
        "fasting_glucose_current": 320.0,
        "hba1c_current": 11.5,
        "cholesterol_total": 380.0,
        "cholesterol_ldl": 220.0,
        "triglycerides": 450.0,
    })

    resp_max = client.post("/ml/v1/risk/predict", json={"features": max_extreme_features})
    assert resp_max.status_code == 200
    pred_max = resp_max.json()
    assert 0.0 <= pred_max["overall_risk_score"] <= 1.0
    assert pred_max["overall_risk_score"] > pred["overall_risk_score"]

def test_schema_rejection_on_invalid_types_and_ranges():
    """Verify HTTP 422 Unprocessable Entity on schema violations and negative values."""
    # Negative age
    response = client.post("/ml/v1/risk/predict", json={
        "features": {"age": -5.0, "biological_sex": "MALE"}
    })
    assert response.status_code == 422

    # Invalid biological sex
    response = client.post("/ml/v1/risk/predict", json={
        "features": {"age": 45.0, "biological_sex": "UNKNOWN"}
    })
    assert response.status_code == 422

    # Systolic BP out of schema bounds (< 40 or > 300)
    response = client.post("/ml/v1/risk/predict", json={
        "features": {"age": 45.0, "biological_sex": "MALE", "systolic_bp_current": 450.0}
    })
    assert response.status_code == 422

def test_top_k_bounds_safeguard():
    """Verify top_k parameter bounds (1 <= top_k <= 18)."""
    base_features = {
        "age": 55.0,
        "biological_sex": "MALE",
        "bmi": 31.0,
        "smoking_status": "FORMER",
        "systolic_bp_current": 145.0,
        "diastolic_bp_current": 92.0,
        "heart_rate_current": 78.0,
        "fasting_glucose_current": 115.0,
        "hba1c_current": 6.8,
        "cholesterol_total": 225.0,
        "cholesterol_hdl": 42.0,
        "cholesterol_ldl": 145.0,
        "triglycerides": 185.0,
        "sleep_hours_current": 6.0,
        "systolic_bp_ewma_delta": 2.5,
        "fasting_glucose_ewma_delta": 3.0,
        "systolic_bp_slope_14d": 0.2,
        "fasting_glucose_slope_14d": 0.3
    }

    # top_k = 0 rejected (ge=1)
    resp_zero = client.post("/ml/v1/risk/explain", json={
        "features": base_features,
        "top_k": 0
    })
    assert resp_zero.status_code == 422

    # top_k = 25 rejected (le=18)
    resp_large = client.post("/ml/v1/risk/explain", json={
        "features": base_features,
        "top_k": 25
    })
    assert resp_large.status_code == 422

    # Valid top_k = 3 returns exactly at most 3 top drivers
    resp_valid = client.post("/ml/v1/risk/explain", json={
        "features": base_features,
        "top_k": 3
    })
    assert resp_valid.status_code == 200
    data = resp_valid.json()
    assert len(data["explanation"]["top_risk_drivers"]) <= 3

def test_counterfactual_immutable_feature_protection():
    """Verify counterfactual engine NEVER attempts to modify age or biological sex."""
    elevated_features = {
        "age": 62.0,
        "biological_sex": "MALE",
        "bmi": 32.5,
        "smoking_status": "CURRENT",
        "systolic_bp_current": 150.0,
        "diastolic_bp_current": 95.0,
        "heart_rate_current": 82.0,
        "fasting_glucose_current": 125.0,
        "hba1c_current": 7.2,
        "cholesterol_total": 235.0,
        "cholesterol_hdl": 38.0,
        "cholesterol_ldl": 150.0,
        "triglycerides": 210.0,
        "sleep_hours_current": 5.5,
        "systolic_bp_ewma_delta": 4.0,
        "fasting_glucose_ewma_delta": 5.0,
        "systolic_bp_slope_14d": 0.4,
        "fasting_glucose_slope_14d": 0.5
    }

    response = client.post("/ml/v1/risk/explain", json={
        "features": elevated_features,
        "generate_counterfactuals": True
    })
    assert response.status_code == 200
    data = response.json()
    cf_res = data["counterfactuals"]
    assert cf_res["counterfactual_available"] is True

    forbidden_to_modify = {"age", "biological_sex"}
    for plan in cf_res["plans"]:
        for target in plan["changed_features"]:
            assert target["feature"] not in forbidden_to_modify
            assert target["modifiable"] is True
            # Assert target value is within allowed range
            min_val, max_val = target["allowed_range"]
            assert float(target["target_value"]) >= min_val
            assert float(target["target_value"]) <= max_val
