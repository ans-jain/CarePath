import { requestJson, BACKEND_BASE_URL } from './api';
import {
  AuditLogItem,
  AuditLogPageResponse,
  SecurityOverview,
  AdminUserItem,
  AdminUserPageResponse,
  DoctorRegistrationRequestItem,
} from '../types/admin';

const DEMO_AUDIT_LOGS_KEY = 'carepath_demo_audit_logs';

function getStoredDemoAuditLogs(): AuditLogItem[] {
  try {
    const raw = localStorage.getItem(DEMO_AUDIT_LOGS_KEY);
    if (raw) return JSON.parse(raw);
  } catch (e) {
    // fallback
  }

  const initial: AuditLogItem[] = [
    {
      id: 'log-001',
      actionType: 'LOGIN_SUCCESS',
      actorUserId: '00000000-0000-0000-0000-000000000001',
      actorEmail: 'admin@carepath.io',
      entityName: 'User',
      entityId: '00000000-0000-0000-0000-000000000001',
      details: 'Principal authenticated via Spring Security JWT',
      ipAddress: '127.0.0.1',
      userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
      status: 'SUCCESS',
      createdAt: new Date(Date.now() - 2 * 60 * 1000).toISOString(),
    },
    {
      id: 'log-002',
      actionType: 'RISK_ASSESSMENT_COMPLETED',
      actorUserId: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
      actorEmail: 'patient.sarah@carepath.io',
      entityName: 'RiskAssessment',
      entityId: 'a7b8c9d0-1234-5678-9abc-def012345678',
      targetPatientId: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
      details: 'Risk tier evaluated: ELEVATED, score: 0.720',
      ipAddress: '192.168.1.105',
      userAgent: 'CarePath-React-Web/1.0',
      status: 'SUCCESS',
      createdAt: new Date(Date.now() - 15 * 60 * 1000).toISOString(),
    },
    {
      id: 'log-003',
      actionType: 'FORBIDDEN_ACCESS_ATTEMPT',
      actorUserId: '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d',
      actorEmail: 'dr.marcus@carepath.io',
      entityName: 'PatientProfile',
      entityId: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
      details: 'Clinician lacks active PatientClinicianAccess grant for patient 9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
      ipAddress: '10.0.0.45',
      userAgent: 'CarePath-React-Web/1.0',
      status: 'FORBIDDEN',
      createdAt: new Date(Date.now() - 45 * 60 * 1000).toISOString(),
    },
    {
      id: 'log-004',
      actionType: 'VITALS_INGESTED',
      actorUserId: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
      actorEmail: 'patient.sarah@carepath.io',
      entityName: 'VitalMetric',
      entityId: 'f1e2d3c4-b5a6-7890-1234-56789abcdef0',
      targetPatientId: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
      details: 'Metric SYSTOLIC_BP recorded with value 128.00 mmHg',
      ipAddress: '192.168.1.105',
      userAgent: 'CarePath-React-Web/1.0',
      status: 'SUCCESS',
      createdAt: new Date(Date.now() - 120 * 60 * 1000).toISOString(),
    },
    {
      id: 'log-005',
      actionType: 'ROLE_CHANGE',
      actorUserId: '00000000-0000-0000-0000-000000000001',
      actorEmail: 'admin@carepath.io',
      entityName: 'User',
      entityId: '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d',
      details: 'Role updated to ROLE_CLINICIAN by administrator',
      ipAddress: '127.0.0.1',
      userAgent: 'CarePath-Admin-Portal/1.0',
      status: 'SUCCESS',
      createdAt: new Date(Date.now() - 360 * 60 * 1000).toISOString(),
    },
  ];

  try {
    localStorage.setItem(DEMO_AUDIT_LOGS_KEY, JSON.stringify(initial));
  } catch (e) {}
  return initial;
}

export const adminService = {
  async getAuditLogs(
    page = 0,
    size = 20,
    actionType?: string,
    status?: string,
    search?: string
  ): Promise<AuditLogPageResponse> {
    try {
      const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString(),
      });
      if (actionType) params.append('actionType', actionType);
      if (status) params.append('status', status);
      if (search) params.append('search', search);

      return await requestJson<AuditLogPageResponse>(
        `${BACKEND_BASE_URL}/admin/audit-logs?${params.toString()}`
      );
    } catch (e) {
      console.warn('[AdminService] Backend unreachable or test mode; using demo audit records', e);
      let logs = getStoredDemoAuditLogs();
      if (actionType) {
        logs = logs.filter((l) => l.actionType === actionType);
      }
      if (status) {
        logs = logs.filter((l) => l.status === status);
      }
      if (search) {
        const q = search.toLowerCase();
        logs = logs.filter(
          (l) =>
            l.actionType.toLowerCase().includes(q) ||
            (l.actorEmail && l.actorEmail.toLowerCase().includes(q)) ||
            (l.details && l.details.toLowerCase().includes(q)) ||
            (l.entityName && l.entityName.toLowerCase().includes(q))
        );
      }

      const totalElements = logs.length;
      const totalPages = Math.ceil(totalElements / size) || 1;
      const start = page * size;
      const content = logs.slice(start, start + size);

      return {
        content,
        totalElements,
        totalPages,
        number: page,
        size,
        first: page === 0,
        last: page >= totalPages - 1,
      };
    }
  },

  async getSecurityOverview(): Promise<SecurityOverview> {
    try {
      return await requestJson<SecurityOverview>(`${BACKEND_BASE_URL}/admin/security/overview`);
    } catch (e) {
      const logs = getStoredDemoAuditLogs();
      const failed = logs.filter((l) => l.status === 'FORBIDDEN' || l.status === 'FAILURE').length;
      return {
        totalAuditLogs: logs.length,
        failedAccessAttempts: failed,
        rateLimiterEnabled: true,
        securityStatus: 'OPTIMAL',
        activeAuditPolicies: [
          'ROLE_BASED_ACCESS_CONTROL',
          'PATIENT_CLINICIAN_RELATIONSHIP_VERIFICATION',
          'IMMUTABLE_AUDIT_LOGGING',
          'SLIDING_WINDOW_RATE_LIMITING',
          'CREDENTIAL_SANITIZATION',
        ],
        serverTime: new Date().toISOString(),
      };
    }
  },

  async getUsers(page = 0, size = 20, search?: string): Promise<AdminUserPageResponse> {
    try {
      const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString(),
      });
      if (search) params.append('search', search);

      return await requestJson<AdminUserPageResponse>(
        `${BACKEND_BASE_URL}/admin/users?${params.toString()}`
      );
    } catch (e) {
      const mockUsers: AdminUserItem[] = [
        {
          id: '00000000-0000-0000-0000-000000000001',
          email: 'admin@carepath.io',
          firstName: 'System',
          lastName: 'Admin',
          role: 'ROLE_ADMIN',
          isActive: true,
          createdAt: new Date(Date.now() - 30 * 24 * 3600 * 1000).toISOString(),
          updatedAt: new Date().toISOString(),
        },
        {
          id: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
          email: 'patient.sarah@carepath.io',
          firstName: 'Sarah',
          lastName: 'Jenkins',
          role: 'ROLE_PATIENT',
          isActive: true,
          createdAt: new Date(Date.now() - 10 * 24 * 3600 * 1000).toISOString(),
          updatedAt: new Date().toISOString(),
        },
        {
          id: '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d',
          email: 'dr.marcus@carepath.io',
          firstName: 'Marcus',
          lastName: 'Vance',
          role: 'ROLE_CLINICIAN',
          isActive: true,
          createdAt: new Date(Date.now() - 8 * 24 * 3600 * 1000).toISOString(),
          updatedAt: new Date().toISOString(),
        },
      ];
      return {
        content: mockUsers,
        totalElements: mockUsers.length,
        totalPages: 1,
        number: 0,
        size: 20,
      };
    }
  },

  async getDoctorRequests(status?: string): Promise<DoctorRegistrationRequestItem[]> {
    try {
      const url = status && status !== 'ALL'
        ? `${BACKEND_BASE_URL}/admin/doctor-requests?status=${status}`
        : `${BACKEND_BASE_URL}/admin/doctor-requests`;
      return await requestJson<DoctorRegistrationRequestItem[]>(url);
    } catch (e) {
      // Demo fallback
      const stored = localStorage.getItem('carepath_demo_doctor_requests');
      if (stored) {
        try {
          const list: DoctorRegistrationRequestItem[] = JSON.parse(stored);
          if (status && status !== 'ALL') {
            return list.filter((d) => d.status === status);
          }
          return list;
        } catch {}
      }
      const initial: DoctorRegistrationRequestItem[] = [
        {
          id: 'doc-req-001',
          userId: '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d',
          name: 'Dr. Marcus Vance',
          firstName: 'Marcus',
          lastName: 'Vance',
          email: 'dr.marcus@carepath.io',
          licenseNumber: 'MD-8921-CARDIO',
          specialization: 'Cardiology',
          hospitalOrganization: 'Metropolitan Heart & Vascular Institute',
          phone: '+15552000002',
          registrationDate: new Date(Date.now() - 48 * 3600 * 1000).toISOString(),
          status: 'ACTIVE',
        },
        {
          id: 'doc-req-002',
          userId: '2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e',
          name: 'Dr. Evelyn Reed',
          firstName: 'Evelyn',
          lastName: 'Reed',
          email: 'dr.reed@carepath.io',
          licenseNumber: 'MD-4432-ENDO',
          specialization: 'Endocrinology & Diabetology',
          hospitalOrganization: 'University Endocrine Associates',
          phone: '+15553334444',
          registrationDate: new Date(Date.now() - 2 * 3600 * 1000).toISOString(),
          status: 'PENDING',
        },
        {
          id: 'doc-req-003',
          userId: '3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f',
          name: 'Dr. Arthur Pendelton',
          firstName: 'Arthur',
          lastName: 'Pendelton',
          email: 'dr.pendelton@carepath.io',
          licenseNumber: 'MD-9011-INVALID',
          specialization: 'Alternative Therapies',
          hospitalOrganization: 'Holistic Wellness Clinic',
          phone: '+15558889999',
          registrationDate: new Date(Date.now() - 72 * 3600 * 1000).toISOString(),
          status: 'REJECTED',
        },
      ];
      localStorage.setItem('carepath_demo_doctor_requests', JSON.stringify(initial));
      if (status && status !== 'ALL') {
        return initial.filter((d) => d.status === status);
      }
      return initial;
    }
  },

  async approveDoctorRequest(userId: string): Promise<DoctorRegistrationRequestItem> {
    try {
      return await requestJson<DoctorRegistrationRequestItem>(
        `${BACKEND_BASE_URL}/admin/doctor-requests/${userId}/approve`,
        { method: 'POST' }
      );
    } catch (e) {
      const stored = localStorage.getItem('carepath_demo_doctor_requests');
      if (stored) {
        try {
          const list: DoctorRegistrationRequestItem[] = JSON.parse(stored);
          const item = list.find((d) => d.userId === userId);
          if (item) {
            item.status = 'ACTIVE';
            localStorage.setItem('carepath_demo_doctor_requests', JSON.stringify(list));
            return item;
          }
        } catch {}
      }
      throw e;
    }
  },

  async rejectDoctorRequest(userId: string): Promise<DoctorRegistrationRequestItem> {
    try {
      return await requestJson<DoctorRegistrationRequestItem>(
        `${BACKEND_BASE_URL}/admin/doctor-requests/${userId}/reject`,
        { method: 'POST' }
      );
    } catch (e) {
      const stored = localStorage.getItem('carepath_demo_doctor_requests');
      if (stored) {
        try {
          const list: DoctorRegistrationRequestItem[] = JSON.parse(stored);
          const item = list.find((d) => d.userId === userId);
          if (item) {
            item.status = 'REJECTED';
            localStorage.setItem('carepath_demo_doctor_requests', JSON.stringify(list));
            return item;
          }
        } catch {}
      }
      throw e;
    }
  },
};
