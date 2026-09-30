export type ReportShareStatus = 'NEW' | 'VIEWED' | 'REVOKED';

export interface DoctorDirectoryItem {
  userId: string;
  clinicianProfileId?: string;
  name: string;
  email: string;
  specialization: string;
  hospitalOrganization: string;
  licenseNumber: string;
  status: string;
}

export interface ReportShareItem {
  id: string;
  reportId: string;
  patientId: string;
  patientName: string;
  doctorId: string;
  doctorName: string;
  doctorEmail: string;
  doctorSpecialty?: string;
  doctorClinic?: string;
  sharedAt: string;
  viewedAt?: string | null;
  revokedAt?: string | null;
  status: ReportShareStatus;
}

export interface SharedReportDetail {
  shareId: string;
  reportId: string;
  patientId: string;
  patientName: string;
  reportDate: string;
  sharedDate: string;
  status: 'NEW' | 'VIEWED';
  overallRiskScore: number;
  riskCategory: 'LOW' | 'MODERATE' | 'ELEVATED' | 'HIGH';
  confidenceLevel?: string;
  modelVersion?: string;
  featureSnapshot?: string;
}
