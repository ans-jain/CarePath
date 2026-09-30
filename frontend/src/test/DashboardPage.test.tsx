import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import React from 'react';
import { DashboardPage } from '../pages/DashboardPage';
import { reportShareService } from '../services/reportShareService';
import { SharedReportDetail } from '../types/reportShare';

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    user: {
      id: 'doctor-uuid-robert-chen',
      firstName: 'Robert',
      lastName: 'Chen',
      email: 'dr.chen@carepath.io',
      role: 'ROLE_CLINICIAN',
    },
    isAuthenticated: true,
  }),
}));

describe('DashboardPage (Doctor Dashboard - Shared Reports Only)', () => {
  const sampleReports: SharedReportDetail[] = [
    {
      shareId: 'share-101',
      reportId: 'report-201',
      patientId: 'patient-301',
      patientName: 'Sarah Jenkins',
      reportDate: '2026-09-27T10:00:00Z',
      sharedDate: '2026-09-28T09:00:00Z',
      status: 'NEW',
      overallRiskScore: 0.72,
      riskCategory: 'ELEVATED',
      confidenceLevel: 'LONGITUDINAL_ROBUST',
      modelVersion: 'calibrated_v1.0.0',
      featureSnapshot: JSON.stringify({
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
        prediction: {
          overall_risk_score: 0.72,
          risk_category: 'ELEVATED',
          prediction: 1,
          probability: 0.72,
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
              narrative: 'Elevated systolic blood pressure is the primary driver.',
            },
          ],
          protective_factors: [],
          summary_narrative: 'Elevated cardiometabolic risk signal.',
        },
        counterfactuals: {
          counterfactual_available: false,
          status: 'SUCCESS',
          message: 'None',
          plans: [],
        },
      }),
    },
  ];

  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('verifies the 5 prohibited features are removed from Doctor Dashboard', async () => {
    vi.spyOn(reportShareService, 'getDoctorSharedReports').mockResolvedValue(sampleReports);

    render(<DashboardPage />);

    await waitFor(() => {
      expect(screen.getByText(/Doctor Clinical Portal/i)).toBeInTheDocument();
    });

    // Prohibited features must NOT be rendered
    expect(screen.queryByText(/Monitored Dimensions/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Explainability Engine/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Calibration Guarantee/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Microservice Health/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Explainable Decision-Support Workflow/i)).not.toBeInTheDocument();
  });

  it('renders Shared Reports list with patient name, dates, status, and View Report button', async () => {
    vi.spyOn(reportShareService, 'getDoctorSharedReports').mockResolvedValue(sampleReports);

    render(<DashboardPage />);

    await waitFor(() => {
      expect(screen.getByText(/Shared Patient Reports/i)).toBeInTheDocument();
    });

    expect(screen.getByText('Sarah Jenkins')).toBeInTheDocument();
    expect(screen.getByText(/Report Date:/i)).toBeInTheDocument();
    expect(screen.getByText(/Shared Date:/i)).toBeInTheDocument();
    expect(screen.getByText('New')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /View Report/i })).toBeInTheDocument();
  });

  it('clicking View Report displays the complete shared report with back button', async () => {
    vi.spyOn(reportShareService, 'getDoctorSharedReports').mockResolvedValue(sampleReports);
    const viewDetailSpy = vi.spyOn(reportShareService, 'getDoctorSharedReportDetail').mockResolvedValue(sampleReports[0]);

    render(<DashboardPage />);

    await waitFor(() => {
      expect(screen.getByText('Sarah Jenkins')).toBeInTheDocument();
    });

    const viewBtn = screen.getByRole('button', { name: /View Report/i });
    fireEvent.click(viewBtn);

    await waitFor(() => {
      expect(viewDetailSpy).toHaveBeenCalledWith('report-201');
      expect(screen.getByText(/Back to Shared Reports/i)).toBeInTheDocument();
      expect(screen.getByText(/Patient Biomarker Measurements/i)).toBeInTheDocument();
      expect(screen.getByText('142/88 mmHg')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Download Assessment \(PDF\)/i })).toBeInTheDocument();
    });

    // Click back button to return to shared reports list
    fireEvent.click(screen.getByRole('button', { name: /Back to Shared Reports/i }));
    await waitFor(() => {
      expect(screen.getByText(/Shared Patient Reports/i)).toBeInTheDocument();
    });
  });
});
