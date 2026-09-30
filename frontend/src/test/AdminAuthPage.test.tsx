import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import React from 'react';
import { AdminAuthPage } from '../pages/AdminAuthPage';
import * as authContextModule from '../context/AuthContext';

describe('AdminAuthPage', () => {
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

  it('renders restricted access warning and sign in form without registration tab', () => {
    render(
      <AdminAuthPage
        onBackToRoles={mockOnBackToRoles}
        onAuthSuccess={mockOnAuthSuccess}
      />
    );

    // Header
    expect(screen.getByText('System Administration')).toBeInTheDocument();

    // Restricted Access Banner
    expect(screen.getByText('Access Restricted')).toBeInTheDocument();
    expect(
      screen.getByText(/Administrator access is restricted. Public registration is not permitted./i)
    ).toBeInTheDocument();

    // No registration buttons
    expect(screen.queryByText(/Register as Admin/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Create Admin Account/i)).not.toBeInTheDocument();

    // Sign in inputs and buttons
    expect(screen.getByPlaceholderText('admin@carepath.io')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Sign In as Administrator/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Continue as Administrator \(Demo Admin\)/i })).toBeInTheDocument();
  });

  it('navigates back to role selection when clicking back button', () => {
    render(
      <AdminAuthPage
        onBackToRoles={mockOnBackToRoles}
        onAuthSuccess={mockOnAuthSuccess}
      />
    );

    fireEvent.click(screen.getByRole('button', { name: /Back to Role Selection/i }));
    expect(mockOnBackToRoles).toHaveBeenCalled();
  });

  it('successfully logs in admin and calls onAuthSuccess', async () => {
    mockLogin.mockResolvedValue(undefined);

    render(
      <AdminAuthPage
        onBackToRoles={mockOnBackToRoles}
        onAuthSuccess={mockOnAuthSuccess}
      />
    );

    fireEvent.click(screen.getByRole('button', { name: /Sign In as Administrator/i }));

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith({
        email: 'admin@carepath.io',
        password: 'AdminMaster123!',
      });
      expect(mockOnAuthSuccess).toHaveBeenCalled();
    });
  });
});
