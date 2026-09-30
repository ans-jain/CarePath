export type { AccountStatus } from './auth';

export interface AuditLogItem {
  id: string;
  actionType: string;
  actorUserId?: string | null;
  actorEmail?: string | null;
  entityName?: string | null;
  entityId?: string | null;
  targetPatientId?: string | null;
  details?: string | null;
  ipAddress?: string | null;
  userAgent?: string | null;
  status: string;
  createdAt: string;
}

export interface AuditLogPageResponse {
  content: AuditLogItem[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface SecurityOverview {
  totalAuditLogs: number;
  failedAccessAttempts: number;
  rateLimiterEnabled: boolean;
  securityStatus: string;
  activeAuditPolicies: string[];
  serverTime: string;
}

export interface AdminUserItem {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  phone?: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AdminUserPageResponse {
  content: AdminUserItem[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface DoctorRegistrationRequestItem {
  id: string;
  userId: string;
  name: string;
  firstName: string;
  lastName: string;
  email: string;
  licenseNumber: string;
  specialization: string;
  hospitalOrganization: string;
  phone?: string | null;
  registrationDate: string;
  status: 'PENDING' | 'ACTIVE' | 'REJECTED';
}

