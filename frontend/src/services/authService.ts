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
      throw err;
    }
  },

  async register(data: RegisterRequest): Promise<AuthResponse> {
    // 1. Create patient account on backend
    await requestJson<any>(`${BACKEND_BASE_URL}/auth/register`, {
      method: 'POST',
      body: JSON.stringify(data),
    });

    // 2. Automatically log the patient in to obtain active session JWT
    return this.login({ email: data.email, password: data.password });
  },

  async registerDoctor(data: DoctorRegisterRequest): Promise<DoctorRegisterResponse> {
    const response = await requestJson<DoctorRegisterResponse>(`${BACKEND_BASE_URL}/auth/doctor/register`, {
      method: 'POST',
      body: JSON.stringify(data),
    });

    return response;
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
