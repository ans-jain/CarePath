import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import React from 'react';
import { CounterfactualCards } from '../components/assessment/CounterfactualCards';
import { CounterfactualResult } from '../types/explainability';

describe('CounterfactualCards', () => {
  const mockSuccessResult: CounterfactualResult = {
    counterfactual_available: true,
    status: 'SUCCESS',
    message: 'Generated 2 actionable counterfactual recommendations.',
    plans: [
      {
        plan_id: 'cf_1',
        title: 'Systolic Blood Pressure Normalization',
        simulated_risk_score: 0.325,
        simulated_risk_category: 'MODERATE',
        risk_reduction: 0.285,
        tier_transition_achieved: true,
        changed_features: [
          {
            feature: 'systolic_bp_current',
            display_name: 'Systolic Blood Pressure',
            current_value: 152,
            target_value: 124,
            unit: 'mmHg',
            change_delta: -28,
            allowed_range: [90, 180],
            direction: 'decrease',
            modifiable: true,
          },
        ],
        rationale: 'Lowering resting systolic blood pressure to standard baseline shifts patient into Moderate tier.',
      },
    ],
  };

  const mockEmptyResult: CounterfactualResult = {
    counterfactual_available: false,
    status: 'NO_VALID_COUNTERFACTUAL_FOUND',
    message: 'Patient is currently within the low baseline tier.',
    plans: [],
  };

  it('renders counterfactual scenarios, simulated risk, delta, and tier transition', () => {
    render(<CounterfactualCards counterfactuals={mockSuccessResult} originalRiskScore={0.61} />);

    expect(screen.getByText("Potential Ways the Model's Risk Estimate Could Change")).toBeInTheDocument();
    expect(screen.getByText('Scenario #1')).toBeInTheDocument();
    expect(screen.getByText('Tier Transition')).toBeInTheDocument();
    expect(screen.getByText('Systolic Blood Pressure Normalization')).toBeInTheDocument();
    expect(screen.getByText('0.3250')).toBeInTheDocument();
    expect(screen.getByText('(-28.5%)')).toBeInTheDocument();
    expect(screen.getByText('Systolic Blood Pressure')).toBeInTheDocument();
    expect(screen.getByText('-28 mmHg')).toBeInTheDocument();
    expect(screen.getByText('152')).toBeInTheDocument();
    expect(screen.getByText('124 mmHg')).toBeInTheDocument();
    expect(screen.getByText(/"Lowering resting systolic blood pressure to standard baseline shifts patient into Moderate tier."/i)).toBeInTheDocument();
  });

  it('renders graceful empty state when no valid counterfactual is found', () => {
    render(<CounterfactualCards counterfactuals={mockEmptyResult} originalRiskScore={0.15} />);

    expect(screen.getByText('No Valid Constrained Counterfactual Found')).toBeInTheDocument();
    expect(screen.getByText('Patient is currently within the low baseline tier.')).toBeInTheDocument();
  });

  it('renders the mandatory non-prescriptive simulation disclaimer', () => {
    render(<CounterfactualCards counterfactuals={mockSuccessResult} originalRiskScore={0.61} />);

    expect(screen.getByText(/Important Clinical Disclaimer:/i)).toBeInTheDocument();
    expect(screen.getByText(/medical advice, prescriptions, or clinical orders/i)).toBeInTheDocument();
  });
});
