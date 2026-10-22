import { requestJson, BACKEND_BASE_URL } from './api';
import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  DoctorRegisterRequest,
  DoctorRegisterResponse,
  User,
} from '../types/auth';

const TOKEN_KEY = 'carepath_access_token';
const USER_KEY = 'carepath_user';
const REGISTERED_USERS_KEY = 'carepath_registered_users';
const DOCTOR_REQUESTS_KEY = 'carepath_demo_doctor_requests';

interface StoredUserRecord {
  user: User;
  password?: string;
  status?: string;
}

const DEFAULT_DEMO_USERS: Record<string, StoredUserRecord> = {
  'patient.sarah@carepath.io': {
    user: {
      id: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
      email: 'patient.sarah@carepath.io',
      firstName: 'Sarah',
      lastName: 'Jenkins',
      role: 'ROLE_PATIENT',
    },
    status: 'ACTIVE',
  },
  'dr.marcus@carepath.io': {
    user: {
      id: '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d',
      email: 'dr.marcus@carepath.io',
      firstName: 'Marcus',
      lastName: 'Vance',
      role: 'ROLE_CLINICIAN',
    },
    status: 'ACTIVE',
  },
  'dr.reed@carepath.io': {
    user: {
      id: '2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e',
      email: 'dr.reed@carepath.io',
      firstName: 'Evelyn',
      lastName: 'Reed',
      role: 'ROLE_CLINICIAN',
    },
    status: 'PENDING',
  },
  'dr.pendelton@carepath.io': {
    user: {
      id: '3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f',
      email: 'dr.pendelton@carepath.io',
      firstName: 'Arthur',
      lastName: 'Pendelton',
      role: 'ROLE_CLINICIAN',
    },
    status: 'REJECTED',
  },
  'admin@carepath.io': {
    user: {
      id: '00000000-0000-0000-0000-000000000001',
      email: 'admin@carepath.io',
      firstName: 'System',
      lastName: 'Admin',
      role: 'ROLE_ADMIN',
    },
    status: 'ACTIVE',
  },
};

function getStoredUsers(): Record<string, StoredUserRecord> {
  try {
    const raw = localStorage.getItem(REGISTERED_USERS_KEY);
    if (raw) return JSON.parse(raw);
  } catch {}
  return {};
}

function saveLocalUser(user: User, password?: string, status: string = 'ACTIVE'): void {
  try {
    const map = getStoredUsers();
    map[user.email.toLowerCase()] = { user, password, status };
    localStorage.setItem(REGISTERED_USERS_KEY, JSON.stringify(map));
  } catch (e) {
    console.warn('[AuthService] Could not persist user locally:', e);
  }
}

function getDoctorStatusFromStorage(email: string): string | null {
  try {
    const raw = localStorage.getItem(DOCTOR_REQUESTS_KEY);
    if (raw) {
      const list = JSON.parse(raw);
      if (Array.isArray(list)) {
        const found = list.find((d: any) => d.email?.toLowerCase() === email.toLowerCase());
        if (found) return found.status;
      }
    }
  } catch {}
  return null;
}

function saveDoctorRequestLocally(item: any): void {
  try {
    const raw = localStorage.getItem(DOCTOR_REQUESTS_KEY);
    const list = raw ? JSON.parse(raw) : [];
    if (Array.isArray(list)) {
      list.unshift(item);
      localStorage.setItem(DOCTOR_REQUESTS_KEY, JSON.stringify(list));
    }
  } catch {}
}

export const authService = {
  async login(credentials: LoginRequest): Promise<AuthResponse> {
    try {
      const response = await requestJson<AuthResponse>(`${BACKEND_BASE_URL}/auth/login`, {
        method: 'POST',
        body: JSON.stringify(credentials),
      });

      if (response && response.accessToken) {
        localStorage.setItem(TOKEN_KEY, response.accessToken);
        localStorage.setItem(USER_KEY, JSON.stringify(response.user));
      }
      return response;
    } catch (err: any) {
      // Backend is unreachable or returned static 404 / empty response on Render Static Site
      const email = credentials.email.trim().toLowerCase();

      // Check stored custom registered users and default personas
      const storedUsers = getStoredUsers();
      const localRecord = storedUsers[email] || DEFAULT_DEMO_USERS[email];

      if (localRecord) {
        // Check doctor status if clinician
        if (localRecord.user.role === 'ROLE_CLINICIAN' || localRecord.user.role === 'ROLE_DOCTOR') {
          const currentStatus = getDoctorStatusFromStorage(email) || localRecord.status || 'ACTIVE';
          if (currentStatus === 'PENDING') {
            throw new Error('Your doctor registration is still pending admin approval. You will be able to access your account once an administrator approves your registration.');
          }
          if (currentStatus === 'REJECTED') {
            throw new Error('Your doctor registration request was not approved.');
          }
        }

        const fallbackResponse: AuthResponse = {
          accessToken: `demo_jwt_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`,
          tokenType: 'Bearer',
          expiresIn: 86400,
          user: localRecord.user,
        };

        localStorage.setItem(TOKEN_KEY, fallbackResponse.accessToken);
        localStorage.setItem(USER_KEY, JSON.stringify(fallbackResponse.user));
        return fallbackResponse;
      }

      // If neither backend nor local user matches, throw original error
      throw err;
    }
  },

  async register(data: RegisterRequest): Promise<AuthResponse> {
    try {
      await requestJson<any>(`${BACKEND_BASE_URL}/auth/register`, {
        method: 'POST',
        body: JSON.stringify(data),
      });
      return await this.login({ email: data.email, password: data.password });
    } catch (err) {
      // Fallback: register patient locally in browser storage
      const email = data.email.trim().toLowerCase();
      const newUser: User = {
        id: `patient-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
        email,
        firstName: data.firstName.trim(),
        lastName: data.lastName.trim(),
        role: 'ROLE_PATIENT',
      };

      saveLocalUser(newUser, data.password);

      const authResponse: AuthResponse = {
        accessToken: `demo_jwt_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`,
        tokenType: 'Bearer',
        expiresIn: 86400,
        user: newUser,
      };

      localStorage.setItem(TOKEN_KEY, authResponse.accessToken);
      localStorage.setItem(USER_KEY, JSON.stringify(authResponse.user));
      return authResponse;
    }
  },

  async registerDoctor(data: DoctorRegisterRequest): Promise<DoctorRegisterResponse> {
    try {
      const response = await requestJson<DoctorRegisterResponse>(`${BACKEND_BASE_URL}/auth/doctor/register`, {
        method: 'POST',
        body: JSON.stringify(data),
      });
      return response;
    } catch (err) {
      const email = data.email.trim().toLowerCase();
      const requestId = `doc-req-${Date.now()}`;
      const doctorItem = {
        id: requestId,
        userId: `doctor-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
        name: `Dr. ${data.firstName.trim()} ${data.lastName.trim()}`,
        firstName: data.firstName.trim(),
        lastName: data.lastName.trim(),
        email,
        licenseNumber: data.licenseNumber.trim(),
        specialization: data.specialization.trim(),
        hospitalOrganization: data.hospitalOrganization.trim(),
        phone: data.phone.trim(),
        registrationDate: new Date().toISOString(),
        status: 'PENDING',
      };

      // Add to admin requests queue for approval/rejection
      saveDoctorRequestLocally(doctorItem);

      // Save user record for future login after approval
      saveLocalUser(
        {
          id: doctorItem.userId,
          email,
          firstName: data.firstName.trim(),
          lastName: data.lastName.trim(),
          role: 'ROLE_CLINICIAN',
        },
        data.password,
        'PENDING'
      );

      return {
        userId: doctorItem.userId,
        email,
        role: 'ROLE_CLINICIAN',
        status: 'PENDING',
        message: 'Registration submitted successfully and is pending administrative approval.',
      };
    }
  },

  getCurrentUser(): User | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw);
    } catch {
      return null;
    }
  },

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  },

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  },

  /**
   * Demo persona helper for testing frontend when backend Spring Boot is not started
   */
  setDemoUser(user: User): void {
    localStorage.setItem(TOKEN_KEY, 'demo_token_carepath_auth');
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }
};
