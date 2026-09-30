export type UserRole = 'ROLE_PATIENT' | 'ROLE_CLINICIAN' | 'ROLE_ADMIN' | 'ROLE_DOCTOR';

export type AccountStatus = 'PENDING' | 'ACTIVE' | 'REJECTED';

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  status?: AccountStatus;
  phone?: string;
  isActive?: boolean;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
  role: UserRole;
}

export interface DoctorRegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  licenseNumber: string;
  specialization: string;
  hospitalOrganization: string;
  phone: string;
}

export interface DoctorRegisterResponse {
  userId: string;
  email: string;
  role: UserRole;
  status: AccountStatus;
  message: string;
}
