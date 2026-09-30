# CarePath - Machine Learning & Explainability Specification

**Document Version:** 1.0.0  
**Runtime:** Python 3.11 / FastAPI  
**Frameworks:** scikit-learn, SHAP, NumPy, Pandas, Pydantic v2  
**Clinical Scope:** Explainable Multi-Factor Cardiometabolic Risk Stratification & Longitudinal Trend Analysis

---

## 1. Clinical Problem Formulation & Guardrails

### 1.1 Objective
The CarePath ML Engine does **not** diagnose medical conditions (e.g., it never predicts "Patient has Type 2 Diabetes" or "Patient has Essential Hypertension"). Instead, it estimates an uncalibrated logit transformed into a well-calibrated continuous **Cardiometabolic Risk Signal** $R \in [0.00, 1.00]$.

This score quantifies the statistical probability of a patient's multi-system biomarkers diverging into an adverse physiological decompensation state over a 3-year horizon, compared against empirical clinical cohort distributions (e.g., NHANES and Framingham Risk Study benchmarks).

### 1.2 Non-Diagnostic Regulatory Guardrails
* **Clinical Decision Support Boundary:** Every output is delivered as an interpretable probability paired with exact Shapley value attributions ($\phi_i$).
* **Deterministic Red-Flag Override:** If any physiological reading breaches life-threatening boundaries (e.g., Systolic BP $\ge 180$ mmHg, Diastolic BP $\ge 120$ mmHg, or $SpO_2 \le 88\%$), the ML inference pipeline is superseded by a deterministic Clinical Safety Rule, triggering an immediate emergency alert.

---

## 2. Feature Engineering & Taxonomy

The model consumes an 18-dimensional feature vector $\mathbf{x} = [\mathbf{x}_{\text{static}}, \mathbf{x}_{\text{instant}}, \mathbf{x}_{\text{longitudinal}}]$.

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Feature Vector Taxonomy                         │
├────────────────────┬──────────────────────────┬────────────────────────┤
│ Static Demographics│ Instantaneous Biomarkers │ Longitudinal Dynamics  │
├────────────────────┼──────────────────────────┼────────────────────────┤
│ • Age              │ • Systolic BP            │ • Systolic BP EWMA Δ   │
│ • Biological Sex   │ • Diastolic BP           │ • Glucose EWMA Δ       │
│ • BMI              │ • Resting Heart Rate     │ • Systolic 14-day Slope│
│ • Smoking Status   │ • Fasting Blood Glucose  │ • Glucose 14-day Slope │
│                    │ • HbA1c                  │ • Metric Instability CV│
│                    │ • Total Cholesterol      │                        │
│                    │ • HDL Cholesterol        │                        │
│                    │ • LDL Cholesterol        │                        │
│                    │ • Triglycerides          │                        │
│                    │ • Sleep Duration (hrs)   │                        │
└────────────────────┴──────────────────────────┴────────────────────────┘
```

### 2.1 Feature Definitions & Mathematical Formulations

#### 1. Static Demographics
* $\text{Age} \in [18, 100]$ (Years)
* $\text{Biological Sex} \in \{0: \text{Female}, 1: \text{Male}\}$
* $\text{BMI} = \frac{\text{Weight (kg)}}{(\text{Height (m)})^2} \in [14.0, 60.0]$
* $\text{Smoking Status} \in \{0: \text{Never}, 1: \text{Former}, 2: \text{Current}\}$

#### 2. Instantaneous Biomarkers (Latest Readings)
* $\text{Systolic BP}$ (mmHg), $\text{Diastolic BP}$ (mmHg), $\text{Heart Rate}$ (bpm)
* $\text{Fasting Glucose}$ (mg/dL), $\text{HbA1c}$ (%)
* $\text{Total Cholesterol}$, $\text{HDL}$, $\text{LDL}$, $\text{Triglycerides}$ (mg/dL)
* $\text{Sleep Duration}$ (hours/night)

#### 3. Dynamic Longitudinal Features
* **Baseline EWMA Delta ($\Delta_{\text{EWMA}}$):**
  $$\Delta_{\text{EWMA}} = x_{\text{current}} - \text{EWMA}_{30\text{d}}(x)$$
  Measures whether the patient is currently operating above or below their individual 30-day baseline.
* **14-Day Trajectory Slope ($\beta_{14\text{d}}$):**
  Calculated using Ordinary Least Squares (OLS) linear regression on timestamped measurements over the preceding 14 days:
  $$\beta = \frac{\sum_{k=1}^N (t_k - \bar{t})(x_k - \bar{x})}{\sum_{k=1}^N (t_k - \bar{t})^2}$$
* **Metric Instability / Coefficient of Variation ($CV_{14\text{d}}$):**
  $$CV = \frac{\sigma_{14\text{d}}}{\mu_{14\text{d}}}$$
  Quantifies physiological volatility (e.g., erratic glucose swings vs. steady readings).

---

## 3. Baseline & Trend Detection Algorithms

### 3.1 Personal Baseline Formulation
Rather than relying solely on arbitrary population percentiles, CarePath establishes an individualized baseline corridor for each patient:

1. **Exponentially Weighted Moving Average (EWMA):**
   $$\text{EWMA}_t = \alpha \cdot x_t + (1 - \alpha) \cdot \text{EWMA}_{t-1}$$
   Where $\alpha = 0.20$, striking an optimal balance between responsiveness to sustained lifestyle changes and resilience against single-measurement noise.
2. **Robust Statistical Corridor:**
   * $\text{Lower Bound} = \max(Q_{25} - 1.5 \cdot IQR, \; \mu - 2\sigma)$
   * $\text{Upper Bound} = \min(Q_{75} + 1.5 \cdot IQR, \; \mu + 2\sigma)$
   Any observation falling outside this corridor is flagged for anomaly triage.

### 3.2 Trend & Anomaly Classification Logic
```mermaid
graph TD
    A[New Vital Entry Ingested] --> B{Exceeds Emergency Safety Bounds?}
    B -->|Yes| C[Trigger Immediate CRITICAL Alert]
    B -->|No| D[Compute Z-Score vs. 30-Day Personal Baseline]
    D --> E{|z| > 2.5?}
    E -->|No| F[Metric within Personal Normal Corridor]
    E -->|Yes| G{Consecutive Outliers >= 3?}
    G -->|No| H[Flag as Transient Fluctuation Spike]
    G -->|Yes| I[Calculate 14-Day OLS Slope beta]
    I --> J{beta > threshold AND p < 0.05?}
    J -->|Yes| K[Trigger WARNING: Sustained Adverse Drift]
    J -->|No| L[Flag as Baseline Shift]
```

---

## 4. Model Architecture & Calibration

### 4.1 Algorithm Selection: HistGradientBoostingClassifier
* **Why Gradient Boosted Trees for CarePath?**
  1. Exceptional empirical performance on tabular biometric data with heterogeneous feature scales and non-linear interactions.
  2. Native support for missing values (crucial when patients have not yet completed a lipid panel or HbA1c test).
  3. Exact, polynomial-time TreeSHAP computation ($O(TLD^2)$ vs. exponential exponential-time sampling for neural networks).
* **Base Estimator:** `sklearn.ensemble.HistGradientBoostingClassifier(max_iter=150, max_leaf_nodes=31, learning_rate=0.05, min_samples_leaf=20)`.

### 4.2 Probability Calibration (`CalibratedClassifierCV`)
Raw tree ensemble outputs are uncalibrated margins or ranking scores. For medical decision support, raw scores cannot be interpreted as probabilities.
* We wrap the ensemble with `CalibratedClassifierCV(estimator=base_model, method='sigmoid', cv=5)`.
* This ensures that if the model outputs a risk score of $0.40$, approximately 40 out of 100 individuals in that cohort historically experienced cardiometabolic decompensation, minimizing the **Brier Score**:
  $$\text{Brier} = \frac{1}{N} \sum_{i=1}^N (R(x_i) - y_i)^2$$

### 4.3 Training & Evaluation Strategy
* **Clinical Dataset:** Synthetic cohort generated with realistic co-variance matrices modeled from the CDC NHANES (National Health and Nutrition Examination Survey) and Framingham Heart Study parameters.
* **Evaluation Metrics:**
  * **ROC-AUC:** Target $\ge 0.88$ (discrimination ability).
  * **PR-AUC:** Target $\ge 0.75$ (vital for imbalanced event rates).
  * **Brier Score:** Target $\le 0.12$ (calibration accuracy).
  * **Expected Calibration Error (ECE):** Target $< 5\%$.

---

## 5. Explainability Engine (TreeSHAP)

### 5.1 Mathematical Foundation
Shapley values represent the unique linear attribution method that satisfies four fundamental axioms: **Efficiency**, **Symmetry**, **Dummy (Null Player)**, and **Additivity**.

For any patient input $\mathbf{x}$, the prediction $f(\mathbf{x})$ is expressed as:
$$f(\mathbf{x}) = \phi_0 + \sum_{j=1}^{M} \phi_j(\mathbf{x})$$

Where:
* $\phi_0 = \mathbb{E}[f(X)]$ is the base value (expected risk across the representative reference cohort).
* $\phi_j(\mathbf{x})$ is the Shapley value of feature $j$.
* If $\phi_j > 0$, feature $j$ increases risk above baseline (**Risk Driver**).
* If $\phi_j < 0$, feature $j$ decreases risk below baseline (**Protective Factor**).

```
Predicted Risk: 0.48
Base Value:    0.31

Waterfall Decomposition:
0.31 (Base)
  + 0.085 [Fasting Glucose EWMA Δ]   ───► 0.395
  + 0.062 [Systolic BP 14d Slope]    ───► 0.457
  + 0.041 [BMI: 26.6]               ───► 0.498
  + 0.032 [Age: 48]                 ───► 0.530
  - 0.050 [HDL Cholesterol: 58]    ───► 0.480 (Final Risk Score)
```

### 5.2 Patient-Friendly Natural Language Generation (NLG)
To make mathematical SHAP attributions accessible to lay users:
1. Sort features by $|\phi_j|$.
2. Filter for top 3 positive contributors ($\phi_j > 0$) and top 2 protective contributors ($\phi_j < 0$).
3. Map feature identifiers to human-centric clinical templates:
   * `fasting_glucose_ewma_delta` $\implies$ *"Recent morning blood sugar readings are averaging {delta} mg/dL higher than your 30-day baseline, contributing +{shap_pct}% to your risk score."*
   * `systolic_bp_slope_14d` $\implies$ *"Blood pressure has shown a gradual upward trend over the past two weeks (+{slope} mmHg/day), adding +{shap_pct}% to your risk score."*
   * `cholesterol_hdl` $\implies$ *"Your healthy HDL ('good') cholesterol level is protecting your cardiometabolic balance, reducing your risk score by {shap_pct}%."*

---

## 6. Actionable Counterfactual Optimization Solver

### 6.1 Mathematical Formulation
When a patient asks *"How can I lower my risk category from Moderate to Low?"*, the system solves a constrained optimization problem:

$$\mathbf{x}^* = \arg\min_{\mathbf{x}'} \sum_{j=1}^M w_j \left| \frac{x'_j - x_j}{\sigma_j} \right|$$

Subject to:
1. **Target Risk Bound:** $R(\mathbf{x}') \le R_{\text{target}}$ (e.g., $R_{\text{target}} = 0.24$).
2. **Immutability Constraints:** $x'_j = x_j \quad \forall j \in \{\text{Age}, \text{Biological Sex}, \text{Family History}\}$.
3. **Clinical Plausibility & Safety Clamps:**
   * Systolic BP: $\max(95, x_{\text{systolic}} - 25) \le x'_{\text{systolic}} \le x_{\text{systolic}}$ (cannot simulate hypotension or unachievable drops).
   * Fasting Glucose: $\max(75, x_{\text{glucose}} - 30) \le x'_{\text{glucose}} \le x_{\text{glucose}}$.
   * Sleep: $x_{\text{sleep}} \le x'_{\text{sleep}} \le 9.0$ hours.
4. **Feasibility Weights ($w_j$):** Penalizes harder biometric changes (e.g., rapid BMI drops) while favoring accessible lifestyle shifts (e.g., regularizing sleep or moderate blood pressure reduction).

### 6.2 Fast Heuristic Solver
To respond in $< 200$ ms on interactive web sliders:
1. The engine computes the numerical sensitivity gradient $\nabla_{\mathbf{x}} R(\mathbf{x}) \approx \frac{\partial R}{\partial x_j}$.
2. Features are ranked by marginal risk reduction per unit of normalized clinical effort:
   $$\text{Efficiency}_j = \frac{|\phi_j|}{w_j \cdot \sigma_j}$$
3. A greedy line search evaluates achievable steps along the highest efficiency modifiable features until $R(\mathbf{x}') \le R_{\text{target}}$.

---

## 7. Model Validation, Testing & Drift Monitoring

### 7.1 Automated Pytest Verification Suite
1. **SHAP Local Additivity Assertion:**
   For any random patient vector $\mathbf{x}$:
   $$\left| f(\mathbf{x}) - \left( \phi_0 + \sum_{j=1}^M \phi_j(\mathbf{x}) \right) \right| < 10^{-4}$$
2. **Monotonicity Tests:**
   Verifies that increasing systolic blood pressure or fasting glucose (with all other parameters held constant) never decreases predicted risk score:
   $$\frac{\partial R}{\partial x_{\text{systolic}}} \ge 0, \quad \frac{\partial R}{\partial x_{\text{glucose}}} \ge 0$$
3. **Range Invariance:**
   Confirms all predictions strictly obey $0.00 \le R(\mathbf{x}) \le 1.00$.

### 7.2 Data Drift & Model Monitoring
* **Population Stability Index (PSI):**
  Monitors distribution shifts in patient input metrics between baseline training sets and live 30-day production windows:
  $$\text{PSI} = \sum_{b=1}^B (P_b - Q_b) \ln\left(\frac{P_b}{Q_b}\right)$$
  * $\text{PSI} < 0.1$: No significant shift.
  * $0.1 \le \text{PSI} < 0.25$: Moderate drift; alerts administrator to review feature distributions.
  * $\text{PSI} \ge 0.25$: Significant data drift; triggers automated retraining pipeline alert.
