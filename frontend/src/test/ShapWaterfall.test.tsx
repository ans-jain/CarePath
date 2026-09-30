import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import React from 'react';
import { ShapWaterfall } from '../components/assessment/ShapWaterfall';
import { ShapExplanation } from '../types/explainability';

describe('ShapWaterfall', () => {
  const mockExplanation: ShapExplanation = {
    base_value: 0.285,
    shap_values: {
      systolic_bp_current: 0.185,
      bmi: 0.092,
      sleep_hours_current: -0.064,
    },
    top_risk_drivers: [
      {
        feature: 'systolic_bp_current',
        display_name: 'Systolic Blood Pressure',
        value: 148,
        shap_value: 0.185,
        direction: 'increases_risk',
        impact: 'high',
        narrative: 'Elevated systolic BP increases modeled cardiometabolic risk by +0.1850.',
      },
      {
        feature: 'bmi',
        display_name: 'Body Mass Index',
        value: 29.8,
        shap_value: 0.092,
        direction: 'increases_risk',
        impact: 'medium',
        narrative: 'BMI of 29.8 increases modeled risk by +0.0920.',
      },
    ],
    protective_factors: [
      {
        feature: 'sleep_hours_current',
        display_name: 'Daily Sleep Duration',
        value: 7.8,
        shap_value: -0.064,
        direction: 'decreases_risk',
        impact: 'medium',
        narrative: 'Sufficient sleep duration buffers risk by -0.0640.',
      },
    ],
    summary_narrative:
      'Modeled risk is primarily elevated by systolic BP (+0.1850) and BMI (+0.0920), partially buffered by healthy sleep patterns (-0.0640).',
  };

  it('renders section title, base value, and narrative summary', () => {
    render(<ShapWaterfall explanation={mockExplanation} />);

    expect(screen.getByText('Why Did the Model Predict This Risk Score?')).toBeInTheDocument();
    expect(screen.getByText(/Reference Baseline: 0.2850/i)).toBeInTheDocument();
    expect(screen.getByText(mockExplanation.summary_narrative)).toBeInTheDocument();
  });

  it('renders top risk drivers and protective factors with signs and values', () => {
    render(<ShapWaterfall explanation={mockExplanation} />);

    expect(screen.getByText('Primary Risk Drivers')).toBeInTheDocument();
    expect(screen.getByText('2 factors')).toBeInTheDocument();
    expect(screen.getAllByText('+0.1850').length).toBeGreaterThan(0);
    expect(screen.getByText('Elevated systolic BP increases modeled cardiometabolic risk by +0.1850.')).toBeInTheDocument();

    expect(screen.getByText('Protective / Buffering Factors')).toBeInTheDocument();
    expect(screen.getByText('1 factors')).toBeInTheDocument();
    expect(screen.getAllByText('-0.0640').length).toBeGreaterThan(0);
    expect(screen.getByText('Sufficient sleep duration buffers risk by -0.0640.')).toBeInTheDocument();
  });

  it('displays the non-causal interpretability notice', () => {
    render(<ShapWaterfall explanation={mockExplanation} />);

    expect(screen.getByText(/SHAP Interpretability Notice:/i)).toBeInTheDocument();
    expect(screen.getByText(/establish biological cause-and-effect/i)).toBeInTheDocument();
  });

  it('invokes onOpenHowItWorks callback when clicking Attribution Guide', () => {
    const handleOpenHowItWorks = vi.fn();
    render(<ShapWaterfall explanation={mockExplanation} onOpenHowItWorks={handleOpenHowItWorks} />);

    const guideBtn = screen.getByText('Attribution Guide');
    expect(guideBtn).toBeInTheDocument();
    fireEvent.click(guideBtn);
    expect(handleOpenHowItWorks).toHaveBeenCalledTimes(1);
  });
});
