import { describe, it, expect, vi } from 'vitest';
import { pdfReportService } from '../services/pdfReportService';
import { ExplainRiskRequest, ExplainRiskResponse } from '../types/explainability';

describe('pdfReportService', () => {
  const sampleRequest: ExplainRiskRequest = {
    features: {
      age: 48,
      biological_sex: 'FEMALE',
      bmi: 27.4,
      smoking_status: 'FORMER',
      systolic_bp_current: 142,
      diastolic_bp_current: 88,
      heart_rate_current: 74,
      fasting_glucose_current: 108,
      hba1c_current: 5.8,
      cholesterol_total: 195,
      cholesterol_hdl: 52,
      cholesterol_ldl: 118,
      triglycerides: 140,
    },
  };

  const sampleResponse: ExplainRiskResponse = {
    prediction: {
      overall_risk_score: 0.42,
      risk_category: 'MODERATE',
      confidence_level: 'LONGITUDINAL_ROBUST',
      prediction: 0,
      probability: 0.42,
      calibrated: true,
      model_version: 'calibrated_v1.0.0',
      regulatory_disclaimer: 'Decision-support risk signal only.',
    },
    explanation: {
      base_value: 0.18,
      shap_values: { systolic_bp_current: 0.15 },
      top_risk_drivers: [
        {
          feature: 'systolic_bp_current',
          display_name: 'Systolic Blood Pressure',
          value: 142,
          shap_value: 0.15,
          direction: 'increases_risk',
          impact: 'high',
          narrative: 'Elevated systolic BP is the primary driver pushing risk upward.',
        },
      ],
      protective_factors: [
        {
          feature: 'daily_steps',
          display_name: 'Daily Step Count',
          value: 8500,
          shap_value: -0.06,
          direction: 'decreases_risk',
          impact: 'medium',
          narrative: 'Consistent daily physical activity buffers against metabolic risk.',
        },
      ],
      summary_narrative: 'Moderate cardiometabolic risk signal driven by systolic BP.',
    },
    counterfactuals: {
      counterfactual_available: true,
      status: 'SUCCESS',
      message: 'Found counterfactual scenario.',
      plans: [
        {
          plan_id: 'plan-1',
          title: 'BP Normalization Plan',
          simulated_risk_score: 0.22,
          simulated_risk_category: 'LOW',
          risk_reduction: 0.2,
          tier_transition_achieved: true,
          changed_features: [
            {
              feature: 'systolic_bp_current',
              display_name: 'Systolic Blood Pressure',
              current_value: 142,
              target_value: 120,
              unit: 'mmHg',
              allowed_range: [90, 140],
              direction: 'decrease',
              modifiable: true,
            },
          ],
          rationale: 'Lowering blood pressure drops risk category to Low.',
        },
      ],
    },
    regulatory_disclaimer: 'Decision-support risk signal only. Does not diagnose diseases.',
  };

  it('generates a jsPDF instance with valid structure and pages', () => {
    const doc = pdfReportService.generateAssessmentPdf({
      patientName: 'Sarah Jenkins',
      patientEmail: 'patient.sarah@carepath.io',
      patientId: 'patient-uuid-1234',
      request: sampleRequest,
      response: sampleResponse,
      timestamp: '2026-09-28T10:00:00Z',
    });

    expect(doc).toBeDefined();
    expect(Math.round(doc.internal.pageSize.getWidth())).toBe(210);
    expect(doc.internal.pages.length).toBeGreaterThanOrEqual(1);
  });

  it('triggers download with formatted filename containing patient name', () => {
    const saveSpy = vi.fn();
    vi.spyOn(pdfReportService, 'generateAssessmentPdf').mockReturnValue({
      save: saveSpy,
    } as any);

    pdfReportService.downloadAssessmentPdf({
      patientName: 'Sarah Jenkins',
      request: sampleRequest,
      response: sampleResponse,
    });

    expect(saveSpy).toHaveBeenCalledTimes(1);
    expect(saveSpy.mock.calls[0][0]).toMatch(/CarePath-Assessment-Report-Sarah_Jenkins-\d{4}-\d{2}-\d{2}\.pdf/);
  });
});
