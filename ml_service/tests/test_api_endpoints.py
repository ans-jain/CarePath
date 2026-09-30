import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.services.model_registry import model_registry

@pytest.fixture(scope="module")
def client():
    # Ensure model is trained and loaded
    if not model_registry.is_loaded:
        from app.training.train import train_and_persist_model
        train_and_persist_model(n_samples=500, random_seed=42)
        model_registry.load()
    with TestClient(app) as c:
        yield c

def test_health_endpoint_healthy(client):
    response = client.get("/ml/v1/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "HEALTHY"
    assert data["model_loaded"] is True
    assert data["featuresSupported"] == 18
    assert "modelVersion" in data
    assert data["calibrated"] is True

def test_health_endpoint_alias(client):
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "HEALTHY"

def test_predict_endpoint_valid_request(client):
    payload = {
        "patient_uuid": "00000000-0000-0000-0000-000000000001",
        "evaluation_mode": "LONGITUDINAL",
        "features": {
            "age": 48.0,
            "biological_sex": "FEMALE",
            "bmi": 26.63,
            "smoking_status": "NEVER",
            "systolic_bp_current": 128.0,
            "diastolic_bp_current": 82.0,
            "heart_rate_current": 68.0,
            "fasting_glucose_current": 104.0,
            "hba1c_current": 5.8,
            "cholesterol_total": 195.0,
            "cholesterol_hdl": 58.0,
            "cholesterol_ldl": 115.0,
            "triglycerides": 110.0,
            "sleep_hours_current": 6.5,
            "systolic_bp_ewma_delta": 3.9,
            "fasting_glucose_ewma_delta": 5.8,
            "systolic_bp_slope_14d": 0.42,
            "fasting_glucose_slope_14d": 0.55
        }
    }
    response = client.post("/ml/v1/risk/predict", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert "overall_risk_score" in data
    assert 0.0 <= data["overall_risk_score"] <= 1.0
    assert data["risk_category"] in ["LOW", "MODERATE", "ELEVATED", "HIGH"]
    assert data["prediction"] in [0, 1]
    assert data["calibrated"] is True
    assert data["confidence_level"] == "LONGITUDINAL_ROBUST"
    assert "CarePath provides decision-support risk signals" in data["regulatory_disclaimer"]

def test_predict_endpoint_alias(client):
    payload = {
        "features": {
            "age": 55.0,
            "biological_sex": "MALE",
            "bmi": 30.0,
            "smoking_status": "CURRENT",
            "systolic_bp_current": 140.0,
            "diastolic_bp_current": 90.0,
            "heart_rate_current": 80.0,
            "fasting_glucose_current": 120.0
        }
    }
    response = client.post("/predict", json=payload)
    assert response.status_code == 200
    assert "overall_risk_score" in response.json()

def test_predict_endpoint_missing_required_field(client):
    # Missing systolic_bp_current
    payload = {
        "features": {
            "age": 48.0,
            "biological_sex": "FEMALE",
            "bmi": 26.63,
            "smoking_status": "NEVER",
            "diastolic_bp_current": 82.0,
            "heart_rate_current": 68.0,
            "fasting_glucose_current": 104.0
        }
    }
    response = client.post("/ml/v1/risk/predict", json=payload)
    assert response.status_code == 422
    errors = response.json().get("detail", [])
    assert any("systolic_bp_current" in str(e) for e in errors)

def test_predict_endpoint_invalid_data_type(client):
    payload = {
        "features": {
            "age": "invalid_string_age",
            "biological_sex": "FEMALE",
            "bmi": 26.63,
            "smoking_status": "NEVER",
            "systolic_bp_current": 128.0,
            "diastolic_bp_current": 82.0,
            "heart_rate_current": 68.0,
            "fasting_glucose_current": 104.0
        }
    }
    response = client.post("/ml/v1/risk/predict", json=payload)
    assert response.status_code == 422

def test_predict_endpoint_out_of_bounds_value(client):
    payload = {
        "features": {
            "age": 48.0,
            "biological_sex": "FEMALE",
            "bmi": 26.63,
            "smoking_status": "NEVER",
            "systolic_bp_current": 350.0,  # Out of bounds
            "diastolic_bp_current": 82.0,
            "heart_rate_current": 68.0,
            "fasting_glucose_current": 104.0
        }
    }
    response = client.post("/ml/v1/risk/predict", json=payload)
    assert response.status_code == 422

def test_predict_endpoint_unauthorized_extra_field(client):
    payload = {
        "features": {
            "age": 48.0,
            "biological_sex": "FEMALE",
            "bmi": 26.63,
            "smoking_status": "NEVER",
            "systolic_bp_current": 128.0,
            "diastolic_bp_current": 82.0,
            "heart_rate_current": 68.0,
            "fasting_glucose_current": 104.0,
            "unknown_injected_column": 999.9  # Should trigger extra="forbid"
        }
    }
    response = client.post("/ml/v1/risk/predict", json=payload)
    assert response.status_code == 422

def test_openapi_docs_endpoint(client):
    response = client.get("/docs")
    assert response.status_code == 200

def test_unloaded_model_behavior(monkeypatch, tmp_path):
    """Verify endpoint behavior when model is not loaded."""
    from app.core.config import settings
    monkeypatch.setattr(settings, "MODEL_PATH", tmp_path / "non_existent.joblib")
    model_registry.unload()
    with TestClient(app) as test_c:
        # /health reports DEGRADED
        h_resp = test_c.get("/ml/v1/health")
        assert h_resp.status_code == 200
        assert h_resp.json()["status"] == "DEGRADED"
        assert h_resp.json()["model_loaded"] is False

        # /predict returns 503 Service Unavailable
        p_resp = test_c.post("/ml/v1/risk/predict", json={
            "features": {
                "age": 48.0,
                "biological_sex": "FEMALE",
                "bmi": 26.0,
                "smoking_status": "NEVER",
                "systolic_bp_current": 120.0,
                "diastolic_bp_current": 80.0,
                "heart_rate_current": 70.0,
                "fasting_glucose_current": 95.0
            }
        })
        assert p_resp.status_code == 503
        assert "not loaded" in p_resp.json()["detail"]

    # Re-load model after test
    monkeypatch.undo()
    model_registry.load()
