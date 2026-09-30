import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mlService } from '../services/mlService';
import { ExplainRiskRequest, ExplainRiskResponse, PredictRiskResponse, MlServiceHealth } from '../types/explainability';

describe('mlService', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  const mockRequest: ExplainRiskRequest = {
    patient_uuid: '00000000-0000-0000-0000-000000000001',
    evaluation_mode: 'LONGITUDINAL',
    features: {
      age: 56,
      biological_sex: 'MALE',
      bmi: 29.4,
      smoking_status: 'CURRENT',
      systolic_bp_current: 142,
      diastolic_bp_current: 88,
      heart_rate_current: 76,
      fasting_glucose_current: 115,
      cholesterol_total: 220,
      cholesterol_hdl: 42,
      cholesterol_ldl: 145,
      triglycerides: 180,
      sleep_hours_current: 6.2,
      systolic_bp_ewma_delta: 3.2,
      fasting_glucose_ewma_delta: 4.1,
      systolic_bp_slope_14d: 0.15,
      fasting_glucose_slope_14d: 0.2,
    },
    top_k: 5,
    generate_counterfactuals: true,
  };

  it('should call /ml/v1/risk/explain and return parsed ExplainRiskResponse', async () => {
    const mockResponse: ExplainRiskResponse = {
      prediction: {
        model_version: 'calibrated_v1.0.0',
        overall_risk_score: 0.68,
        risk_category: 'ELEVATED',
        risk_tier: 'ELEVATED',
        prediction: 1,
        probability: 0.68,
        calibrated: true,
        confidence_interval: [0.62, 0.74],
        regulatory_disclaimer: 'Clinical decision-support only.',
        metadata: { brier_score: 0.089 },
      },
      explanation: {
        base_value: 0.25,
        predicted_value: 0.68,
        shap_values: { systolic_bp_current: 0.18, sleep_hours_current: -0.05 },
        top_risk_drivers: [
          {
            feature: 'systolic_bp_current',
            display_name: 'Systolic Blood Pressure',
            value: 142,
            shap_value: 0.18,
            direction: 'increases_risk',
            impact: 'high',
            narrative: 'High systolic BP increases risk by +0.1800',
          },
        ],
        protective_factors: [
          {
            feature: 'sleep_hours_current',
            display_name: 'Daily Sleep Duration',
            value: 6.2,
            shap_value: -0.05,
            direction: 'decreases_risk',
            impact: 'low',
            narrative: 'Adequate sleep buffers risk by -0.0500',
          },
        ],
        summary_narrative: 'Risk is elevated primarily due to systolic BP.',
        method: 'TreeSHAP',
      },
      counterfactuals: {
        counterfactual_available: true,
        status: 'SUCCESS',
        message: 'Found 1 counterfactual scenario.',
        plans: [
          {
            plan_id: 'cf_1',
            title: 'Blood Pressure Target',
            simulated_risk_score: 0.38,
            simulated_risk_category: 'MODERATE',
            simulated_tier: 'MODERATE',
            risk_reduction: 0.3,
            tier_transition_achieved: true,
            changed_features: [
              {
                feature: 'systolic_bp_current',
                display_name: 'Systolic Blood Pressure',
                current_value: 142,
                target_value: 120,
                unit: 'mmHg',
                change_delta: -22,
                allowed_range: [90, 180],
                direction: 'decrease',
                modifiable: true,
              },
            ],
            rationale: 'Systolic blood pressure within normal range',
          },
        ],
      },
      regulatory_disclaimer: 'Clinical decision-support only.',
    };

    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => mockResponse,
    } as Response);

    const result = await mlService.explainRisk(mockRequest);

    expect(fetchSpy).toHaveBeenCalledTimes(1);
    const [url, options] = fetchSpy.mock.calls[0];
    expect(url).toContain('/ml/v1/risk/explain');
    expect(options?.method).toBe('POST');
    expect(JSON.parse(options?.body as string)).toEqual(mockRequest);
    expect(result.prediction.overall_risk_score).toBe(0.68);
    expect(result.explanation.method).toBe('TreeSHAP');
    expect(result.counterfactuals.plans.length).toBe(1);
  });

  it('should call /ml/v1/risk/predict and return PredictRiskResponse', async () => {
    const mockPrediction: PredictRiskResponse = {
      model_version: 'calibrated_v1.0.0',
      overall_risk_score: 0.15,
      risk_category: 'LOW',
      risk_tier: 'LOW',
      prediction: 0,
      probability: 0.15,
      calibrated: true,
      confidence_interval: [0.11, 0.19],
      regulatory_disclaimer: 'Clinical decision-support only.',
    };

    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => mockPrediction,
    } as Response);

    const result = await mlService.predictRisk({ features: mockRequest.features });
    expect(result.risk_category).toBe('LOW');
    expect(result.overall_risk_score).toBe(0.15);
  });

  it('should call /ml/v1/health and return health status', async () => {
    const mockHealth: MlServiceHealth = {
      status: 'HEALTHY',
      service_name: 'carepath-ml-microservice',
      service_version: '1.0.0',
      modelVersion: 'calibrated_v1.0.0',
      model_loaded: true,
      calibrated: true,
      featuresSupported: 18,
    };

    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => mockHealth,
    } as Response);

    const health = await mlService.getHealth();
    expect(health.status).toBe('HEALTHY');
    expect(health.model_loaded).toBe(true);
  });

  it('should throw structured error on 422 Unprocessable Entity', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: false,
      status: 422,
      json: async () => ({
        detail: [
          { loc: ['body', 'features', 'systolic_bp_current'], msg: 'ensure this value is greater than 50' },
        ],
      }),
    } as Response);

    await expect(mlService.explainRisk(mockRequest)).rejects.toThrow(
      'systolic_bp_current: ensure this value is greater than 50'
    );
  });

  it('should throw friendly message when 503 Service Unavailable', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: false,
      status: 503,
      json: async () => ({
        detail: 'Model artifacts currently loading',
      }),
    } as Response);

    await expect(mlService.explainRisk(mockRequest)).rejects.toThrow(
      'Model service is initializing or model is not loaded yet.'
    );
  });
});
