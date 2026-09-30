#!/usr/bin/env python3
"""
CarePath Production Smoke Test Suite (scripts/smoke_test.py)
Validates full system availability, health probes, authentication,
predictive inference, TreeSHAP explainability, and access controls.

Usage:
    python scripts/smoke_test.py [--base-url http://localhost:80] [--backend-url http://localhost:8080] [--ml-url http://localhost:8000]
"""

import sys
import json
import argparse
import urllib.request
import urllib.error

GREEN = "\033[92m"
RED = "\033[91m"
YELLOW = "\033[93m"
CYAN = "\033[96m"
RESET = "\033[0m"

def log_pass(name: str, detail: str = ""):
    print(f"  {GREEN}[PASS]{RESET} {name} {CYAN}{detail}{RESET}")

def log_fail(name: str, detail: str = ""):
    print(f"  {RED}[FAIL]{RESET} {name} {RED}{detail}{RESET}")

def request(url: str, method: str = "GET", headers: dict = None, body: dict = None, expected_status: int = 200):
    req_headers = headers or {}
    data = None
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        req_headers["Content-Type"] = "application/json"
    
    req = urllib.request.Request(url, data=data, headers=req_headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            status = resp.status
            content_type = resp.headers.get("Content-Type", "")
            raw = resp.read().decode("utf-8")
            parsed = json.loads(raw) if "application/json" in content_type else raw
            return status, parsed
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8")
        try:
            parsed = json.loads(raw)
        except Exception:
            parsed = raw
        return e.code, parsed
    except Exception as e:
        return 0, str(e)

def run_smoke_tests(base_url: str, backend_url: str, ml_url: str) -> bool:
    print(f"\n{CYAN}================================================================={RESET}")
    print(f"{CYAN}       CarePath Production Deployment Smoke Test Suite           {RESET}")
    print(f"{CYAN}================================================================={RESET}\n")

    all_passed = True

    # -------------------------------------------------------------------------
    # 1. Health & Readiness Probes
    # -------------------------------------------------------------------------
    print(f"{YELLOW}--- 1. Health & Liveness Probes ---{RESET}")
    
    # Backend Health
    b_status, b_resp = request(f"{backend_url}/health")
    if b_status == 200 and isinstance(b_resp, dict) and b_resp.get("status") == "UP":
        log_pass("Backend /health", f"Status: {b_resp.get('status')}")
    else:
        log_fail("Backend /health", f"HTTP {b_status}: {b_resp}")
        all_passed = False

    # Backend Readiness
    br_status, br_resp = request(f"{backend_url}/health/ready")
    if br_status == 200 and isinstance(br_resp, dict) and br_resp.get("status") == "UP":
        log_pass("Backend /health/ready (Database Connected)", f"Database: {br_resp.get('database')}")
    else:
        log_fail("Backend /health/ready", f"HTTP {br_status}: {br_resp}")
        all_passed = False

    # ML Microservice Health
    ml_status, ml_resp = request(f"{ml_url}/ml/v1/health")
    if ml_status == 200 and isinstance(ml_resp, dict) and ml_resp.get("status") == "HEALTHY":
        log_pass("ML Service /ml/v1/health", f"Model: {ml_resp.get('modelVersion')}")
    else:
        log_fail("ML Service /ml/v1/health", f"HTTP {ml_status}: {ml_resp}")
        all_passed = False

    # ML Microservice Readiness
    mlr_status, mlr_resp = request(f"{ml_url}/ml/v1/health/ready")
    if mlr_status == 200 and isinstance(mlr_resp, dict) and mlr_resp.get("model_loaded") is True:
        log_pass("ML Service /ml/v1/health/ready", f"Explainer: {mlr_resp.get('shapExplainerType')}")
    else:
        log_fail("ML Service /ml/v1/health/ready", f"HTTP {mlr_status}: {mlr_resp}")
        all_passed = False

    # -------------------------------------------------------------------------
    # 2. Security Guardrails & Authentication
    # -------------------------------------------------------------------------
    print(f"\n{YELLOW}--- 2. Security & Authentication Guardrails ---{RESET}")

    # Test Unauthorized Rejection
    unauth_status, _ = request(f"{backend_url}/api/v1/patients/me")
    if unauth_status in (401, 403):
        log_pass("Unauthenticated Access Guard", f"Correctly rejected with HTTP {unauth_status}")
    else:
        log_fail("Unauthenticated Access Guard", f"Expected HTTP 401/403, received HTTP {unauth_status}")
        all_passed = False

    # Test Login (Default seeded persona or registered user)
    login_payload = {
        "email": "sarah.jenkins@carepath.io",
        "password": "Password123!"
    }
    # Attempt register if not existing, then login
    reg_payload = {
        "email": "smoke.test@carepath.io",
        "password": "SmokeTestPassword123!",
        "firstName": "Smoke",
        "lastName": "Tester"
    }
    reg_status, reg_resp = request(f"{backend_url}/api/v1/auth/register", method="POST", body=reg_payload)
    
    auth_payload = {
        "email": "smoke.test@carepath.io",
        "password": "SmokeTestPassword123!"
    }
    login_status, login_resp = request(f"{backend_url}/api/v1/auth/login", method="POST", body=auth_payload)
    token = None
    if login_status == 200 and isinstance(login_resp, dict) and "accessToken" in login_resp:
        token = login_resp["accessToken"]
        log_pass("Authentication (POST /api/v1/auth/login)", f"Token received for {auth_payload['email']}")
    else:
        log_fail("Authentication", f"HTTP {login_status}: {login_resp}")
        all_passed = False

    # -------------------------------------------------------------------------
    # 3. Machine Learning Inference & TreeSHAP Explainability
    # -------------------------------------------------------------------------
    print(f"\n{YELLOW}--- 3. Machine Learning Inference & TreeSHAP Explainability ---{RESET}")

    sample_patient = {
        "patient_uuid": "00000000-0000-0000-0000-000000000001",
        "evaluation_mode": "LONGITUDINAL",
        "features": {
            "age": 56.0,
            "biological_sex": "MALE",
            "bmi": 31.5,
            "smoking_status": "CURRENT",
            "systolic_bp_current": 148.0,
            "diastolic_bp_current": 92.0,
            "heart_rate_current": 82.0,
            "fasting_glucose_current": 138.0,
            "hba1c_current": 6.8,
            "cholesterol_total": 230.0,
            "cholesterol_hdl": 40.0,
            "cholesterol_ldl": 150.0,
            "triglycerides": 210.0,
            "sleep_hours_current": 5.5,
            "systolic_bp_ewma_delta": 7.5,
            "fasting_glucose_ewma_delta": 11.2,
            "systolic_bp_slope_14d": 0.65,
            "fasting_glucose_slope_14d": 0.80
        },
        "top_k": 4,
        "generate_counterfactuals": True
    }

    # Prediction API
    pred_status, pred_resp = request(f"{ml_url}/ml/v1/risk/predict", method="POST", body=sample_patient)
    if pred_status == 200 and isinstance(pred_resp, dict) and "overall_risk_score" in pred_resp:
        score = pred_resp["overall_risk_score"]
        tier = pred_resp.get("risk_category")
        log_pass("Risk Prediction (POST /ml/v1/risk/predict)", f"Score: {score:.3f} | Tier: {tier}")
    else:
        log_fail("Risk Prediction", f"HTTP {pred_status}: {pred_resp}")
        all_passed = False

    # Explainability API (TreeSHAP + Counterfactuals)
    exp_status, exp_resp = request(f"{ml_url}/ml/v1/risk/explain", method="POST", body=sample_patient)
    if exp_status == 200 and isinstance(exp_resp, dict):
        shap_expl = exp_resp.get("explanation", {})
        cf_res = exp_resp.get("counterfactuals", {})
        drivers = shap_expl.get("top_risk_drivers", [])
        plans = cf_res.get("plans", [])
        disclaimer = exp_resp.get("regulatory_disclaimer")
        
        has_shap = len(drivers) > 0
        has_cf = len(plans) > 0
        has_disclaimer = bool(disclaimer)

        if has_shap and has_cf and has_disclaimer:
            log_pass("Explainability API (POST /ml/v1/risk/explain)", 
                     f"SHAP Drivers: {len(drivers)}, Counterfactual Plans: {len(plans)}")
        else:
            log_fail("Explainability API", f"Incomplete response structure: shap={has_shap}, cf={has_cf}, disc={has_disclaimer}")
            all_passed = False
    else:
        log_fail("Explainability API", f"HTTP {exp_status}: {exp_resp}")
        all_passed = False

    # -------------------------------------------------------------------------
    # Summary
    # -------------------------------------------------------------------------
    print(f"\n{CYAN}================================================================={RESET}")
    if all_passed:
        print(f"{GREEN}       ALL PRODUCTION SMOKE TESTS PASSED SUCCESSFULLY!          {RESET}")
    else:
        print(f"{RED}       SMOKE TEST DETECTED DEGRADED SERVICES OR FAILURES!        {RESET}")
    print(f"{CYAN}================================================================={RESET}\n")

    return all_passed

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="CarePath Production Deployment Smoke Test Suite")
    parser.add_argument("--base-url", default="http://localhost:80", help="Frontend web URL")
    parser.add_argument("--backend-url", default="http://localhost:8080", help="Spring Boot backend URL")
    parser.add_argument("--ml-url", default="http://localhost:8000", help="FastAPI ML service URL")
    args = parser.parse_args()

    success = run_smoke_tests(args.base_url, args.backend_url, args.ml_url)
    sys.exit(0 if success else 1)
