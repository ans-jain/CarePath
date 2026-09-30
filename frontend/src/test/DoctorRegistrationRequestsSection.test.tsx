import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import React from 'react';
import { DoctorRegistrationRequestsSection } from '../components/admin/DoctorRegistrationRequestsSection';
import { adminService } from '../services/adminService';
import { DoctorRegistrationRequestItem } from '../types/admin';

const mockDoctors: DoctorRegistrationRequestItem[] = [
  {
    id: 'req-1',
    userId: 'user-doc-1',
    name: 'Dr. Evelyn Reed',
    firstName: 'Evelyn',
    lastName: 'Reed',
    email: 'dr.reed@carepath.io',
    licenseNumber: 'MD-4432-ENDO',
    specialization: 'Endocrinology & Diabetology',
    hospitalOrganization: 'University Endocrine Associates',
    phone: '+15553334444',
    registrationDate: new Date().toISOString(),
    status: 'PENDING',
  },
  {
    id: 'req-2',
    userId: 'user-doc-2',
    name: 'Dr. Marcus Vance',
    firstName: 'Marcus',
    lastName: 'Vance',
    email: 'dr.marcus@carepath.io',
    licenseNumber: 'MD-984321',
    specialization: 'Cardiology',
    hospitalOrganization: 'Metropolitan General Hospital',
    phone: '+15552000002',
    registrationDate: new Date().toISOString(),
    status: 'ACTIVE',
  },
];

describe('DoctorRegistrationRequestsSection', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.spyOn(adminService, 'getDoctorRequests').mockResolvedValue(mockDoctors);
  });

  it('renders requests table with all doctor details', async () => {
    render(<DoctorRegistrationRequestsSection />);

    await waitFor(() => {
      expect(screen.getByText('Doctor Registration Requests')).toBeInTheDocument();
      expect(screen.getByText('Dr. Evelyn Reed')).toBeInTheDocument();
      expect(screen.getByText('dr.reed@carepath.io')).toBeInTheDocument();
      expect(screen.getByText('MD-4432-ENDO')).toBeInTheDocument();
      expect(screen.getByText('Endocrinology & Diabetology')).toBeInTheDocument();
      expect(screen.getByText('University Endocrine Associates')).toBeInTheDocument();
      expect(screen.getByText('+15553334444')).toBeInTheDocument();
      expect(screen.getAllByText('Pending Review').length).toBeGreaterThanOrEqual(1);
    });

    // Also renders active doctor
    expect(screen.getByText('Dr. Marcus Vance')).toBeInTheDocument();
    expect(screen.getByText('Approved')).toBeInTheDocument();
  });

  it('approves a pending doctor application when clicking Approve', async () => {
    vi.spyOn(adminService, 'approveDoctorRequest').mockResolvedValue({
      ...mockDoctors[0],
      status: 'ACTIVE',
    });

    render(<DoctorRegistrationRequestsSection />);

    await waitFor(() => {
      expect(screen.getByText('Dr. Evelyn Reed')).toBeInTheDocument();
    });

    const approveBtn = screen.getByRole('button', { name: /^Approve$/i });
    fireEvent.click(approveBtn);

    await waitFor(() => {
      expect(adminService.approveDoctorRequest).toHaveBeenCalledWith('user-doc-1');
      expect(screen.getByText(/Successfully approved Dr. Evelyn Reed/i)).toBeInTheDocument();
    });
  });

  it('rejects a pending doctor application when clicking Reject', async () => {
    vi.spyOn(adminService, 'rejectDoctorRequest').mockResolvedValue({
      ...mockDoctors[0],
      status: 'REJECTED',
    });

    render(<DoctorRegistrationRequestsSection />);

    await waitFor(() => {
      expect(screen.getByText('Dr. Evelyn Reed')).toBeInTheDocument();
    });

    const rejectBtn = screen.getByRole('button', { name: /^Reject$/i });
    fireEvent.click(rejectBtn);

    await waitFor(() => {
      expect(adminService.rejectDoctorRequest).toHaveBeenCalledWith('user-doc-1');
      expect(screen.getByText(/Doctor registration for Dr. Evelyn Reed has been REJECTED/i)).toBeInTheDocument();
    });
  });
});
