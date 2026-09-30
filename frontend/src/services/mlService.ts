import { requestJson, ML_BASE_URL } from './api';
import {
  ExplainRiskRequest,
  ExplainRiskResponse,
  PredictRiskResponse,
  MlServiceHealth
} from '../types/explainability';

export const mlService = {
  /**
   * Invokes the unified explainability API endpoint /ml/v1/risk/explain
   * Returns calibrated prediction, SHAP waterfall attributions, and constrained counterfactuals.
   */
  async explainRisk(request: ExplainRiskRequest): Promise<ExplainRiskResponse> {
    return requestJson<ExplainRiskResponse>(`${ML_BASE_URL}/ml/v1/risk/explain`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  },

  /**
   * Invokes the core prediction endpoint /ml/v1/risk/predict
   */
  async predictRisk(request: ExplainRiskRequest): Promise<PredictRiskResponse> {
    return requestJson<PredictRiskResponse>(`${ML_BASE_URL}/ml/v1/risk/predict`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  },

  /**
   * Checks ML microservice health, model status, and explainer capabilities.
   */
  async getHealth(): Promise<MlServiceHealth> {
    return requestJson<MlServiceHealth>(`${ML_BASE_URL}/ml/v1/health`, {
      method: 'GET',
    });
  },
};
