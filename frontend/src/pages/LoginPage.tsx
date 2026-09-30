import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { Activity, Lock, Mail, User, ShieldCheck } from 'lucide-react';

interface LoginPageProps {
  onLoginSuccess: () => void;
}

export const LoginPage: React.FC<LoginPageProps> = ({ onLoginSuccess }) => {
  const { login, register, switchDemoPersona } = useAuth();
  const [isRegister, setIsRegister] = useState(false);
  const [email, setEmail] = useState('patient.sarah@carepath.io');
  const [password, setPassword] = useState('SecurePass123!');
  const [firstName, setFirstName] = useState('Sarah');
  const [lastName, setLastName] = useState('Jenkins');
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsLoading(true);

    try {
      if (isRegister) {
        await register({
          email,
          password,
          firstName,
          lastName,
          role: 'ROLE_PATIENT',
        });
      } else {
        await login({ email, password });
      }
      onLoginSuccess();
    } catch (err: any) {
      setError(err.message || 'Authentication failed. Please verify backend status.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleQuickDemoLogin = (role: 'PATIENT' | 'CLINICIAN') => {
    switchDemoPersona(role);
    onLoginSuccess();
  };

  return (
    <div className="min-h-[80vh] flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
        {/* Brand Header */}
        <div className="p-6 bg-slate-50 border-b border-slate-100 text-center">
          <div className="inline-flex items-center justify-center w-12 h-12 rounded-xl bg-clinical-600 text-white mb-2 shadow-xs">
            <Activity className="w-7 h-7" />
          </div>
          <h1 className="text-xl font-bold text-slate-900 tracking-tight">CarePath Health Platform</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            {isRegister ? 'Register your patient portal account' : 'Sign in to access your cardiometabolic risk analysis'}
          </p>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="p-3 bg-rose-50 border border-rose-200 rounded-lg text-xs text-rose-700">
              {error}
            </div>
          )}

          {isRegister && (
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">First Name</label>
                <input
                  type="text"
                  required
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg min-h-[44px]"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">Last Name</label>
                <input
                  type="text"
                  required
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg min-h-[44px]"
                />
              </div>
            </div>
          )}

          <div>
            <label className="block text-xs font-medium text-slate-700 mb-1">Email Address</label>
            <div className="relative">
              <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-3.5" />
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 rounded-lg min-h-[44px]"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-700 mb-1">Password</label>
            <div className="relative">
              <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-3.5" />
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 rounded-lg min-h-[44px]"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={isLoading}
            className="w-full py-2.5 text-sm font-bold text-white bg-clinical-600 hover:bg-clinical-700 disabled:bg-slate-300 rounded-lg shadow-2xs transition-colors min-h-[48px]"
          >
            {isLoading ? 'Authenticating...' : (isRegister ? 'Complete Registration' : 'Sign In')}
          </button>

          <div className="text-center pt-2">
            <button
              type="button"
              onClick={() => setIsRegister(!isRegister)}
              className="text-xs text-clinical-600 hover:text-clinical-800 font-semibold"
            >
              {isRegister ? 'Already have an account? Sign in' : "Don't have an account? Register"}
            </button>
          </div>

          {/* Quick Demo Access Buttons */}
          <div className="pt-4 border-t border-slate-100">
            <span className="block text-[11px] font-semibold text-slate-400 uppercase tracking-wider text-center mb-2.5">
              Quick Demo Personas (One-Click)
            </span>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => handleQuickDemoLogin('PATIENT')}
                className="p-2 text-xs font-medium text-slate-700 bg-slate-50 hover:bg-slate-100 border border-slate-200 rounded-lg transition-colors"
              >
                Sarah (Patient)
              </button>
              <button
                type="button"
                onClick={() => handleQuickDemoLogin('CLINICIAN')}
                className="p-2 text-xs font-medium text-clinical-800 bg-clinical-50 hover:bg-clinical-100 border border-clinical-200 rounded-lg transition-colors"
              >
                Dr. Marcus (Clinician)
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};
