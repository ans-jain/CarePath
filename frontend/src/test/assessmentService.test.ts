import { describe, it, expect, vi, beforeEach } from 'vitest';
import { assessmentService } from '../services/assessmentService';
import { SavedAssessment } from '../types/explainability';
import * as apiModule from '../services/api';

describe('assessmentService', () => {
  const sampleAssessment: SavedAssessment = {
    id: 'assessment-test-1',
    patientId: 'patient-sarah-123',
    patientName: 'Sarah Jenkins',
    timestamp: '2026-09-28T12:00:00.000Z',
    overallRiskScore: 0.42,
    riskCategory: 'MODERATE',
    confidenceLevel: 'LONGITUDINAL_ROBUST',
    modelVersion: 'calibrated_v1.0.0',
    request: {
      features: {
        age: 48,
        biological_sex: 'FEMALE',
        bmi: 27.4,
        smoking_status: 'FORMER',
        systolic_bp_current: 142,
        diastolic_bp_current: 88,
        heart_rate_current: 74,
        fasting_glucose_current: 108,
      },
    },
    response: {
      prediction: {
        overall_risk_score: 0.42,
        risk_category: 'MODERATE',
        prediction: 0,
        probability: 0.42,
        calibrated: true,
        model_version: 'calibrated_v1.0.0',
        regulatory_disclaimer: 'Decision-support risk signal only.',
      },
      explanation: {
        base_value: 0.18,
        shap_values: {},
        top_risk_drivers: [],
        protective_factors: [],
        summary_narrative: 'Moderate risk signal.',
      },
      counterfactuals: {
        counterfactual_available: false,
        status: 'SUCCESS',
        message: 'None',
        plans: [],
      },
      regulatory_disclaimer: 'Decision support only.',
    },
  };

  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('saves assessment to localStorage and returns saved record', async () => {
    vi.spyOn(apiModule, 'requestJson').mockResolvedValue({ id: 'backend-rec-1' });

    const result = await assessmentService.saveAssessment(sampleAssessment);

    expect(result.id).toBe('assessment-test-1');
    const local = assessmentService.getLocalHistory('patient-sarah-123');
    expect(local).toHaveLength(1);
    expect(local[0].id).toBe('assessment-test-1');
    expect(local[0].overallRiskScore).toBe(0.42);
  });

  it('retrieves assessment history and merges with backend results', async () => {
    // Put one in local storage
    localStorage.setItem(
      'carepath_assessment_history_patient-sarah-123',
      JSON.stringify([sampleAssessment])
    );

    // Mock backend returning an older assessment
    vi.spyOn(apiModule, 'requestJson').mockResolvedValue({
      content: [
        {
          id: 'assessment-backend-2',
          patientId: 'patient-sarah-123',
          assessmentTimestamp: '2026-09-25T10:00:00Z',
          overallRiskScore: 0.55,
          riskCategory: 'ELEVATED',
          featureSnapshot: JSON.stringify({
            request: sampleAssessment.request,
            response: sampleAssessment.response,
            patientName: 'Sarah Jenkins',
          }),
        },
      ],
    });

    const history = await assessmentService.getAssessmentHistory('patient-sarah-123');
    expect(history.length).toBeGreaterThanOrEqual(2);
    expect(history[0].id).toBe('assessment-test-1'); // newest first
  });

  it('clears local history cleanly', () => {
    localStorage.setItem(
      'carepath_assessment_history_patient-sarah-123',
      JSON.stringify([sampleAssessment])
    );
    expect(assessmentService.getLocalHistory('patient-sarah-123')).toHaveLength(1);

    assessmentService.clearLocalHistory('patient-sarah-123');
    expect(assessmentService.getLocalHistory('patient-sarah-123')).toHaveLength(0);
  });
});
