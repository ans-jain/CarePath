import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import React from 'react';
import { DoctorAuthPage } from '../pages/DoctorAuthPage';
import { authService } from '../services/authService';
import * as authContextModule from '../context/AuthContext';

describe('DoctorAuthPage', () => {
  const mockOnBackToRoles = vi.fn();
  const mockOnAuthSuccess = vi.fn();
  const mockLogin = vi.fn();
  const mockSwitchDemoPersona = vi.fn();

  beforeEach(() => {
    vi.restoreAllMocks();
    vi.spyOn(authContextModule, 'useAuth').mockReturnValue({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      login: mockLogin,
      register: vi.fn(),
      logout: vi.fn(),
      switchDemoPersona: mockSwitchDemoPersona,
    });
  });

  it('renders Doctor Sign In tab by default with demo doctor shortcut', () => {
    render(
      <DoctorAuthPage
        onBackToRoles={mockOnBackToRoles}
        onAuthSuccess={mockOnAuthSuccess}
      />
    );

    expect(screen.getByText('Clinician & Doctor Portal')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Doctor Sign In' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Register as Doctor' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Sign In to Doctor Dashboard/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Continue as Dr. Marcus/i })).toBeInTheDocument();
  });

  it('displays pending approval error message when logging in with a pending account', async () => {
    mockLogin.mockRejectedValue(
      new Error('Your doctor registration is still pending admin approval. You will be able to access your account once an administrator approves your registration.')
    );

    render(
      <DoctorAuthPage
        onBackToRoles={mockOnBackToRoles}
        onAuthSuccess={mockOnAuthSuccess}
      />
    );

    const submitBtn = screen.getByRole('button', { name: /Sign In to Doctor Dashboard/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(
        screen.getByText(
          'Your doctor registration is still pending admin approval. You will be able to access your account once an administrator approves your registration.'
        )
      ).toBeInTheDocument();
    });
    expect(mockOnAuthSuccess).not.toHaveBeenCalled();
  });

  it('displays rejected error message when logging in with a rejected account', async () => {
    mockLogin.mockRejectedValue(
      new Error('Your doctor registration request was not approved.')
    );

    render(
      <DoctorAuthPage
        onBackToRoles={mockOnBackToRoles}
        onAuthSuccess={mockOnAuthSuccess}
      />
    );

    const submitBtn = screen.getByRole('button', { name: /Sign In to Doctor Dashboard/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(
        screen.getByText('Your doctor registration request was not approved.')
      ).toBeInTheDocument();
    });
    expect(mockOnAuthSuccess).not.toHaveBeenCalled();
  });

  it('renders registration form fields and submits doctor registration successfully', async () => {
    vi.spyOn(authService, 'registerDoctor').mockResolvedValue({
      userId: 'doc-uuid-1234',
      email: 'dr.new@carepath.io',
      role: 'ROLE_CLINICIAN',
      status: 'PENDING',
      message: 'Your doctor registration request has been submitted for admin approval.',
    });

    render(
      <DoctorAuthPage
        initialMode="register"
        onBackToRoles={mockOnBackToRoles}
        onAuthSuccess={mockOnAuthSuccess}
      />
    );

    expect(screen.getByText(/Medical License \/ Reg No\./i)).toBeInTheDocument();
    expect(screen.getByText(/Specialization \*/i)).toBeInTheDocument();
    expect(screen.getByText(/Hospital \/ Organization \*/i)).toBeInTheDocument();
    expect(screen.getByText(/Phone Number \*/i)).toBeInTheDocument();

    const submitBtn = screen.getByRole('button', {
      name: /Submit Doctor Registration for Approval/i,
    });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Registration Submitted')).toBeInTheDocument();
      expect(screen.getByText('Status: PENDING ADMIN APPROVAL')).toBeInTheDocument();
      expect(
        screen.getByText(
          /Your registration request has been submitted for admin approval. You will be able to access your account once an administrator approves your registration./i
        )
      ).toBeInTheDocument();
    });

    // Doctor is NOT automatically logged in or redirected to dashboard
    expect(mockOnAuthSuccess).not.toHaveBeenCalled();
  });
});
