import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import React from 'react';
import { AuthEntryPage } from '../pages/AuthEntryPage';

describe('AuthEntryPage', () => {
  it('renders all three healthcare role selection cards with required descriptions', () => {
    const handleSelectRole = vi.fn();
    render(<AuthEntryPage onSelectRole={handleSelectRole} />);

    // Header & Brand
    expect(screen.getByText('Welcome to CarePath')).toBeInTheDocument();
    expect(screen.getByText('Choose how you want to access the platform')).toBeInTheDocument();

    // Patient Card
    expect(screen.getByRole('heading', { name: 'Patient' })).toBeInTheDocument();
    expect(
      screen.getByText('Access your health profile, risk predictions, explanations and recommendations.')
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Continue as Patient/i })).toBeInTheDocument();

    // Doctor Card
    expect(screen.getByRole('heading', { name: 'Doctor' })).toBeInTheDocument();
    expect(
      screen.getByText(
        'Review patient risk scores, analyze explainable AI factors, inspect counterfactuals, and manage high-risk cases.'
      )
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Sign In as Doctor/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Register as Doctor/i })).toBeInTheDocument();

    // Admin Card
    expect(screen.getByRole('heading', { name: 'Admin' })).toBeInTheDocument();
    expect(
      screen.getByText(
        'Manage doctor approvals, oversee platform audit logs, monitor system health, and enforce clinical safety controls.'
      )
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Sign In as Administrator/i })).toBeInTheDocument();
  });

  it('invokes onSelectRole callback with correct role and mode', () => {
    const handleSelectRole = vi.fn();
    render(<AuthEntryPage onSelectRole={handleSelectRole} />);

    // Click Continue as Patient
    fireEvent.click(screen.getByRole('button', { name: /Continue as Patient/i }));
    expect(handleSelectRole).toHaveBeenCalledWith('patient', 'login');

    // Click Sign In as Doctor
    fireEvent.click(screen.getByRole('button', { name: /Sign In as Doctor/i }));
    expect(handleSelectRole).toHaveBeenCalledWith('doctor', 'login');

    // Click Register as Doctor
    fireEvent.click(screen.getByRole('button', { name: /Register as Doctor/i }));
    expect(handleSelectRole).toHaveBeenCalledWith('doctor', 'register');

    // Click Sign In as Administrator
    fireEvent.click(screen.getByRole('button', { name: /Sign In as Administrator/i }));
    expect(handleSelectRole).toHaveBeenCalledWith('admin', 'login');
  });
});
