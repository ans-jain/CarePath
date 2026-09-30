import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.services.model_registry import model_registry

@pytest.fixture(scope="module")
def client():
    if not model_registry.is_loaded:
        from app.training.train import train_and_persist_model
        train_and_persist_model(n_samples=500, random_seed=42)
        model_registry.load()
    with TestClient(app) as c:
        yield c

@pytest.fixture
def valid_explain_payload():
    return {
        "patient_uuid": "00000000-0000-0000-0000-000000000001",
        "evaluation_mode": "LONGITUDINAL",
        "features": {
            "age": 55.0,
            "biological_sex": "MALE",
            "bmi": 29.5,
            "smoking_status": "CURRENT",
            "systolic_bp_current": 142.0,
            "diastolic_bp_current": 90.0,
            "heart_rate_current": 78.0,
            "fasting_glucose_current": 125.0,
            "hba1c_current": 6.4,
            "cholesterol_total": 220.0,
            "cholesterol_hdl": 42.0,
            "cholesterol_ldl": 140.0,
            "triglycerides": 180.0,
            "sleep_hours_current": 6.0,
            "systolic_bp_ewma_delta": 6.5,
            "fasting_glucose_ewma_delta": 9.2,
            "systolic_bp_slope_14d": 0.55,
            "fasting_glucose_slope_14d": 0.70
        },
        "top_k": 4,
        "generate_counterfactuals": True
    }

def test_explain_endpoint_success(client, valid_explain_payload):
    """Verify /ml/v1/risk/explain returns unified prediction, SHAP, and counterfactuals."""
    response = client.post("/ml/v1/risk/explain", json=valid_explain_payload)
    assert response.status_code == 200
    data = response.json()

    # 1. Prediction component
    assert "prediction" in data
    pred = data["prediction"]
    assert "overall_risk_score" in pred
    assert "risk_category" in pred
    assert pred["calibrated"] is True

    # 2. Explanation component
    assert "explanation" in data
    exp = data["explanation"]
    assert "base_value" in exp
    assert "shap_values" in exp
    assert len(exp["shap_values"]) == 18
    assert "top_risk_drivers" in exp
    assert "protective_factors" in exp
    assert "summary_narrative" in exp

    # Check top_k limit respected
    assert len(exp["top_risk_drivers"]) <= 4
    assert len(exp["protective_factors"]) <= 4

    # 3. Counterfactuals component
    assert "counterfactuals" in data
    cf = data["counterfactuals"]
    assert "counterfactual_available" in cf
    assert "status" in cf
    assert "plans" in cf

    # 4. Clinical disclaimer
    assert "regulatory_disclaimer" in data
    assert "CarePath provides decision-support risk signals" in data["regulatory_disclaimer"]

def test_explain_endpoint_alias(client, valid_explain_payload):
    """Verify convenience alias /explain works identically."""
    response = client.post("/explain", json=valid_explain_payload)
    assert response.status_code == 200
    assert "explanation" in response.json()

def test_explain_endpoint_disable_counterfactuals(client, valid_explain_payload):
    """Verify generate_counterfactuals=False skips counterfactual generation."""
    payload = dict(valid_explain_payload)
    payload["generate_counterfactuals"] = False

    response = client.post("/ml/v1/risk/explain", json=payload)
    assert response.status_code == 200
    cf = response.json()["counterfactuals"]
    assert cf["counterfactual_available"] is False
    assert len(cf["plans"]) == 0

def test_explain_endpoint_missing_feature(client):
    """Verify 422 Unprocessable Entity when required feature is missing."""
    payload = {
        "features": {
            "age": 50.0,
            "biological_sex": "FEMALE",
            # systolic_bp_current is missing
            "diastolic_bp_current": 80.0
        }
    }
    response = client.post("/ml/v1/risk/explain", json=payload)
    assert response.status_code == 422

def test_explain_endpoint_forbidden_extra_feature(client, valid_explain_payload):
    """Verify 422 Unprocessable Entity when extra unexpected key is injected."""
    payload = dict(valid_explain_payload)
    payload["features"] = dict(payload["features"])
    payload["features"]["injected_unauthorized_key"] = 999.0

    response = client.post("/ml/v1/risk/explain", json=payload)
    assert response.status_code == 422

def test_explain_endpoint_unloaded_model_returns_503(monkeypatch, tmp_path):
    """Verify 503 Service Unavailable when model is not loaded."""
    from app.core.config import settings
    monkeypatch.setattr(settings, "MODEL_PATH", tmp_path / "non_existent.joblib")
    model_registry.unload()

    with TestClient(app) as test_c:
        resp = test_c.post("/ml/v1/risk/explain", json={
            "features": {
                "age": 45.0,
                "biological_sex": "FEMALE",
                "bmi": 24.0,
                "smoking_status": "NEVER",
                "systolic_bp_current": 120.0,
                "diastolic_bp_current": 80.0,
                "heart_rate_current": 70.0,
                "fasting_glucose_current": 95.0
            }
        })
        assert resp.status_code == 503
        assert "not loaded" in resp.json()["detail"]

    # Re-load
    monkeypatch.undo()
    model_registry.load()
