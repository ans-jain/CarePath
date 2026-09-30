import React, { createContext, useContext, useState, useEffect } from 'react';
import { User, LoginRequest, RegisterRequest } from '../types/auth';
import { authService } from '../services/authService';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => void;
  switchDemoPersona: (role: 'PATIENT' | 'CLINICIAN' | 'ADMIN') => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  useEffect(() => {
    const existing = authService.getCurrentUser();
    if (existing) {
      setUser(existing);
    } else {
      setUser(null);
    }
    setIsLoading(false);
  }, []);

  const login = async (credentials: LoginRequest) => {
    const res = await authService.login(credentials);
    setUser(res.user);
  };

  const register = async (data: RegisterRequest) => {
    const res = await authService.register(data);
    setUser(res.user);
  };

  const logout = () => {
    authService.logout();
    setUser(null);
  };

  const switchDemoPersona = (role: 'PATIENT' | 'CLINICIAN' | 'ADMIN') => {
    let newUser: User;
    if (role === 'PATIENT') {
      newUser = {
        id: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
        email: 'patient.sarah@carepath.io',
        firstName: 'Sarah',
        lastName: 'Jenkins',
        role: 'ROLE_PATIENT'
      };
    } else if (role === 'CLINICIAN') {
      newUser = {
        id: '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d',
        email: 'dr.marcus@carepath.io',
        firstName: 'Marcus',
        lastName: 'Vance',
        role: 'ROLE_CLINICIAN'
      };
    } else {
      newUser = {
        id: '00000000-0000-0000-0000-000000000001',
        email: 'admin@carepath.io',
        firstName: 'System',
        lastName: 'Admin',
        role: 'ROLE_ADMIN'
      };
    }
    authService.setDemoUser(newUser);
    setUser(newUser);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        register,
        logout,
        switchDemoPersona,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
