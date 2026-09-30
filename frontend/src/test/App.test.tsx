import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import React from 'react';
import App from '../App';
import { AuthProvider } from '../context/AuthContext';
import { mlService } from '../services/mlService';

describe('App Integration', () => {
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

  it('renders landing role selection page by default when unauthenticated at /', async () => {
    localStorage.clear();
    window.history.pushState({}, '', '/');
    render(
      <AuthProvider>
        <App />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('Welcome to CarePath')).toBeInTheDocument();
    });
    expect(screen.getByText('Choose how you want to access the platform')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Patient' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Doctor' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Admin' })).toBeInTheDocument();
  });

  it('renders navbar and patient risk assessment view when patient is authenticated', async () => {
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

    render(
      <AuthProvider>
        <App />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('CarePath')).toBeInTheDocument();
    });
    expect(screen.getByText('Cardiometabolic Risk Assessment')).toBeInTheDocument();
    expect(screen.getByText(/Strict Clinical Guardrail/i)).toBeInTheDocument();
    // Persona switcher should NOT be in the navbar
    expect(screen.queryByTitle('Switch to Clinician View')).not.toBeInTheDocument();
    expect(screen.queryByTitle('Switch to Admin View')).not.toBeInTheDocument();
  });

  it('opens and closes How It Works educational modal', async () => {
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

    render(
      <AuthProvider>
        <App />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /How It Works/i })).toBeInTheDocument();
    });

    const howItWorksBtn = screen.getByRole('button', { name: /How It Works/i });
    fireEvent.click(howItWorksBtn);

    expect(screen.getByText('How CarePath Explainable AI Works')).toBeInTheDocument();
    expect(screen.getByText(/SHAP \(TreeExplainer\) Attributions/i)).toBeInTheDocument();

    const gotItBtn = screen.getByRole('button', { name: 'Got It' });
    fireEvent.click(gotItBtn);

    await waitFor(() => {
      expect(screen.queryByText('How CarePath Explainable AI Works')).not.toBeInTheDocument();
    });
  });

  it('signs out and redirects to the landing selection page without role switcher tabs in navbar', async () => {
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

    render(
      <AuthProvider>
        <App />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('Sarah Jenkins')).toBeInTheDocument();
    });

    // Ensure no persona switcher exists
    expect(screen.queryByTitle('Switch to Patient View')).not.toBeInTheDocument();
    expect(screen.queryByTitle('Switch to Clinician View')).not.toBeInTheDocument();
    expect(screen.queryByTitle('Switch to Admin View')).not.toBeInTheDocument();

    // Click Sign Out
    const signOutBtn = screen.getByRole('button', { name: /Sign Out/i });
    fireEvent.click(signOutBtn);

    // Redirected to landing selection page
    await waitFor(() => {
      expect(screen.getByText('Welcome to CarePath')).toBeInTheDocument();
    });
    expect(screen.getByText('Choose how you want to access the platform')).toBeInTheDocument();
  });
});
