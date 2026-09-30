import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import React from 'react';
import App from '../App';
import * as authContextModule from '../context/AuthContext';
import { mlService } from '../services/mlService';

describe('Role-Based Routing & Access Control Guards', () => {
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

  it('renders AuthEntryPage when unauthenticated at /', async () => {
    window.history.pushState({}, '', '/');
    vi.spyOn(authContextModule, 'useAuth').mockReturnValue({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      switchDemoPersona: vi.fn(),
    });

    render(<App />);

    expect(screen.getByText('Welcome to CarePath')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Continue as Patient/i })).toBeInTheDocument();
  });

  it('renders PatientAuthPage when navigating to /auth/patient', async () => {
    window.history.pushState({}, '', '/auth/patient');
    vi.spyOn(authContextModule, 'useAuth').mockReturnValue({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      switchDemoPersona: vi.fn(),
    });

    render(<App />);

    expect(screen.getByText('Patient Health Portal')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Continue as Sarah/i })).toBeInTheDocument();
  });

  it('renders DoctorAuthPage when navigating to /auth/doctor', async () => {
    window.history.pushState({}, '', '/auth/doctor');
    vi.spyOn(authContextModule, 'useAuth').mockReturnValue({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      switchDemoPersona: vi.fn(),
    });

    render(<App />);

    expect(screen.getByText('Clinician & Doctor Portal')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Continue as Dr. Marcus/i })).toBeInTheDocument();
  });

  it('renders AdminAuthPage with restricted warning when navigating to /auth/admin', async () => {
    window.history.pushState({}, '', '/auth/admin');
    vi.spyOn(authContextModule, 'useAuth').mockReturnValue({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      switchDemoPersona: vi.fn(),
    });

    render(<App />);

    expect(screen.getByText('System Administration')).toBeInTheDocument();
    expect(screen.getByText(/Administrator access is restricted. Public registration is not permitted./i)).toBeInTheDocument();
  });

  it('blocks Patient from accessing /doctor/dashboard with "You do not have permission to access this area."', async () => {
    window.history.pushState({}, '', '/doctor/dashboard');
    vi.spyOn(authContextModule, 'useAuth').mockReturnValue({
      user: {
        id: 'patient-id',
        email: 'patient@carepath.io',
        firstName: 'Sarah',
        lastName: 'Jenkins',
        role: 'ROLE_PATIENT',
      },
      isAuthenticated: true,
      isLoading: false,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      switchDemoPersona: vi.fn(),
    });

    render(<App />);

    expect(screen.getByText('Access Denied')).toBeInTheDocument();
    expect(screen.getByText('You do not have permission to access this area.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Return to Your Dashboard/i })).toBeInTheDocument();
  });

  it('blocks Doctor from accessing /admin/dashboard with "You do not have permission to access this area."', async () => {
    window.history.pushState({}, '', '/admin/dashboard');
    vi.spyOn(authContextModule, 'useAuth').mockReturnValue({
      user: {
        id: 'doctor-id',
        email: 'dr.marcus@carepath.io',
        firstName: 'Marcus',
        lastName: 'Vance',
        role: 'ROLE_CLINICIAN',
      },
      isAuthenticated: true,
      isLoading: false,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      switchDemoPersona: vi.fn(),
    });

    render(<App />);

    expect(screen.getByText('Access Denied')).toBeInTheDocument();
    expect(screen.getByText('You do not have permission to access this area.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Return to Your Dashboard/i })).toBeInTheDocument();
  });
});
