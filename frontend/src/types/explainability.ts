export type RiskTier = 'LOW' | 'MODERATE' | 'ELEVATED' | 'HIGH';

export type ShapDirection = 'increases_risk' | 'decreases_risk' | 'neutral';

export type ShapImpact = 'low' | 'medium' | 'high';

export interface FeatureAttribution {
  feature: string;
  display_name: string;
  value: number | string | null;
  shap_value: number;
  direction: ShapDirection;
  impact: ShapImpact;
  narrative: string;
}

export interface ShapExplanation {
  base_value: number;
  predicted_value?: number;
  shap_values: Record<string, number>;
  top_risk_drivers: FeatureAttribution[];
  protective_factors: FeatureAttribution[];
  summary_narrative: string;
  method?: string;
}

export interface CounterfactualFeatureTarget {
  feature: string;
  display_name: string;
  current_value: number | string;
  target_value: number | string;
  unit?: string | null;
  change_delta?: number | null;
  allowed_range: [number | string, number | string];
  direction: string;
  modifiable: boolean;
}

export interface CounterfactualPlan {
  plan_id: string;
  title: string;
  simulated_risk_score: number;
  simulated_risk_category: RiskTier;
  simulated_tier?: RiskTier;
  risk_reduction: number;
  tier_transition_achieved: boolean;
  changed_features: CounterfactualFeatureTarget[];
  rationale: string;
}

export interface CounterfactualResult {
  counterfactual_available: boolean;
  status: 'SUCCESS' | 'NO_VALID_COUNTERFACTUAL_FOUND';
  message: string;
  plans: CounterfactualPlan[];
}

export interface PredictRiskResponse {
  model_version: string;
  overall_risk_score: number;
  risk_category: RiskTier;
  risk_tier?: RiskTier;
  prediction: number;
  probability: number;
  calibrated: boolean;
  confidence_level?: string | null;
  confidence_interval?: [number, number];
  regulatory_disclaimer: string;
  metadata?: Record<string, any>;
}

export interface ExplainRiskResponse {
  prediction: PredictRiskResponse;
  explanation: ShapExplanation;
  counterfactuals: CounterfactualResult;
  regulatory_disclaimer: string;
}

export interface FeatureVectorInput {
  age: number;
  biological_sex: 'FEMALE' | 'MALE' | string;
  bmi: number;
  smoking_status: 'NEVER' | 'FORMER' | 'CURRENT' | string;
  systolic_bp_current: number;
  diastolic_bp_current: number;
  heart_rate_current: number;
  fasting_glucose_current: number;
  hba1c_current?: number | null;
  cholesterol_total?: number | null;
  cholesterol_hdl?: number | null;
  cholesterol_ldl?: number | null;
  triglycerides?: number | null;
  sleep_hours_current?: number | null;
  systolic_bp_ewma_delta?: number | null;
  fasting_glucose_ewma_delta?: number | null;
  systolic_bp_slope_14d?: number | null;
  fasting_glucose_slope_14d?: number | null;
}

export interface ExplainRiskRequest {
  patient_uuid?: string;
  evaluation_mode?: 'INTAKE' | 'PRELIMINARY_INTAKE' | 'LONGITUDINAL' | 'LONGITUDINAL_ROBUST';
  features: FeatureVectorInput;
  top_k?: number;
  generate_counterfactuals?: boolean;
}

export interface MlServiceHealth {
  status: 'HEALTHY' | 'DEGRADED' | 'UNHEALTHY';
  service_name: string;
  service_version: string;
  modelVersion: string;
  model_loaded: boolean;
  calibrated: boolean;
  shapExplainerType?: string;
  featuresSupported: number;
}

export interface SavedAssessment {
  id: string;
  patientId?: string;
  patientName?: string;
  timestamp: string; // ISO 8601 string
  overallRiskScore: number;
  riskCategory: RiskTier;
  confidenceLevel?: string | null;
  modelVersion: string;
  request: ExplainRiskRequest;
  response: ExplainRiskResponse;
}
