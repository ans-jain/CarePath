# CarePath — Frontend Integration & Explainability UI (Phase 8)

CarePath is an enterprise-grade, longitudinal health-monitoring and explainable cardiometabolic risk analysis platform.
This frontend application delivers a clinical decision-support interface that connects directly to the **CarePath ML Microservice** (FastAPI, TreeSHAP, Constrained Counterfactuals) and the **Spring Boot Core Backend** (JWT Security, Patient Profiles, Vitals Ingestion).

---

## 1. Clinical Mission & Guardrails

> [!IMPORTANT]
> **Strict Non-Diagnostic Clinical Guardrail:**
> CarePath is designed strictly for **clinical decision support**.
> - It does **NOT** diagnose medical conditions, diseases, or syndromes.
> - It does **NOT** prescribe pharmaceutical medications or treatment regimens.
> - It does **NOT** replace licensed physicians or clinical judgment.
> - All outputs are framed in **non-causal language** (e.g., *"increases modeled risk"* rather than *"causes hypertension"*).
> - Counterfactual targets are framed as **computational optimization simulations**, not clinical prescriptions.

---

## 2. Tech Stack & Architecture

- **Framework**: React 18 (TypeScript)
- **Build Tool**: Vite 5
- **Styling**: Tailwind CSS 3 (custom healthcare/clinical palette, WCAG AA accessible contrast)
- **Icons**: Lucide React
- **Testing**: Vitest + `@testing-library/react` + `jsdom`
- **State & Auth**: React Context (`AuthContext`) with Spring Boot JWT token management and instant demo personas (`Sarah Jenkins` — Patient, `Dr. Marcus Vance` — Clinician)
- **API Client**: Strongly typed Fetch wrapper with automatic Bearer token injection, Pydantic 422 error normalization, and 503 degraded model detection

### Port & Service Topology

| Service | Port | Description |
| :--- | :--- | :--- |
| **CarePath Frontend** | `http://localhost:5173` | React 18 / Vite single-page application |
| **CarePath ML Service** | `http://localhost:8000` | FastAPI service (`/ml/v1/risk/explain`, `/ml/v1/health`) |
| **CarePath Core Backend**| `http://localhost:8080` | Spring Boot API (`/api/v1/auth`, `/api/v1/patients`) |

---

## 3. Component Hierarchy & Features

```
frontend/src/
├── App.tsx                     # Master layout, navigation tabs, modal state, clinical footer
├── main.tsx                    # React 18 createRoot mounting AuthProvider & App
├── index.css                   # Tailwind imports & clinical design system styling
├── types/
│   ├── explainability.ts       # TypeScript interfaces matching Phase 6 & 7 contracts
│   └── auth.ts                 # Auth and User DTOs matching Spring Boot backend
├── services/
│   ├── api.ts                  # Centralized HTTP client, error parsing, and JWT header injection
│   ├── mlService.ts            # Client for /ml/v1/risk/explain, /ml/v1/risk/predict, /ml/v1/health
│   └── authService.ts          # Client for Spring Boot JWT login, register, and demo persona storage
├── context/
│   └── AuthContext.tsx         # Session state, login/logout, and demo persona switching
├── components/
│   ├── common/
│   │   ├── Navbar.tsx          # Brand, navigation tabs, health poller, persona switcher, sign out
│   │   ├── AlertBanner.tsx     # Disclaimer banner and retryable error state banner
│   │   └── LoadingSpinner.tsx  # Accessible loading skeleton for assessment execution
│   ├── dashboard/
│   │   └── SystemHealthBadge.tsx # Real-time poller for /ml/v1/health endpoint
│   └── assessment/
│       ├── PatientForm.tsx     # 18-feature input form, physiological validation, and 3 clinical presets
│       ├── RiskSummaryCard.tsx # Calibrated tier badge, continuous risk gauge, and Brier guarantee
│       ├── ShapWaterfall.tsx   # Diverging horizontal bar chart, top risk drivers, protective buffers
│       ├── CounterfactualCards.tsx # Actionable target scenarios, tier transitions, modifiable deltas
│       └── HowItWorksModal.tsx # Educational modal explaining Platt scaling, TreeSHAP, and counterfactuals
└── pages/
    ├── RiskAssessmentPage.tsx  # End-to-end clinical assessment workflow
    ├── DashboardPage.tsx       # System overview, monitored feature metrics, active patient profile
    └── LoginPage.tsx           # Authentication view with email/password and one-click demo access
```

---

## 4. Explainability UI Walkthrough

1. **Patient Form & Physiological Clamps**:
   - Accepts 18 biometric indicators spanning demographics, resting vitals, serum labs, and 30-day longitudinal trends.
   - Includes 3 clinical presets:
     - **High Risk**: Hypertension (148/92 mmHg), impaired fasting glucose (138 mg/dL), dyslipidemia.
     - **Moderate Risk**: Prehypertension (132/84 mmHg), mild fasting glucose drift (112 mg/dL).
     - **Low Baseline**: Normotensive (116/74 mmHg), euglycemic (88 mg/dL), protective HDL buffer.
   - Configurable $k$ (Top 3, 4, 5, or 8 SHAP drivers) and optional counterfactual solver generation.

2. **Risk Stratification Card**:
   - Evaluates continuous posterior risk score ($0.0000$ to $1.0000$).
   - Color-coded decision tiers:
     - **Low Risk** (Emerald, score $< 0.25$)
     - **Moderate Risk** (Amber, $0.25 \le \text{score} < 0.50$)
     - **Elevated Risk** (Orange, $0.50 \le \text{score} < 0.75$)
     - **High Risk** (Rose, score $\ge 0.75$)
   - Displays calibration guarantee (Brier score $\le 0.12$ Platt sigmoid calibration).

3. **SHAP Feature Attributions (`ShapWaterfall`)**:
   - Visualizes exact additive contributions $\phi_i$ relative to population base reference.
   - **Primary Risk Drivers** ($\phi > 0$): Highlighted with upward trending markers in rose, detailing observed value and mathematical push on the prediction.
   - **Protective Buffering Factors** ($\phi < 0$): Highlighted with downward trending markers in emerald, detailing buffering effects against risk.
   - Non-causal decision summary narrative.

4. **Actionable Counterfactual Scenarios (`CounterfactualCards`)**:
   - Generates constrained, modifiable lifestyle/clinical targets (e.g. resting systolic BP, fasting glucose, sleep duration).
   - Immutable variables (such as age and biological sex) are never manipulated.
   - Shows simulated risk score, risk reduction percentage, and tier transition achievements (e.g., *Elevated $\to$ Moderate*).
   - Displays clear baseline vs target values and deltas with units (mmHg, mg/dL, hrs/night).
   - Graceful fallback when `NO_VALID_COUNTERFACTUAL_FOUND`.

---

## 5. Running the Application

### Development Server
```bash
cd frontend
npm run dev
```
Open `http://localhost:5173` in your browser.

### Running Automated Test Suite
```bash
cd frontend
npm test
```
Executes all 23 unit and integration tests across 6 test suites with Vitest.

### Production Build
```bash
cd frontend
npm run build
```
Executes strict TypeScript type checking (`tsc`) followed by Vite production bundling into `dist/`.
