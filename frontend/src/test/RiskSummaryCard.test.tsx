import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import React from 'react';
import { RiskSummaryCard } from '../components/assessment/RiskSummaryCard';
import { PredictRiskResponse } from '../types/explainability';

describe('RiskSummaryCard', () => {
  const lowRiskPrediction: PredictRiskResponse = {
    overall_risk_score: 0.1425,
    risk_category: 'LOW',
    risk_tier: 'LOW',
    probability: 0.1425,
    prediction: 0,
    calibrated: true,
    confidence_interval: [0.11, 0.18],
    confidence_level: 'LONGITUDINAL_ROBUST',
    model_version: 'calibrated_v1.0.0',
    regulatory_disclaimer: 'Clinical decision-support only.',
  };

  const highRiskPrediction: PredictRiskResponse = {
    overall_risk_score: 0.824,
    risk_category: 'HIGH',
    risk_tier: 'HIGH',
    probability: 0.824,
    prediction: 1,
    calibrated: true,
    confidence_interval: [0.78, 0.86],
    model_version: 'calibrated_v1.0.0',
    regulatory_disclaimer: 'Clinical decision-support only.',
  };

  it('renders low risk tier, formatted score, and longitudinal robust badge', () => {
    render(<RiskSummaryCard prediction={lowRiskPrediction} assessmentTimestamp="14:30" />);

    expect(screen.getByText('Low Cardiometabolic Risk')).toBeInTheDocument();
    expect(screen.getByText('0.1425')).toBeInTheDocument();
    expect(screen.getByText(/\(14\.\d%\)/)).toBeInTheDocument();
    expect(screen.getByText('Longitudinal Robust')).toBeInTheDocument();
    expect(screen.getByText(/Patient biomarkers reside comfortably within empirical reference cohort ranges/i)).toBeInTheDocument();
  });

  it('renders high risk tier with appropriate clinical badge and progress bar', () => {
    render(<RiskSummaryCard prediction={highRiskPrediction} assessmentTimestamp="14:30" />);

    expect(screen.getByText('High Risk')).toBeInTheDocument();
    expect(screen.getByText('0.8240')).toBeInTheDocument();
    expect(screen.getByText('(82.4%)')).toBeInTheDocument();
    expect(screen.getByText(/High statistical probability of cardiometabolic decompensation/i)).toBeInTheDocument();

    const progressBar = screen.getByRole('progressbar');
    expect(progressBar).toBeInTheDocument();
    expect(progressBar).toHaveAttribute('aria-valuenow', '0.824');
  });

  it('renders model version and calibration information', () => {
    render(<RiskSummaryCard prediction={lowRiskPrediction} assessmentTimestamp="10:15 AM" />);

    expect(screen.getByText('calibrated_v1.0.0')).toBeInTheDocument();
    expect(screen.getByText('Calibrated via Sigmoid / Platt Scaling')).toBeInTheDocument();
    expect(screen.getByText(/Evaluated at 10:15 AM/i)).toBeInTheDocument();
  });
});
