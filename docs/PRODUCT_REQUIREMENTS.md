# CarePath - Product Requirements Document (PRD)

**Document Version:** 1.0.0  
**Status:** Approved Architecture Draft  
**Target Systems:** Web Platform (Patient Portal, Clinician Dashboard)

---

## 1. Product Overview & Mission

### 1.1 Executive Summary
**CarePath** is an enterprise-grade, longitudinal health-monitoring and explainable risk-analysis platform. Traditional healthcare applications either present raw data tables without context or generate black-box diagnostic claims that raise safety and regulatory concerns. CarePath bridges this gap by:
1. Recording multi-modal biometric measurements and patient-reported symptoms over time.
2. Dynamically computing an individualized personal baseline rather than relying solely on generic population percentiles.
3. Detecting meaningful longitudinal trends, slopes, and trajectory shifts while filtering transient noise.
4. Providing machine-learning-derived cardiometabolic risk signals with transparent, local attribution via **SHAP (SHapley Additive exPlanations)**.
5. Offering an interactive counterfactual simulation engine ("What-If" analysis) to demonstrate how potential lifestyle or biometric modifications could shift a patient's risk trajectory.
6. Generating structured, high-density, doctor-ready clinical consultation reports alongside patient-friendly health literacy summaries.

### 1.2 Mission Statement
To empower patients and healthcare providers with transparent, continuous, and explainable health trajectory insights, fostering proactive collaboration while upholding the highest standards of safety, privacy, and scientific rigor.

---

## 2. Regulatory Boundaries & Non-Diagnostic Guardrails

### 2.1 Clinical Decision Support (CDS) Classification
CarePath is explicitly architected and positioned as a **Clinical Decision Support / Health Tracking System**, aligned with the non-device criteria outlined in Section 520(o)(1)(E) of the U.S. FDA 21st Century Cures Act and EU MDR Rule 11 (Software as a Medical Device - Class I boundary support).

### 2.2 Strict Non-Diagnostic Constraints
1. **No Autonomous Diagnosis:** The software will never state, imply, or label any condition as a diagnosed disease (e.g., the platform will never state "You have Type 2 Diabetes" or "Hypertension Stage 2 Confirmed"). Instead, it will output:
   * *"Elevated cardiometabolic risk signal detected (Score: 0.68/1.00)."*
   * *"Primary contributing factor: 30-day upward trajectory in fasting blood glucose (+18 mg/dL above personal baseline)."*
2. **No Treatment Prescriptions or Clinical Triage:** The system will not recommend specific drug names, dosages, or self-treatment regimens. It provides educational correlations and points out actionable factors for clinical discussion.
3. **Mandatory UI Disclaimers:**
   * Every page, report header, and prediction visualization must prominently feature the permanent regulatory disclaimer:
     > *"CarePath is an informational and risk-stratification decision-support tool. It does not provide medical diagnoses, treatment advice, or emergency triage. Always consult a qualified healthcare professional before making clinical decisions."*
4. **Emergency Red Flag Safeguards:** If recorded metrics breach life-threatening boundaries (e.g., Systolic BP > 180 mmHg with severe headache, or SpO2 < 88%), the system bypasses standard ML evaluation and displays an immediate, prominent clinical safety alert urging the user to seek immediate emergency medical care.

---

## 3. User Personas & Core Workflows

### 3.1 Personas

| Persona | Role | Primary Goals | Key Pain Points |
| :--- | :--- | :--- | :--- |
| **Sarah (Patient)** | 48-year-old pre-diabetic professional | Tracks vitals; wants to understand what her daily numbers actually mean; desires actionable steps to prevent chronic progression. | Confused by complex lab numbers; anxious about unexplained fluctuations; forgets long-term vitals during doctor visits. |
| **Dr. Evans (Clinician)** | Primary Care Physician (PCP) | Needs a fast, high-density summary of patient trends between visits; values explainability over black-box AI. | 15-minute appointment constraints; overwhelmed by raw logs with no signal-to-noise separation; skeptical of opaque AI. |
| **Marcus (Admin)** | Compliance & Operations Officer | Ensures HIPAA/GDPR compliance, oversees user access controls, monitors ML service health and audit logs. | Unaudited data mutations; silent ML model drift; credential vulnerabilities. |

### 3.2 User Journeys

#### Journey A: Patient Daily Tracking & Trend Inspection
1. Sarah logs into CarePath on her laptop/mobile browser using MFA/JWT authentication.
2. She enters her morning blood pressure (128/82 mmHg) and fasting glucose (104 mg/dL).
3. The platform validates values, records the timestamped entry, and runs background baseline updating.
4. The dashboard displays her personal 30-day normal baseline band. She sees that while 128 mmHg is within general population limits, it represents a slight upward deviation from her personal baseline (118 mmHg).
5. The ML engine updates her risk score and highlights that reduced sleep and increased glucose slope contributed most to this week's delta.
6. She opens the Counterfactual Simulation tab and observes: *"Reducing resting systolic BP to 120 mmHg and stabilizing fasting glucose below 100 mg/dL is estimated to decrease your risk signal from 'Moderate' to 'Low'."*

#### Journey B: Clinician Pre-Consultation Review
1. Dr. Evans receives an access grant code from Sarah during an annual checkup.
2. Dr. Evans opens the Clinician Portal and accesses Sarah's longitudinal file.
3. The view renders:
   * A 90-day time-series vitals chart with personal baseline corridors and statistical anomaly flags.
   * A TreeSHAP feature waterfall diagram displaying the exact mathematical weight of each biomarker in Sarah's current risk score.
   * A synthesized, one-click downloadable PDF "Clinical Summary Report" with standardized observation tables, slope calculations, and medication correlation annotations.
4. Dr. Evans uses these insights to have an informed 10-minute consultation focused on metabolic stabilization.

---

## 4. Functional Requirements (FR)

### Module 1: Identity, Authentication & Role-Based Access Control (RBAC)
* **FR-1.1:** Support registration and login for `PATIENT`, `CLINICIAN`, and `ADMIN` roles.
* **FR-1.2:** Enforce password complexity (min 12 chars, mixed case, numbers, special characters) with BCrypt hashing.
* **FR-1.3:** Implement stateless JSON Web Token (JWT) authentication with short-lived Access Tokens (15 min) and Refresh Tokens (7 days) stored in Secure, HttpOnly, SameSite cookies.
* **FR-1.4:** Patient-Clinician Delegation: Patients can generate secure time-limited access grants (e.g., 30-day or indefinite revocable tokens) allowing specific verified clinicians to review their profile and longitudinal charts.
* **FR-1.5:** Audit Trail: Log every auth event, credential change, data access, and clinical report download into an immutable audit table.

### Module 2: Health Profile & Demographic Baseline Intake
* **FR-2.1:** Collect initial demographic and static baseline attributes: Date of birth (age calculation), biological sex, height, initial weight/BMI, smoking status (Current, Former, Never), alcohol intake, and self-reported family history of cardiovascular/metabolic illness.
* **FR-2.2:** Support demographic profile updates with versioned history.

### Module 3: Vitals & Biomarker Ingestion Engine
* **FR-3.1:** Support single-entry and bulk-entry logging for the following standardized metrics:
  * Blood Pressure: Systolic and Diastolic (mmHg)
  * Heart Rate: Resting (bpm)
  * Fasting Blood Glucose (mg/dL)
  * Hemoglobin A1c (HbA1c, %)
  * Lipid Panel: Total Cholesterol, HDL, LDL, Triglycerides (mg/dL)
  * Body Weight (kg) & Auto-calculated BMI ($kg/m^2$)
  * Blood Oxygen Saturation ($SpO_2$, %)
  * Sleep Duration (hours) & Daily Physical Activity (steps)
* **FR-3.2:** Context Tagging: Allow tagging entries with measurement conditions (e.g., `FASTING`, `POST_PRANDIAL`, `RESTING`, `POST_EXERCISE`).
* **FR-3.3:** Input Range Validation: Enforce physiological bounds at the API level (e.g., Systolic [60-260], Diastolic [40-160], Heart Rate [30-220], $SpO_2$ [50-100]). Reject unphysiological values with descriptive 400 Bad Request responses.

### Module 4: Patient-Reported Symptoms & Notes
* **FR-4.1:** Allow logging structured subjective symptoms (e.g., Fatigue, Dizziness, Chest Discomfort, Shortness of Breath, Headache, Joint Pain).
* **FR-4.2:** Rate severity on a standardized 1–10 visual analog scale (VAS).
* **FR-4.3:** Support optional free-text clinical notes (up to 1,000 characters) associated with any symptom or vital entry.

### Module 5: Dynamic Personal Baseline Engine
* **FR-5.1:** Compute a rolling personal baseline for each metric using a configurable historical window (default: 30 days, min required entries: 5).
* **FR-5.2:** Compute statistical parameters per patient and metric:
  * Exponentially Weighted Moving Average (EWMA) to prioritize recent measurements without discarding historical context.
  * Rolling arithmetic mean ($\mu$) and standard deviation ($\sigma$).
  * Interquartile Range (IQR, $Q_{25}$, $Q_{75}$) and median to establish outlier-resistant normal bands.
* **FR-5.3:** Identify individual-specific normal boundaries rather than static clinical ranges alone (e.g., a patient whose baseline resting heart rate is consistently 54 bpm vs. a patient whose baseline is 78 bpm).

### Module 6: Trend & Trajectory Detection
* **FR-6.1:** Trajectory Estimation: Calculate the rate of change (Ordinary Least Squares slope $\beta$) over 7, 14, and 30-day windows.
* **FR-6.2:** Anomaly Classification:
  * *Transient Fluctuation (Spike):* Single measurement exceeding $\mu \pm 2.5\sigma$ that returns to baseline within 24–48 hours.
  * *Sustained Drift:* $\ge 3$ consecutive measurements deviating in the same direction with statistically significant slope ($p < 0.05$).
  * *High Variability / Instability:* Coefficient of Variation ($CV = \sigma / \mu$) exceeding established stability thresholds over 14 days.

### Module 7: Machine Learning Risk Stratification Engine
* **FR-7.1:** Multi-Factor Risk Assessment: Ingest demographic vectors, current metric states, baseline delta scores, and trajectory slopes to produce a normalized risk signal $R \in [0.00, 1.00]$.
* **FR-7.2:** Tier Categorization:
  * `LOW` ($0.00 \le R < 0.25$): Stable longitudinal profile; metrics conform to baseline and healthy clinical bounds.
  * `MODERATE` ($0.25 \le R < 0.50$): Mild baseline deviation or single biomarker elevation.
  * `ELEVATED` ($0.50 \le R < 0.75$): Multi-marker drift or sustained upward trajectory in primary risk biomarkers.
  * `HIGH` ($0.75 \le R \le 1.00$): Marked multi-system instability or severe baseline divergence warranting priority clinical review.
* **FR-7.3:** "Cold-Start" Intake Scoring: If a new patient has $< 5$ historical records, the model performs an intake baseline assessment using age, sex, BMI, and initial lab values, explicitly marking the prediction confidence as `PRELIMINARY_INTAKE`.
* **FR-7.4:** Existing-Patient Longitudinal Scoring: Incorporates rolling baseline z-scores and 14-day trajectory slopes, marking prediction confidence as `LONGITUDINAL_ROBUST`.

### Module 8: Explainability (SHAP) & Attribution Engine
* **FR-8.1:** Compute exact TreeSHAP local attributions for every risk evaluation.
* **FR-8.2:** Decompose risk into:
  * Base Value ($\phi_0$): Expected value across the training population.
  * Feature SHAP Values ($\phi_i$): Individual additive contributions such that $\sum \phi_i + \phi_0 = f(x)$.
* **FR-8.3:** Categorize top risk factors into:
  * *Risk Drivers ($\phi_i > 0$):* Biomarkers pushing the score toward higher risk.
  * *Protective Factors ($\phi_i < 0$):* Biomarkers stabilizing the score toward lower risk.
* **FR-8.4:** Natural Language Generation (NLG): Translate numeric SHAP attributions into patient-friendly educational prose without clinical jargon.

### Module 9: Actionable Counterfactual Simulation ("What-If" Analysis)
* **FR-9.1:** Allow patients to interact with dynamic sliders on modifiable biomarkers (e.g., Systolic BP, Fasting Glucose, Weight/BMI, Sleep Duration).
* **FR-9.2:** Re-evaluate the ML pipeline on the simulated vector in real-time ($< 250$ ms).
* **FR-9.3:** Compute and visualize the delta in risk score and projected tier transition (e.g., *"If you achieve a 10 mmHg reduction in systolic blood pressure and maintain 7.5 hours of sleep, your projected risk drops from 0.58 [Elevated] to 0.42 [Moderate]"*).
* **FR-9.4:** Guardrails on Counterfactuals: Restrict slider ranges to physiologically plausible and safe transitions (e.g., maximum systolic BP drop of 25 mmHg; cannot simulate BP $< 90$ mmHg). Lock immutable variables (Age, Biological Sex).

### Module 10: Clinical Alerts & Notifications
* **FR-10.1:** Categorize alerts into 4 severity levels: `INFO`, `WARNING`, `URGENT`, `CRITICAL`.
* **FR-10.2:** Trigger types:
  * *Critical Safety Threshold:* Life-threatening vitals (e.g., $SpO_2 < 88\%$, Systolic $> 180$ mmHg).
  * *Sustained Adverse Drift:* 14-day continuous upward trend in fasting glucose or blood pressure.
  * *Risk Tier Escalation:* Patient transitions from `MODERATE` to `ELEVATED` or `HIGH`.
* **FR-10.3:** Alert Lifecycle: Support alert viewing, acknowledgment, clinician notation, and resolution timestamps.
* **FR-10.4:** Event-Driven Notification Framework: Decoupled event hooks ready to route alerts to email, SMS, or webhooks.

### Module 11: Clinical Reports & Summaries
* **FR-11.1:** Patient-Friendly Health Summary: A visual, easy-to-read summary explaining current stability, positive improvements, and areas to monitor.
* **FR-11.2:** Doctor-Ready Clinical Consultation Report:
  * Standardized header with patient demographics, recording date range, and verification timestamp.
  * 30/90-day vitals statistical summary table (Mean, Min, Max, Standard Deviation, Trajectory Slope).
  * High-resolution charts showing measurement points superimposed over personal baseline corridors.
  * Model-based risk index breakdown with SHAP feature contribution waterfall.
  * Symptom logs, medication notes, and critical threshold breach occurrences.
  * High-fidelity printable stylesheet and PDF generation endpoint.

---

## 5. Non-Functional Requirements (NFR)

### 5.1 Security, Privacy & Compliance
* **NFR-1.1 (Encryption):** All data in transit must use TLS 1.3. Sensitive database columns and backups must be encrypted at rest using AES-256.
* **NFR-1.2 (HIPAA & GDPR Alignment):** No Protected Health Information (PHI) or personal identifiers (name, email) sent across internal ML microservice boundaries; ML service operates solely on pseudonymized identifiers (`patient_uuid`).
* **NFR-1.3 (Zero-Trust Session Management):** JWT tokens signed using RS256 or HMAC-SHA256 with rotating secrets; tokens verified on every protected request.
* **NFR-1.4 (Input Sanitization):** Strict OWASP Top 10 protection: SQL injection prevention via Hibernate parameterized queries, XSS sanitization on free-text symptom notes, CORS locked to trusted origins.

### 5.2 Performance & Latency
* **NFR-2.1:** Standard REST API responses (vitals retrieval, baseline queries) $< 150$ ms at 95th percentile.
* **NFR-2.2:** End-to-end ML Risk Inference + TreeSHAP feature attribution $< 800$ ms.
* **NFR-2.3:** Interactive Counterfactual Simulation $< 300$ ms.
* **NFR-2.4:** PDF Clinical Report generation $< 3.0$ seconds.

### 5.3 Reliability, Availability & Fault Tolerance
* **NFR-3.1 (Decoupled Graceful Degradation):** If the Python FastAPI ML microservice is unreachable or restarts, the Spring Boot core backend must remain 100% operational. Core vitals logging, baseline calculation, and historical charting must continue without interruption, falling back to a cached risk score or an explicit `"ML Decision Support Temporarily Unavailable"` UI badge.
* **NFR-3.2 (Data Durability):** PostgreSQL write transactions must adhere strictly to ACID guarantees.

### 5.4 Maintainability & Code Quality
* **NFR-4.1:** Clean architecture separation: Presentation (React), Orchestration & Domain Logic (Spring Boot), Data Persistence (PostgreSQL), Explainable Inference (FastAPI).
* **NFR-4.2:** Automated test coverage target $\ge 80\%$ across backend unit/integration tests (JUnit 5, Mockito, Spring Boot Test) and ML service test suite (Pytest).
* **NFR-4.3:** Type safety across full stack: TypeScript strict mode enabled on frontend; strong domain DTO typing on Spring Boot and Pydantic v2 schemas on FastAPI.

---

## 6. Future Extensibility Requirements

* **EXT-1 (Wearable Integration Pipeline):** Extensible ingestion adapter pattern supporting standard FHIR Observation resources and webhooks from Apple HealthKit, Google Health Connect, and Garmin Health APIs.
* **EXT-2 (Asynchronous Notification Service):** Pluggable messaging broker integration (RabbitMQ / Kafka / AWS SQS) for multi-channel notification delivery (FCM Push, SendGrid Email, Twilio SMS).
* **EXT-3 (Clinician EHR Integration):** Capability to export reports as HL7 CDA or FHIR DiagnosticReport resources.
