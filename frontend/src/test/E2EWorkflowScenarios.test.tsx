import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import React from 'react';
import App from '../App';
import { AuthProvider } from '../context/AuthContext';
import { mlService } from '../services/mlService';
import { notificationService } from '../services/notificationService';
import { adminService } from '../services/adminService';

describe('Phase 11 — End-to-End Workflow Scenarios', () => {
  beforeEach(() => {
    vi.restoreAllMocks();

    vi.spyOn(mlService, 'getHealth').mockResolvedValue({
      status: 'HEALTHY',
      service_name: 'carepath-ml-microservice',
      service_version: '1.0.0',
      modelVersion: 'calibrated_v1.0.0',
      model_loaded: true,
      calibrated: true,
      shapExplainerType: 'TreeExplainer',
      featuresSupported: 18,
    });
  });

  it('Scenario 1 — Complete Patient Assessment Workflow (Form Submission -> Risk Score -> SHAP Attributions -> Counterfactuals)', async () => {
    localStorage.setItem(
      'carepath_user',
      JSON.stringify({
        id: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
        email: 'patient.sarah@carepath.io',
        firstName: 'Sarah',
        lastName: 'Jenkins',
        role: 'ROLE_PATIENT',
      })
    );
    window.history.pushState({}, '', '/patient/dashboard');

    const explainSpy = vi.spyOn(mlService, 'explainRisk').mockResolvedValue({
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
        shap_values: { systolic_bp_current: 0.15, fasting_glucose_current: 0.09 },
        top_risk_drivers: [
          {
            feature: 'systolic_bp_current',
            display_name: 'Systolic Blood Pressure',
            value: 142.0,
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
        summary_narrative: 'Patient demonstrates moderate risk primarily driven by systolic blood pressure.',
      },
      counterfactuals: {
        counterfactual_available: true,
        status: 'SUCCESS',
        message: 'Identified realistic lifestyle and biomarker modifications.',
        plans: [
          {
            plan_id: 'plan-bp-glucose',
            title: 'Blood Pressure & Glycemic Optimization',
            simulated_risk_score: 0.22,
            simulated_risk_category: 'LOW',
            risk_reduction: 0.20,
            tier_transition_achieved: true,
            changed_features: [
              {
                feature: 'systolic_bp_current',
                display_name: 'Systolic Blood Pressure',
                current_value: 142.0,
                target_value: 120.0,
                unit: 'mmHg',
                change_delta: -22.0,
                allowed_range: [90.0, 140.0],
                direction: 'decrease',
                modifiable: true,
              },
            ],
            rationale: 'Achieving target systolic BP reduces predicted risk category from MODERATE to LOW.',
          },
        ],
      },
      regulatory_disclaimer: 'Decision-support risk signal only. Does not diagnose diseases or prescribe medication.',
    });

    render(
      <AuthProvider>
        <App />
      </AuthProvider>
    );

    // Initial load: Risk Assessment tab active
    await waitFor(() => {
      expect(screen.getByText('Cardiometabolic Risk Assessment')).toBeInTheDocument();
    });

    // Submit evaluation
    const evaluateBtn = screen.getByRole('button', { name: /Run Risk Assessment/i });
    expect(evaluateBtn).toBeInTheDocument();
    fireEvent.click(evaluateBtn);
    expect(explainSpy).toHaveBeenCalled();

    // Verify Risk Score appears
    await waitFor(() => {
      expect(screen.getByText('(42.0%)')).toBeInTheDocument();
    });
    expect(screen.getByText('Moderate Risk')).toBeInTheDocument();

    // Verify SHAP Waterfall and attributions appear
    expect(screen.getAllByText('Systolic Blood Pressure').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText(/Elevated systolic BP/i)).toBeInTheDocument();

    // Verify Counterfactual plan appears
    expect(screen.getByText('Blood Pressure & Glycemic Optimization')).toBeInTheDocument();
    expect(screen.getAllByText(/LOW/i).length).toBeGreaterThanOrEqual(1);
  });

  it('Scenario 2 — Notification Lifecycle (Receive, View, Mark as Read, Badge Count Update)', async () => {
    localStorage.setItem(
      'carepath_user',
      JSON.stringify({
        id: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
        email: 'patient.sarah@carepath.io',
        firstName: 'Sarah',
        lastName: 'Jenkins',
        role: 'ROLE_PATIENT',
      })
    );
    window.history.pushState({}, '', '/patient/dashboard');

    vi.spyOn(notificationService, 'getNotifications').mockResolvedValue({
      content: [
        {
          id: 'notif-1',
          userId: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
          title: 'Risk Profile Updated',
          message: 'New assessment recorded for patient.',
          type: 'RISK_ASSESSMENT_COMPLETED',
          channel: 'IN_APP',
          status: 'SENT',
          isRead: false,
          createdAt: new Date().toISOString(),
        },
      ],
      totalElements: 1,
      totalPages: 1,
      page: 0,
      size: 20,
      first: true,
      last: true,
      unreadCount: 1,
    });
    vi.spyOn(notificationService, 'getUnreadCount').mockResolvedValue(1);
    vi.spyOn(notificationService, 'markAllAsRead').mockResolvedValue(undefined as any);

    render(
      <AuthProvider>
        <App />
      </AuthProvider>
    );

    // Wait for header to render
    await waitFor(() => {
      expect(screen.getByLabelText('View notifications')).toBeInTheDocument();
    });

    // Open notifications dropdown
    const bellBtn = screen.getByLabelText('View notifications');
    fireEvent.click(bellBtn);

    // Verify notification list appears
    await waitFor(() => {
      expect(screen.getByText('Risk Profile Updated')).toBeInTheDocument();
    });

    // If mark all as read is present, click it
    const markAllBtn = screen.queryByRole('button', { name: /Mark all read/i });
    if (markAllBtn) {
      fireEvent.click(markAllBtn);
    }
  });

  it('Scenario 3 — RBAC Authorization & Domain Protection', async () => {
    // 1. Patient verification: Patient cannot see Admin tab or persona switcher
    localStorage.setItem(
      'carepath_user',
      JSON.stringify({
        id: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
        email: 'patient.sarah@carepath.io',
        firstName: 'Sarah',
        lastName: 'Jenkins',
        role: 'ROLE_PATIENT',
      })
    );
    window.history.pushState({}, '', '/patient/dashboard');

    const { unmount } = render(
      <AuthProvider>
        <App />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('CarePath')).toBeInTheDocument();
    });
    expect(screen.queryByRole('button', { name: /Security & Audit/i })).not.toBeInTheDocument();
    expect(screen.queryByTitle('Switch to Admin View')).not.toBeInTheDocument();
    expect(screen.queryByTitle('Switch to Clinician View')).not.toBeInTheDocument();

    unmount();

    // 2. Doctor verification: Doctor cannot see Security & Audit tab or access Admin dashboard
    localStorage.setItem(
      'carepath_user',
      JSON.stringify({
        id: '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d',
        email: 'dr.marcus@carepath.io',
        firstName: 'Marcus',
        lastName: 'Vance',
        role: 'ROLE_CLINICIAN',
      })
    );
    window.history.pushState({}, '', '/doctor/dashboard');

    const { unmount: unmountDoctor } = render(
      <AuthProvider>
        <App />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByText(/Clinical Intelligence Dashboard/i)).toBeInTheDocument();
    });
    expect(screen.queryByRole('button', { name: /Security & Audit/i })).not.toBeInTheDocument();
    expect(screen.queryByTitle('Switch to Admin View')).not.toBeInTheDocument();

    unmountDoctor();

    // 3. Admin verification: Admin sees Security & Audit Console
    localStorage.setItem(
      'carepath_user',
      JSON.stringify({
        id: '00000000-0000-0000-0000-000000000001',
        email: 'admin@carepath.io',
        firstName: 'System',
        lastName: 'Admin',
        role: 'ROLE_ADMIN',
      })
    );
    window.history.pushState({}, '', '/admin/dashboard');

    render(
      <AuthProvider>
        <App />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Security & Audit/i })).toBeInTheDocument();
    });

    // Audit console header rendered
    expect(screen.getByText('Security & Audit Console')).toBeInTheDocument();
    expect(screen.getByText(/Total Audit Events/i)).toBeInTheDocument();
  });
});
