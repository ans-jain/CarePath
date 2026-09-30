import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import React from 'react';
import { AssessmentHistorySection } from '../components/assessment/AssessmentHistorySection';
import { SavedAssessment } from '../types/explainability';
import { pdfReportService } from '../services/pdfReportService';

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    user: {
      id: 'patient-id-1',
      firstName: 'Sarah',
      lastName: 'Jenkins',
      email: 'patient.sarah@carepath.io',
      role: 'PATIENT',
    },
    isAuthenticated: true,
  }),
}));

describe('AssessmentHistorySection', () => {
  const sampleItem: SavedAssessment = {
    id: 'history-item-1',
    patientId: 'patient-id-1',
    patientName: 'Sarah Jenkins',
    timestamp: '2026-09-28T14:00:00Z',
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
        top_risk_drivers: [
          {
            feature: 'systolic_bp_current',
            display_name: 'Systolic Blood Pressure',
            value: 142,
            shap_value: 0.15,
            direction: 'increases_risk',
            impact: 'high',
            narrative: 'Elevated systolic BP is the primary driver.',
          },
        ],
        protective_factors: [],
        summary_narrative: 'Moderate cardiometabolic risk.',
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

  it('renders empty state when history is empty', () => {
    const handleStartNew = vi.fn();
    render(
      <AssessmentHistorySection
        history={[]}
        onSelectAssessment={vi.fn()}
        onStartNewAssessment={handleStartNew}
      />
    );

    expect(screen.getByText('No Assessment History Yet')).toBeInTheDocument();
    const startBtn = screen.getByRole('button', { name: /Run Your First Assessment/i });
    expect(startBtn).toBeInTheDocument();
    fireEvent.click(startBtn);
    expect(handleStartNew).toHaveBeenCalled();
  });

  it('renders history cards with tier badge, biomarkers and download button', () => {
    const handleSelect = vi.fn();
    const downloadSpy = vi.spyOn(pdfReportService, 'downloadAssessmentPdf').mockImplementation(() => {});

    render(
      <AssessmentHistorySection
        history={[sampleItem]}
        onSelectAssessment={handleSelect}
        onStartNewAssessment={vi.fn()}
      />
    );

    expect(screen.getByText(/Patient Assessment History/i)).toBeInTheDocument();
    expect(screen.getByText(/MODERATE Risk \(42.0%\)/i)).toBeInTheDocument();
    expect(screen.getByText('142/88 mmHg')).toBeInTheDocument();
    expect(screen.getByText('108 mg/dL')).toBeInTheDocument();

    // Verify Share Report button
    expect(screen.getByRole('button', { name: /Share Report/i })).toBeInTheDocument();

    // Click Download PDF
    const downloadBtn = screen.getByRole('button', { name: /Download PDF/i });
    expect(downloadBtn).toBeInTheDocument();
    fireEvent.click(downloadBtn);
    expect(downloadSpy).toHaveBeenCalled();

    // Click card to view details
    fireEvent.click(screen.getByText(/Inspect Full Attributions/i));
    expect(handleSelect).toHaveBeenCalledWith(sampleItem);
  });
});
