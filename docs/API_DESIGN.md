# CarePath - REST API Design & Integration Specification

**Document Version:** 1.0.0  
**Protocols:** HTTPS / REST / JSON  
**API Gateways:** Spring Boot Enterprise API (`/api/v1/*`) & Internal ML Microservice API (`/ml/v1/*`)

---

## 1. Global API Conventions & Standards

### 1.1 Content Types & Encoding
* All endpoints consume and produce `application/json; charset=UTF-8`, except report download endpoints which produce `application/pdf`.
* Date and time fields adhere strictly to ISO 8601 UTC format: `YYYY-MM-DDTHH:mm:ss.sssZ` (e.g., `2026-09-26T14:30:00.000Z`).
* Field Naming:
  * Spring Boot External API: `camelCase` (standard for JavaScript/TypeScript frontend integration).
  * Internal ML Microservice API: `snake_case` (standard Python Pydantic conventions).

### 1.2 Unified Error Response Contract
Every error (4xx / 5xx) returned by the platform follows the RFC 7807 problem detail standard:

```json
{
  "timestamp": "2026-09-26T14:30:00.000Z",
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Validation failed for vital metric submission",
  "path": "/api/v1/patients/9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d/vitals",
  "validationErrors": [
    {
      "field": "value",
      "rejectedValue": 320.0,
      "rule": "Value must be physiologically plausible (Systolic BP must be between 60 and 260 mmHg)"
    }
  ],
  "traceId": "c8a49df2-a392-4912-bdf9-42b78a9c1824"
}
```

### 1.3 Standard Pagination & Sorting Query Parameters
* `page`: 0-indexed page number (default: `0`).
* `size`: Number of records per page (default: `20`, max: `100`).
* `sort`: Field and direction (e.g., `recordedAt,desc`).
* `startDate` & `endDate`: ISO 8601 temporal range filters.

---

## 2. Spring Boot External REST API Specification

### 2.1 Authentication & Session Management (`/api/v1/auth`)

#### 2.1.1 Register User
* **Method:** `POST /api/v1/auth/register`
* **Access:** Public
* **Request Body:**
```json
{
  "email": "sarah.jenkins@example.com",
  "password": "SecurePassword123!",
  "firstName": "Sarah",
  "lastName": "Jenkins",
  "phone": "+15551234567",
  "role": "ROLE_PATIENT"
}
```
* **Response (201 Created):**
```json
{
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "email": "sarah.jenkins@example.com",
  "role": "ROLE_PATIENT",
  "message": "User registered successfully. Please complete intake profile."
}
```

#### 2.1.2 Login & Token Issue
* **Method:** `POST /api/v1/auth/login`
* **Access:** Public
* **Request Body:**
```json
{
  "email": "sarah.jenkins@example.com",
  "password": "SecurePassword123!"
}
```
* **Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresInSeconds": 900,
  "user": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "email": "sarah.jenkins@example.com",
    "role": "ROLE_PATIENT",
    "firstName": "Sarah",
    "lastName": "Jenkins"
  }
}
```
*(Sets HttpOnly secure cookie for `refreshToken`).*

#### 2.1.3 Refresh Access Token
* **Method:** `POST /api/v1/auth/refresh`
* **Access:** Public (Requires valid Refresh Cookie)
* **Response (200 OK):** Returns new `accessToken`.

---

### 2.2 Patient Profile & Intake (`/api/v1/patients`)

#### 2.2.1 Get Patient Profile
* **Method:** `GET /api/v1/patients/{patientId}`
* **Headers:** `Authorization: Bearer <token>`
* **Response (200 OK):**
```json
{
  "patientId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "dateOfBirth": "1978-04-12",
  "biologicalSex": "FEMALE",
  "heightCm": 165.0,
  "baselineWeightKg": 72.5,
  "currentBmi": 26.63,
  "smokingStatus": "NEVER",
  "alcoholUse": "OCCASIONAL",
  "medicalHistory": {
    "hypertensionHistory": true,
    "gestationalDiabetes": true
  }
}
```

#### 2.2.2 Complete / Update Patient Intake
* **Method:** `POST /api/v1/patients/intake`
* **Headers:** `Authorization: Bearer <token>`
* **Request Body:**
```json
{
  "dateOfBirth": "1978-04-12",
  "biologicalSex": "FEMALE",
  "heightCm": 165.0,
  "baselineWeightKg": 72.5,
  "smokingStatus": "NEVER",
  "alcoholUse": "OCCASIONAL",
  "medicalHistory": {
    "hypertensionHistory": true
  },
  "initialLabs": [
    { "metricType": "SYSTOLIC_BP", "value": 132.0, "unit": "mmHg" },
    { "metricType": "DIASTOLIC_BP", "value": 84.0, "unit": "mmHg" },
    { "metricType": "FASTING_GLUCOSE", "value": 108.0, "unit": "mg/dL" },
    { "metricType": "HBA1C", "value": 5.8, "unit": "%" }
  ]
}
```
* **Response (201 Created):** Returns created profile and initial intake risk assessment.

---

### 2.3 Vital Metrics Management (`/api/v1/patients/{patientId}/vitals`)

#### 2.3.1 Ingest Single Vital Entry
* **Method:** `POST /api/v1/patients/{patientId}/vitals`
* **Headers:** `Authorization: Bearer <token>`
* **Request Body:**
```json
{
  "recordedAt": "2026-09-26T07:30:00.000Z",
  "metricType": "SYSTOLIC_BP",
  "value": 128.0,
  "unit": "mmHg",
  "measurementContext": "RESTING",
  "source": "MANUAL"
}
```
* **Response (201 Created):**
```json
{
  "id": "e39f3791-7643-41bb-b09b-6db3e477f13b",
  "patientId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "metricType": "SYSTOLIC_BP",
  "value": 128.0,
  "unit": "mmHg",
  "recordedAt": "2026-09-26T07:30:00.000Z",
  "isBaselineAnomaly": false,
  "zScore": 0.85
}
```

#### 2.3.2 Bulk Ingest Vitals (e.g., Morning Battery)
* **Method:** `POST /api/v1/patients/{patientId}/vitals/batch`
* **Headers:** `Authorization: Bearer <token>`
* **Request Body:**
```json
{
  "recordedAt": "2026-09-26T07:30:00.000Z",
  "entries": [
    { "metricType": "SYSTOLIC_BP", "value": 128.0, "unit": "mmHg" },
    { "metricType": "DIASTOLIC_BP", "value": 82.0, "unit": "mmHg" },
    { "metricType": "HEART_RATE", "value": 68.0, "unit": "bpm" },
    { "metricType": "FASTING_GLUCOSE", "value": 104.0, "unit": "mg/dL" }
  ]
}
```
* **Response (201 Created):** Array of created records.

#### 2.3.3 Query Longitudinal Vitals (Time-Series)
* **Method:** `GET /api/v1/patients/{patientId}/vitals?metricType=SYSTOLIC_BP&startDate=2026-08-01T00:00:00Z&endDate=2026-09-26T23:59:59Z&page=0&size=50`
* **Headers:** `Authorization: Bearer <token>`
* **Response (200 OK):**
```json
{
  "content": [
    {
      "id": "e39f3791-7643-41bb-b09b-6db3e477f13b",
      "recordedAt": "2026-09-26T07:30:00.000Z",
      "metricType": "SYSTOLIC_BP",
      "value": 128.0,
      "unit": "mmHg",
      "context": "RESTING"
    }
  ],
  "page": 0,
  "size": 50,
  "totalElements": 48,
  "totalPages": 1
}
```

---

### 2.4 Personal Baseline & Trends (`/api/v1/patients/{patientId}/baselines`)

#### 2.4.1 Get Active Personal Baselines
* **Method:** `GET /api/v1/patients/{patientId}/baselines`
* **Headers:** `Authorization: Bearer <token>`
* **Response (200 OK):**
```json
{
  "patientId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "evaluatedWindowDays": 30,
  "baselines": [
    {
      "metricType": "SYSTOLIC_BP",
      "meanValue": 122.4,
      "medianValue": 121.0,
      "stdDeviation": 5.2,
      "p25Value": 118.0,
      "p75Value": 125.0,
      "ewmaValue": 124.1,
      "sampleCount": 28,
      "current14DaySlope": 0.42,
      "slopeSignificancePValue": 0.038,
      "trendClassification": "MILD_UPWARD_DRIFT"
    },
    {
      "metricType": "FASTING_GLUCOSE",
      "meanValue": 98.2,
      "medianValue": 97.0,
      "stdDeviation": 4.1,
      "p25Value": 95.0,
      "p75Value": 101.0,
      "ewmaValue": 102.5,
      "sampleCount": 26,
      "current14DaySlope": 0.55,
      "slopeSignificancePValue": 0.012,
      "trendClassification": "SUSTAINED_ELEVATION"
    }
  ]
}
```

---

### 2.5 Explainable Risk & Counterfactuals (`/api/v1/patients/{patientId}/risks`)

#### 2.5.1 Get Latest Risk Evaluation with SHAP Attribution
* **Method:** `GET /api/v1/patients/{patientId}/risks/latest`
* **Headers:** `Authorization: Bearer <token>`
* **Response (200 OK):**
```json
{
  "assessmentId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "timestamp": "2026-09-26T07:31:00.000Z",
  "overallRiskScore": 0.48,
  "riskCategory": "MODERATE",
  "confidenceLevel": "LONGITUDINAL_ROBUST",
  "regulatoryDisclaimer": "CarePath provides decision-support risk signals, not clinical diagnoses. Consult a physician.",
  "shapExplanation": {
    "baseValue": 0.31,
    "featureContributions": {
      "fastingGlucoseEwma": 0.085,
      "systolicBpSlope14d": 0.062,
      "bmi": 0.041,
      "age": 0.032,
      "hdlCholesterol": -0.050
    },
    "topRiskDrivers": [
      {
        "feature": "fastingGlucoseEwma",
        "label": "Recent Fasting Blood Glucose Elevation",
        "shapValue": 0.085,
        "observedValue": "104.0 mg/dL",
        "narrative": "Fasting glucose is averaging 5.8 mg/dL above your 30-day baseline."
      },
      {
        "feature": "systolicBpSlope14d",
        "label": "14-Day Systolic Blood Pressure Trend",
        "shapValue": 0.062,
        "observedValue": "+0.42 mmHg/day",
        "narrative": "Consistent upward trajectory over the past two weeks."
      }
    ],
    "topProtectiveFactors": [
      {
        "feature": "hdlCholesterol",
        "label": "Optimal HDL ('Good') Cholesterol",
        "shapValue": -0.050,
        "observedValue": "58.0 mg/dL",
        "narrative": "Healthy HDL levels are buffering against higher cardiometabolic risk."
      }
    ],
    "patientSummaryNarrative": "Your overall health score reflects moderate cardiometabolic risk. While your HDL cholesterol is healthy, an upward drift in morning blood sugar and blood pressure has contributed to a slight increase in your risk score."
  }
}
```

#### 2.5.2 Execute Counterfactual Simulation ("What-If" Analysis)
* **Method:** `POST /api/v1/patients/{patientId}/risks/counterfactual`
* **Headers:** `Authorization: Bearer <token>`
* **Request Body:**
```json
{
  "modifiedFeatures": {
    "systolicBp": 120.0,
    "fastingGlucose": 96.0,
    "sleepHours": 7.5
  }
}
```
* **Response (200 OK):**
```json
{
  "originalRiskScore": 0.48,
  "originalCategory": "MODERATE",
  "simulatedRiskScore": 0.22,
  "simulatedCategory": "LOW",
  "riskScoreDelta": -0.26,
  "achievableTierTransition": true,
  "explanation": "Targeting a systolic blood pressure of 120 mmHg and stabilizing fasting glucose below 100 mg/dL could lower your risk signal into the Low tier."
}
```

---

### 2.6 Clinical Alerts Management (`/api/v1/patients/{patientId}/alerts`)

#### 2.6.1 List Alerts
* **Method:** `GET /api/v1/patients/{patientId}/alerts?status=NEW`
* **Response (200 OK):**
```json
{
  "alerts": [
    {
      "id": "5f3a2c1b-90e8-4682-938b-d779a1f28b49",
      "severity": "WARNING",
      "alertType": "SUSTAINED_TREND",
      "status": "NEW",
      "title": "Sustained Upward Glucose Trajectory",
      "description": "Fasting blood glucose has risen for 14 consecutive days (slope: +0.55 mg/dL/day).",
      "createdAt": "2026-09-26T07:31:00.000Z"
    }
  ]
}
```

#### 2.6.2 Acknowledge / Resolve Alert
* **Method:** `PATCH /api/v1/patients/{patientId}/alerts/{alertId}`
* **Request Body:**
```json
{
  "status": "ACKNOWLEDGED",
  "clinicianNotes": "Reviewed with patient. Advised low-glycemic dietary modifications for 2 weeks."
}
```

---

### 2.7 Reports & Clinician Delegation

#### 2.7.1 Download Doctor-Ready PDF Report
* **Method:** `GET /api/v1/patients/{patientId}/reports/doctor-summary.pdf?timeframeDays=90`
* **Headers:** `Authorization: Bearer <token>`, `Accept: application/pdf`
* **Response (200 OK):** Binary PDF Stream (`Content-Disposition: attachment; filename="CarePath-ClinicalSummary-SarahJenkins.pdf"`).

#### 2.7.2 Generate Ephemeral Clinician Access Grant Code
* **Method:** `POST /api/v1/patients/{patientId}/delegation/generate-code`
* **Response (200 OK):**
```json
{
  "grantCode": "CARE-8492-XQ7",
  "expiresAt": "2026-09-27T14:30:00.000Z",
  "instructions": "Provide this 6-character code to your doctor to authorize 30-day access to your CarePath chart."
}
```

---

## 3. Internal ML Microservice REST API (FastAPI)

These endpoints are strictly internal to the VPC/Docker network and invoked exclusively by Spring Boot.

### 3.1 Health & Model Metadata
* **Method:** `GET /ml/v1/health`
* **Response (200 OK):**
```json
{
  "status": "HEALTHY",
  "modelVersion": "carepath-gbm-v1.0.0",
  "calibrated": true,
  "shapExplainerType": "TreeExplainer",
  "featuresSupported": 18
}
```

### 3.2 Predict Risk & Compute SHAP
* **Method:** `POST /ml/v1/risk/predict`
* **Request Body:**
```json
{
  "patient_uuid": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
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
```
* **Response (200 OK):**
```json
{
  "model_version": "carepath-gbm-v1.0.0",
  "overall_risk_score": 0.4812,
  "risk_category": "MODERATE",
  "shap_base_value": 0.3120,
  "shap_values": {
    "age": 0.0321,
    "biological_sex": -0.0150,
    "bmi": 0.0410,
    "smoking_status": -0.0210,
    "systolic_bp_current": 0.0215,
    "diastolic_bp_current": 0.0110,
    "heart_rate_current": -0.0050,
    "fasting_glucose_current": 0.0420,
    "hba1c_current": 0.0310,
    "cholesterol_total": 0.0080,
    "cholesterol_hdl": -0.0500,
    "cholesterol_ldl": 0.0120,
    "triglycerides": 0.0060,
    "sleep_hours_current": 0.0180,
    "systolic_bp_ewma_delta": 0.0410,
    "fasting_glucose_ewma_delta": 0.0850,
    "systolic_bp_slope_14d": 0.0620,
    "fasting_glucose_slope_14d": 0.0780
  },
  "top_drivers": ["fasting_glucose_ewma_delta", "fasting_glucose_slope_14d", "systolic_bp_slope_14d"],
  "top_protectors": ["cholesterol_hdl", "smoking_status", "biological_sex"]
}
```
