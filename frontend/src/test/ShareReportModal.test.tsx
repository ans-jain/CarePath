import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import React from 'react';
import { ShareReportModal } from '../components/assessment/ShareReportModal';
import { SavedAssessment } from '../types/explainability';
import { reportShareService } from '../services/reportShareService';

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    user: {
      id: 'patient-id-123',
      firstName: 'Sarah',
      lastName: 'Jenkins',
      email: 'patient.sarah@carepath.io',
      role: 'ROLE_PATIENT',
    },
    isAuthenticated: true,
  }),
}));

describe('ShareReportModal', () => {
  const sampleReport: SavedAssessment = {
    id: 'report-abc-123',
    patientId: 'patient-id-123',
    patientName: 'Sarah Jenkins',
    timestamp: '2026-09-28T10:00:00Z',
    overallRiskScore: 0.72,
    riskCategory: 'ELEVATED',
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
        overall_risk_score: 0.72,
        risk_category: 'ELEVATED',
        prediction: 1,
        probability: 0.72,
        calibrated: true,
        model_version: 'calibrated_v1.0.0',
        regulatory_disclaimer: 'Decision support only.',
      },
      explanation: {
        base_value: 0.18,
        shap_values: {},
        top_risk_drivers: [],
        protective_factors: [],
        summary_narrative: 'Elevated risk.',
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

  it('renders modal with scoped access warning and verified doctors list', async () => {
    render(
      <ShareReportModal
        isOpen={true}
        onClose={vi.fn()}
        report={sampleReport}
      />
    );

    expect(screen.getByText('Share Assessment Report')).toBeInTheDocument();
    expect(screen.getByText(/Strictly Scoped Access/i)).toBeInTheDocument();
    expect(screen.getByText(/Only this specific report is shared with your chosen physician/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Dr. Robert Chen')).toBeInTheDocument();
    });
  });

  it('allows searching doctors and confirming share', async () => {
    const handleUpdated = vi.fn();
    const shareSpy = vi.spyOn(reportShareService, 'shareReport');

    render(
      <ShareReportModal
        isOpen={true}
        onClose={vi.fn()}
        report={sampleReport}
        onShareUpdated={handleUpdated}
      />
    );

    await waitFor(() => {
      expect(screen.getByText('Dr. Robert Chen')).toBeInTheDocument();
    });

    // Filter doctors
    const searchInput = screen.getByPlaceholderText(/Search verified doctors/i);
    fireEvent.change(searchInput, { target: { value: 'Robert' } });

    expect(screen.getByText('Dr. Robert Chen')).toBeInTheDocument();
    expect(screen.queryByText('Dr. Priya Patel')).not.toBeInTheDocument();

    // Select doctor
    const selectBtn = screen.getByRole('button', { name: /Select Doctor/i });
    fireEvent.click(selectBtn);

    // Confirmation drawer opens
    expect(screen.getByText('Confirm Report Sharing')).toBeInTheDocument();
    const confirmBtn = screen.getByRole('button', { name: /Confirm & Share Report/i });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(shareSpy).toHaveBeenCalled();
    });
  });
});
