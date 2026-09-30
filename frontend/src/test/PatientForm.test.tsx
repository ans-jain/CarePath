import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import React from 'react';
import { PatientForm } from '../components/assessment/PatientForm';

describe('PatientForm', () => {
  it('renders biomarker form sections and default high-risk preset values', () => {
    render(<PatientForm onSubmit={vi.fn()} isLoading={false} />);

    expect(screen.getByText('Patient Biomarker Assessment')).toBeInTheDocument();
    expect(screen.getByText('1. Patient Demographics')).toBeInTheDocument();
    expect(screen.getByText('2. Core Resting Vitals')).toBeInTheDocument();
    expect(screen.getByText(/3. Serum Biomarkers & Lifestyle/i)).toBeInTheDocument();

    const ageInput = screen.getByLabelText(/Age/i) as HTMLInputElement;
    expect(ageInput.value).toBe('56');

    const sbpInput = screen.getByLabelText(/Systolic BP/i) as HTMLInputElement;
    expect(sbpInput.value).toBe('148');
  });

  it('switches preset values when clicking preset buttons', () => {
    render(<PatientForm onSubmit={vi.fn()} isLoading={false} />);

    // Click "Low" preset
    const lowPresetBtn = screen.getByRole('button', { name: /^Low/i });
    fireEvent.click(lowPresetBtn);

    const sbpInput = screen.getByLabelText(/Systolic BP/i) as HTMLInputElement;
    expect(sbpInput.value).toBe('116');

    const ageInput = screen.getByLabelText(/Age/i) as HTMLInputElement;
    expect(ageInput.value).toBe('32');
  });

  it('calls onSubmit with structured ExplainRiskRequest when valid', () => {
    const handleSubmit = vi.fn();
    render(<PatientForm onSubmit={handleSubmit} isLoading={false} />);

    const submitBtn = screen.getByRole('button', { name: /Run Risk Assessment/i });
    fireEvent.click(submitBtn);

    expect(handleSubmit).toHaveBeenCalledTimes(1);
    const request = handleSubmit.mock.calls[0][0];

    expect(request.patient_uuid).toBeDefined();
    expect(request.top_k).toBe(4);
    expect(request.generate_counterfactuals).toBe(true);
    expect(request.features.age).toBe(56);
    expect(request.features.systolic_bp_current).toBe(148);
    expect(request.features.biological_sex).toBe('MALE');
  });

  it('validates that Systolic BP must exceed Diastolic BP', () => {
    const handleSubmit = vi.fn();
    render(<PatientForm onSubmit={handleSubmit} isLoading={false} />);

    const sbpInput = screen.getByLabelText(/Systolic BP/i);
    const dbpInput = screen.getByLabelText(/Diastolic BP/i);

    // Make systolic 80 and diastolic 90
    fireEvent.change(sbpInput, { target: { value: '80' } });
    fireEvent.change(dbpInput, { target: { value: '90' } });

    const submitBtn = screen.getByRole('button', { name: /Run Risk Assessment/i });
    fireEvent.click(submitBtn);

    expect(screen.getByText('Systolic BP must exceed Diastolic BP')).toBeInTheDocument();
    expect(handleSubmit).not.toHaveBeenCalled();
  });
});
