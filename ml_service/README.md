# CarePath - Machine Learning Microservice (`ml_service`)

## 1. Overview
The CarePath ML Microservice is an internal Python 3.12 / FastAPI microservice that computes continuous, calibrated **Cardiometabolic Risk Signals** $R \in [0.00, 1.00]$, maps them to clinical decision-support tiers (`LOW`, `MODERATE`, `ELEVATED`, `HIGH`), computes exact **TreeSHAP** patient-level feature attributions, and generates **constrained, actionable counterfactual targets**.

### Critical Clinical Guardrails
* **NON-DIAGNOSTIC NOTICE**: Model outputs represent statistical decision-support risk signals and feature attributions, NOT clinical diagnoses, medical conclusions, or treatment directives.
* **SHAP INTERPRETABILITY DISCLAIMER**:
  > SHAP explanations describe how the trained model attributes a prediction; they do not establish causal relationships.
* **COUNTERFACTUAL RECOMMENDATION NOTICE**:
  > Counterfactual targets are model-oriented simulation targets, not medical advice, drug prescriptions, or clinical orders. Consult a licensed physician.

---

## 2. Feature Vector Taxonomy (18 Dimensions)
The model consumes an 18-dimensional feature vector $\mathbf{x} = [\mathbf{x}_{\text{static}}, \mathbf{x}_{\text{instant}}, \mathbf{x}_{\text{longitudinal}}]$ defined in `docs/ML_DESIGN.md`:

| # | Feature Name | Clinical Description | Modifiable in CF | Type / Valid Bounds |
|---|:---|:---|:---:|:---|
| 1 | `age` | Patient chronological age | ❌ No | `float` [18.0, 120.0] |
| 2 | `biological_sex` | Biological sex | ❌ No | `FEMALE` (0.0) / `MALE` (1.0) |
| 3 | `bmi` | Body Mass Index ($kg/m^2$) | ✅ Yes | `float` [10.0, 100.0] |
| 4 | `smoking_status` | Tobacco use status | ✅ Yes | `NEVER` (0.0) / `FORMER` (1.0) / `CURRENT` (2.0) |
| 5 | `systolic_bp_current` | Current Systolic BP ($mmHg$) | ✅ Yes | `float` [40.0, 300.0] |
| 6 | `diastolic_bp_current` | Current Diastolic BP ($mmHg$) | ✅ Yes | `float` [30.0, 200.0] |
| 7 | `heart_rate_current` | Resting Heart Rate ($bpm$) | ✅ Yes | `float` [20.0, 260.0] |
| 8 | `fasting_glucose_current`| Fasting Blood Glucose ($mg/dL$)| ✅ Yes | `float` [20.0, 800.0] |
| 9 | `hba1c_current` | Glycated Hemoglobin ($\%$) | ✅ Yes | Optional `float` [3.0, 20.0] |
| 10| `cholesterol_total` | Total Serum Cholesterol ($mg/dL$) | ✅ Yes | Optional `float` [50.0, 800.0] |
| 11| `cholesterol_hdl` | High-Density Lipoprotein ($mg/dL$) | ✅ Yes | Optional `float` [5.0, 200.0] |
| 12| `cholesterol_ldl` | Low-Density Lipoprotein ($mg/dL$) | ✅ Yes | Optional `float` [10.0, 600.0] |
| 13| `triglycerides` | Serum Triglycerides ($mg/dL$) | ✅ Yes | Optional `float` [10.0, 2000.0] |
| 14| `sleep_hours_current` | Sleep Duration ($hours/night$) | ✅ Yes | Optional `float` (0.0, 24.0] |
| 15| `systolic_bp_ewma_delta`| Current vs 30d Baseline EWMA $\Delta$ ($mmHg$) | ❌ No | Optional `float` |
| 16| `fasting_glucose_ewma_delta`| Current vs 30d Baseline EWMA $\Delta$ ($mg/dL$) | ❌ No | Optional `float` |
| 17| `systolic_bp_slope_14d` | 14-day OLS trajectory slope ($mmHg/day$) | ❌ No | Optional `float` |
| 18| `fasting_glucose_slope_14d`| 14-day OLS trajectory slope ($mg/dL/day$) | ❌ No | Optional `float` |

---

## 3. Explainability Architecture (SHAP + Counterfactuals)

```
Patient Feature Vector
         ↓
  CarePathPreprocessor (Encoding & Imputation)
         ↓
  CalibratedClassifierCV (HistGradientBoosting 5-Fold Ensemble)
         ↓
  Predicted Risk Score & Tier
         ├──► SHAP TreeExplainer Ensemble
         │       ↓
         │    Exact Signed Shapley Attributions (φ)
         │    • Base Value (Population Baseline)
         │    • Top-K Risk Drivers (φ > 0)
         │    • Top-K Protective Factors (φ < 0)
         │
         └──► Constrained Counterfactual Engine
                 ↓
              Actionable Target Plans
              • Bounds & Safety Clamps
              • Modifiable Feature Search
              • Simulated Risk Reduction & Tier Transitions
```

### 3.1 SHAP Methodology & TreeExplainer
* **Why `TreeExplainer`?** Unlike model-agnostic explainers (e.g., KernelSHAP) which rely on sampling approximations and exponential complexity, `TreeExplainer` calculates exact Shapley values in polynomial time ($O(TLD^2)$) by traversing tree graph topologies directly.
* **Calibrated Ensemble Integration:** Because `CalibratedClassifierCV` is a calibration meta-wrapper, our service extracts the underlying tree estimators across all cross-validation folds, generates fold-specific exact Shapley attributions, and computes the ensemble mean.
* **Local Additivity:** Satisfies exact efficiency: $\text{Margin}(x) = \phi_0 + \sum_{j=1}^{18} \phi_j(x)$ down to numerical precision ($< 10^{-6}$).
* **Interpretation of Signed SHAP Values:**
  * $\phi_j > 0$ (**Risk Driver**): The patient's observed feature value pushed the model's prediction higher than the reference population baseline.
  * $\phi_j < 0$ (**Protective Factor**): The feature value pulled the model's prediction lower than the baseline.
  * *Notice: SHAP explains model mechanics, NOT medical causation.*

### 3.2 Constrained Counterfactual Engine
* **Objective:** Generates realistic, actionable biomarker/lifestyle modifications that move a patient into a lower risk category.
* **Safety Constraints:**
  * **Strict Immutability:** Demographics (`age`, `biological_sex`) and historical trajectory metrics (`ewma_delta`, `slope_14d`) are strictly immutable.
  * **Physiological Clamps:** Prevents unachievable or dangerous simulations (e.g., systolic BP cannot drop below 100 mmHg; glucose cannot drop below 75 mg/dL; BMI cannot drop more than 5.0 points instantaneously).
  * **Parsimony & Feasibility:** Favors single-factor and high-yield dual-factor lifestyle targets (e.g., sleep regularisation, systolic blood pressure normalisation, smoking cessation).
  * **Non-Prescriptive Rationale:** Generates model-oriented associations (e.g., *"Reducing modeled systolic blood pressure by 35 mmHg is associated with a risk score reduction of 0.24"*).
  * **Structured Fallback:** If a patient is already in the `LOW` risk category ($R < 0.20$), or if no safe combination yields significant risk reduction ($\Delta R \ge 0.01$), the system returns `NO_VALID_COUNTERFACTUAL_FOUND` with a structured explanatory message rather than fabricating unrealistic targets.

---

## 4. Endpoints & API Specification

### 4.1 `GET /ml/v1/health`
Checks service health and model status:
```json
{
  "status": "HEALTHY",
  "service_name": "CarePath ML Risk Service",
  "service_version": "1.0.0",
  "modelVersion": "carepath-gbm-v1.0.0",
  "model_loaded": true,
  "calibrated": true,
  "shapExplainerType": "TreeExplainer",
  "featuresSupported": 18
}
```

### 4.2 `POST /ml/v1/risk/predict`
Calculates calibrated cardiometabolic risk signal and risk category.

### 4.3 `POST /ml/v1/risk/explain`
Unified explainability endpoint returning prediction, SHAP waterfall attributions, and constrained counterfactual target plans:

**Request Body:**
```json
{
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
  "generate_counterfactuals": true
}
```

**Response Body (200 OK):**
```json
{
  "prediction": {
    "model_version": "carepath-gbm-v1.0.0",
    "overall_risk_score": 0.7288,
    "risk_category": "ELEVATED",
    "prediction": 1,
    "probability": 0.7288,
    "calibrated": true,
    "confidence_level": "LONGITUDINAL_ROBUST",
    "regulatory_disclaimer": "CarePath provides decision-support risk signals, not clinical diagnoses. Consult a physician."
  },
  "explanation": {
    "base_value": -2.1684,
    "shap_values": {
      "age": 0.1250,
      "systolic_bp_current": 0.3540,
      "fasting_glucose_current": 0.2810,
      "cholesterol_hdl": -0.0450
    },
    "top_risk_drivers": [
      {
        "feature": "systolic_bp_current",
        "display_name": "Current Systolic Blood Pressure",
        "value": 148.0,
        "shap_value": 0.3540,
        "direction": "increases_risk",
        "impact": "high",
        "narrative": "Current Systolic Blood Pressure (148.0 mmHg) is a primary risk driver, increasing the model's predicted risk score (+0.3540)."
      }
    ],
    "protective_factors": [
      {
        "feature": "cholesterol_hdl",
        "display_name": "HDL ('Good') Cholesterol",
        "value": 40.0,
        "shap_value": -0.0450,
        "direction": "decreases_risk",
        "impact": "low",
        "narrative": "HDL ('Good') Cholesterol (40.0 mg/dL) serves as a protective factor, reducing the model's predicted risk score (-0.0450)."
      }
    ],
    "summary_narrative": "Modeled risk is predominantly influenced by Current Systolic Blood Pressure, Current Fasting Blood Glucose; buffering factors include HDL ('Good') Cholesterol. Feature attributions explain model decision weighting and do not represent medical conclusions."
  },
  "counterfactuals": {
    "counterfactual_available": true,
    "status": "SUCCESS",
    "message": "Identified 3 actionable counterfactual target plan(s).",
    "plans": [
      {
        "plan_id": "plan_dual_systolic_bp_current_fasting_glucose_current",
        "title": "Cardiometabolic Dual Target",
        "simulated_risk_score": 0.3249,
        "simulated_risk_category": "MODERATE",
        "risk_reduction": 0.4039,
        "tier_transition_achieved": true,
        "changed_features": [
          {
            "feature": "systolic_bp_current",
            "display_name": "Current Systolic Blood Pressure",
            "current_value": 148.0,
            "target_value": 113.0,
            "unit": "mmHg",
            "change_delta": -35.0,
            "allowed_range": [100.0, 200.0],
            "direction": "decrease",
            "modifiable": true
          },
          {
            "feature": "fasting_glucose_current",
            "display_name": "Current Fasting Blood Glucose",
            "current_value": 138.0,
            "target_value": 83.0,
            "unit": "mg/dL",
            "change_delta": -55.0,
            "allowed_range": [75.0, 300.0],
            "direction": "decrease",
            "modifiable": true
          }
        ],
        "rationale": "Combined modeled optimization of Current Systolic Blood Pressure and Current Fasting Blood Glucose yields a simulated risk score reduction of 0.4039 (from 0.7288 to 0.3249)."
      }
    ]
  },
  "regulatory_disclaimer": "CarePath provides decision-support risk signals, not clinical diagnoses. Consult a physician."
}
```

---

## 5. Local Setup & Testing

### 5.1 Run Automated Tests
```bash
pytest -v
```
All 47 unit and integration tests covering dataset generation, preprocessing, training, model loading, calibrated inference, SHAP TreeExplainer, counterfactual engine, and FastAPI endpoints will execute.

### 5.2 Start Local Microservice
```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000
```
* **Interactive API Documentation:** `http://localhost:8000/docs`
* **OpenAPI Schema:** `http://localhost:8000/openapi.json`
