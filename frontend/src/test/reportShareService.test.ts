import { describe, it, expect, beforeEach, vi } from 'vitest';
import { reportShareService } from '../services/reportShareService';

describe('reportShareService', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('retrieves verified doctors list', async () => {
    const doctors = await reportShareService.getVerifiedDoctors();
    expect(doctors).toBeDefined();
    expect(doctors.length).toBeGreaterThan(0);
    expect(doctors[0]).toHaveProperty('name');
    expect(doctors[0]).toHaveProperty('specialization');
    expect(doctors[0]).toHaveProperty('hospitalOrganization');
  });

  it('shares report with doctor and saves record locally', async () => {
    const share = await reportShareService.shareReport(
      'report-uuid-1',
      'doctor-uuid-robert-chen',
      {
        name: 'Dr. Robert Chen',
        email: 'dr.chen@carepath.io',
        specialization: 'Cardiology',
        hospitalOrganization: 'Metro Heart Center',
      },
      { name: 'Sarah Jenkins', id: 'patient-uuid-1' }
    );

    expect(share).toBeDefined();
    expect(share.reportId).toBe('report-uuid-1');
    expect(share.doctorId).toBe('doctor-uuid-robert-chen');
    expect(share.doctorName).toBe('Dr. Robert Chen');
    expect(share.status).toBe('NEW');

    // Verify lookup for this report
    const sharesForReport = await reportShareService.getSharesForReport('report-uuid-1');
    expect(sharesForReport).toHaveLength(1);
    expect(sharesForReport[0].status).toBe('NEW');
  });

  it('revokes access to a shared report', async () => {
    const share = await reportShareService.shareReport(
      'report-uuid-2',
      'doctor-uuid-priya-patel',
      { name: 'Dr. Priya Patel' }
    );

    expect(share.status).toBe('NEW');

    const revoked = await reportShareService.revokeShare(share.id);
    expect(revoked.status).toBe('REVOKED');
    expect(revoked.revokedAt).toBeDefined();

    // Verify doctor shared reports exclude revoked shares
    const doctorReports = await reportShareService.getDoctorSharedReports('doctor-uuid-priya-patel');
    const matched = doctorReports.find((r) => r.reportId === 'report-uuid-2');
    expect(matched).toBeUndefined();
  });

  it('marks shared report status as VIEWED when doctor accesses detail', async () => {
    await reportShareService.shareReport(
      'report-uuid-3',
      'doctor-uuid-robert-chen',
      { name: 'Dr. Robert Chen' }
    );

    const detail = await reportShareService.getDoctorSharedReportDetail('report-uuid-3');
    expect(detail).toBeDefined();
    expect(detail?.status).toBe('VIEWED');
  });
});
