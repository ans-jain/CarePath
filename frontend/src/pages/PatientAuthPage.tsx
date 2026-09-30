import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { Heart, Lock, Mail, User as UserIcon, ArrowLeft, CheckCircle2, ShieldCheck } from 'lucide-react';

interface PatientAuthPageProps {
  initialMode?: 'login' | 'register';
  onBackToRoles: () => void;
  onAuthSuccess: () => void;
}

export const PatientAuthPage: React.FC<PatientAuthPageProps> = ({
  initialMode = 'login',
  onBackToRoles,
  onAuthSuccess,
}) => {
  const { login, register, switchDemoPersona } = useAuth();
  const [isRegister, setIsRegister] = useState<boolean>(initialMode === 'register');
  const [email, setEmail] = useState<string>('patient.sarah@carepath.io');
  const [password, setPassword] = useState<string>('SecurePass123!');
  const [confirmPassword, setConfirmPassword] = useState<string>('SecurePass123!');
  const [firstName, setFirstName] = useState<string>('Sarah');
  const [lastName, setLastName] = useState<string>('Jenkins');
  const [phone, setPhone] = useState<string>('+15553000003');
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (isRegister) {
      if (password !== confirmPassword) {
        setError('Passwords do not match.');
        return;
      }
      if (password.length < 8) {
        setError('Password must be at least 8 characters long.');
        return;
      }
    }

    setIsLoading(true);

    try {
      if (isRegister) {
        await register({
          email: email.trim().toLowerCase(),
          password,
          firstName: firstName.trim(),
          lastName: lastName.trim(),
          phone: phone.trim() || undefined,
          role: 'ROLE_PATIENT',
        });
      } else {
        await login({ email: email.trim().toLowerCase(), password });
      }
      onAuthSuccess();
    } catch (err: any) {
      setError(err.message || 'Patient authentication failed. Please verify your details.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleDemoPatientLogin = () => {
    switchDemoPersona('PATIENT');
    onAuthSuccess();
  };

  return (
    <div className="min-h-[85vh] flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
        {/* Navigation & Header */}
        <div className="p-6 bg-slate-50 border-b border-slate-100">
          <button
            type="button"
            onClick={onBackToRoles}
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-slate-800 transition-colors mb-4"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Back to Role Selection</span>
          </button>

          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-emerald-500 text-white flex items-center justify-center shadow-xs">
              <Heart className="w-5 h-5" />
            </div>
            <div>
              <h1 className="text-lg font-bold text-slate-900">Patient Health Portal</h1>
              <p className="text-xs text-slate-500">
                {isRegister ? 'Create your patient account (Active Immediately)' : 'Sign in to access your risk predictions'}
              </p>
            </div>
          </div>

          {/* Tab Selector */}
          <div className="grid grid-cols-2 gap-1 bg-slate-200/70 p-1 rounded-lg mt-5">
            <button
              type="button"
              onClick={() => {
                setIsRegister(false);
                setError(null);
              }}
              className={`py-1.5 text-xs font-bold rounded-md transition-all ${
                !isRegister ? 'bg-white text-slate-900 shadow-xs' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              Sign In
            </button>
            <button
              type="button"
              onClick={() => {
                setIsRegister(true);
                setError(null);
              }}
              className={`py-1.5 text-xs font-bold rounded-md transition-all ${
                isRegister ? 'bg-white text-emerald-800 shadow-xs' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              Register New Patient
            </button>
          </div>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="p-3 bg-rose-50 border border-rose-200 rounded-lg text-xs text-rose-700">
              {error}
            </div>
          )}

          {isRegister && (
            <>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-slate-700 mb-1">First Name *</label>
                  <input
                    type="text"
                    required
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg min-h-[44px] focus:ring-1 focus:ring-emerald-500"
                    placeholder="Sarah"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-700 mb-1">Last Name *</label>
                  <input
                    type="text"
                    required
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg min-h-[44px] focus:ring-1 focus:ring-emerald-500"
                    placeholder="Jenkins"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">Phone Number</label>
                <input
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg min-h-[44px] focus:ring-1 focus:ring-emerald-500"
                  placeholder="+1 (555) 000-0000"
                />
              </div>
            </>
          )}

          <div>
            <label className="block text-xs font-medium text-slate-700 mb-1">Email Address *</label>
            <div className="relative">
              <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-3.5" />
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 rounded-lg min-h-[44px] focus:ring-1 focus:ring-emerald-500"
                placeholder="patient@carepath.io"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-700 mb-1">Password *</label>
            <div className="relative">
              <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-3.5" />
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 rounded-lg min-h-[44px] focus:ring-1 focus:ring-emerald-500"
                placeholder="••••••••"
              />
            </div>
            {isRegister && (
              <p className="text-[11px] text-slate-500 mt-1">
                Must be at least 8 characters with 1 uppercase, 1 lowercase, 1 number, and 1 special character (e.g. <span className="font-mono text-slate-700">SecurePass123!</span>).
              </p>
            )}
          </div>

          {isRegister && (
            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">Confirm Password *</label>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-3.5" />
                <input
                  type="password"
                  required
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  className="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 rounded-lg min-h-[44px] focus:ring-1 focus:ring-emerald-500"
                  placeholder="••••••••"
                />
              </div>
            </div>
          )}

          <button
            type="submit"
            disabled={isLoading}
            className="w-full py-2.5 text-sm font-bold text-white bg-emerald-600 hover:bg-emerald-700 disabled:bg-slate-300 rounded-lg shadow-sm transition-colors min-h-[48px]"
          >
            {isLoading
              ? 'Processing...'
              : isRegister
              ? 'Complete Patient Registration'
              : 'Sign In to Patient Portal'}
          </button>

          {/* Quick Demo Access */}
          <div className="pt-4 border-t border-slate-100">
            <span className="block text-[11px] font-semibold text-slate-400 uppercase tracking-wider text-center mb-2">
              Instant Patient Testing
            </span>
            <button
              type="button"
              onClick={handleDemoPatientLogin}
              className="w-full p-2.5 text-xs font-medium text-emerald-800 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 rounded-lg transition-colors flex items-center justify-center gap-1.5"
            >
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
              <span>Continue as Sarah (Demo Patient)</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
