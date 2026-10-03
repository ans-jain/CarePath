import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import React from 'react';
import { AdminAuditLogPage } from '../pages/AdminAuditLogPage';
import { AuthProvider } from '../context/AuthContext';
import { adminService } from '../services/adminService';
import * as authContextModule from '../context/AuthContext';

describe('AdminAuditLogPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders restricted access notice when user does not have ROLE_ADMIN', async () => {
    vi.spyOn(authContextModule, 'useAuth').mockReturnValue({
      user: {
        id: 'user-1',
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

    render(<AdminAuditLogPage />);

    expect(screen.getByText('Restricted Security Domain')).toBeInTheDocument();
    expect(screen.getByText(/requires elevated/i)).toBeInTheDocument();
    expect(screen.getByText('ROLE_ADMIN')).toBeInTheDocument();
    expect(screen.getByText('Switch to Administrator Persona')).toBeInTheDocument();
  });

  it('renders audit console, overview metrics, and logs table when user is ROLE_ADMIN', async () => {
    vi.spyOn(authContextModule, 'useAuth').mockReturnValue({
      user: {
        id: 'admin-1',
        email: 'admin@carepath.io',
        firstName: 'System',
        lastName: 'Admin',
        role: 'ROLE_ADMIN',
      },
      isAuthenticated: true,
      isLoading: false,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      switchDemoPersona: vi.fn(),
    });

    vi.spyOn(adminService, 'getSecurityOverview').mockResolvedValue({
      totalAuditLogs: 42,
      failedAccessAttempts: 3,
      rateLimiterEnabled: true,
      securityStatus: 'OPTIMAL',
      activeAuditPolicies: ['ROLE_BASED_ACCESS_CONTROL'],
      serverTime: new Date().toISOString(),
    });

    vi.spyOn(adminService, 'getAuditLogs').mockResolvedValue({
      content: [
        {
          id: 'log-1',
          actionType: 'FORBIDDEN_ACCESS_ATTEMPT',
          actorUserId: 'user-clinician',
          actorEmail: 'dr.marcus@carepath.io',
          entityName: 'PatientProfile',
          entityId: 'profile-1',
          targetPatientId: 'profile-1',
          details: 'Unassigned clinician probe',
          ipAddress: '10.0.0.1',
          userAgent: 'TestAgent/1.0',
          status: 'FORBIDDEN',
          createdAt: new Date().toISOString(),
        },
        {
          id: 'log-2',
          actionType: 'LOGIN_SUCCESS',
          actorUserId: 'user-admin',
          actorEmail: 'admin@carepath.io',
          entityName: 'User',
          entityId: 'user-admin',
          details: 'Admin login',
          ipAddress: '127.0.0.1',
          userAgent: 'TestAgent/1.0',
          status: 'SUCCESS',
          createdAt: new Date().toISOString(),
        },
      ],
      totalElements: 2,
      totalPages: 1,
      number: 0,
      size: 15,
      first: true,
      last: true,
    });

    render(<AdminAuditLogPage />);

    await waitFor(() => {
      expect(screen.getByText('Security & Audit Console')).toBeInTheDocument();
      expect(screen.getByText('42')).toBeInTheDocument(); // totalAuditLogs
    });

    // Check metric cards
    expect(screen.getByText('3')).toBeInTheDocument(); // failedAccessAttempts
    expect(screen.getByText('Active')).toBeInTheDocument(); // rate limiter

    // Check table contents
    expect(screen.getByText('FORBIDDEN_ACCESS')).toBeInTheDocument();
    expect(screen.getAllByText('LOGIN_SUCCESS').length).toBeGreaterThanOrEqual(1);

    const forbiddenRow = screen.getByText('FORBIDDEN_ACCESS').closest('tr');
    expect(forbiddenRow).not.toBeNull();
    expect(within(forbiddenRow!).getByText('dr.marcus@carepath.io')).toBeInTheDocument();
    expect(within(forbiddenRow!).getByText('BLOCKED (403)')).toBeInTheDocument();
  });
});
