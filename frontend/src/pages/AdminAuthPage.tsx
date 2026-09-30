import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import {
  ShieldAlert,
  Lock,
  Mail,
  ArrowLeft,
  CheckCircle2,
  AlertCircle,
  ShieldCheck,
  Server
} from 'lucide-react';

interface AdminAuthPageProps {
  onBackToRoles: () => void;
  onAuthSuccess: () => void;
}

export const AdminAuthPage: React.FC<AdminAuthPageProps> = ({
  onBackToRoles,
  onAuthSuccess,
}) => {
  const { login, switchDemoPersona } = useAuth();
  const [email, setEmail] = useState<string>('admin@carepath.io');
  const [password, setPassword] = useState<string>('AdminMaster123!');
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsLoading(true);

    try {
      await login({ email: email.trim().toLowerCase(), password });
      onAuthSuccess();
    } catch (err: any) {
      setError(err.message || 'Invalid administrator credentials. Access is logged and monitored.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleDemoAdminLogin = () => {
    switchDemoPersona('ADMIN');
    onAuthSuccess();
  };

  return (
    <div className="min-h-[85vh] flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
        {/* Navigation & Header */}
        <div className="p-6 bg-slate-900 text-white">
          <button
            type="button"
            onClick={onBackToRoles}
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-400 hover:text-white transition-colors mb-4"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Back to Role Selection</span>
          </button>

          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-purple-600 text-white flex items-center justify-center shadow-xs">
              <ShieldAlert className="w-5 h-5" />
            </div>
            <div>
              <h1 className="text-lg font-bold text-white">System Administration</h1>
              <p className="text-xs text-slate-400">
                Authorized Personnel & Clinical Oversight Only
              </p>
            </div>
          </div>
        </div>

        {/* Restricted Access Warning Banner */}
        <div className="p-4 bg-amber-50 border-b border-amber-200 text-amber-900 text-xs flex items-start gap-2.5">
          <AlertCircle className="w-4 h-4 text-amber-700 shrink-0 mt-0.5" />
          <div className="leading-relaxed">
            <span className="font-bold block">Access Restricted</span>
            Administrator access is restricted. Public registration is not permitted. All access attempts are recorded in the immutable audit log.
          </div>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="p-3 bg-rose-50 border border-rose-200 rounded-lg text-xs text-rose-700">
              {error}
            </div>
          )}

          <div>
            <label className="block text-xs font-medium text-slate-700 mb-1">Admin Email Address *</label>
            <div className="relative">
              <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-3.5" />
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 rounded-lg min-h-[44px] focus:ring-1 focus:ring-purple-600"
                placeholder="admin@carepath.io"
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
                className="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 rounded-lg min-h-[44px] focus:ring-1 focus:ring-purple-600"
                placeholder="••••••••"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={isLoading}
            className="w-full py-2.5 text-sm font-bold text-white bg-slate-900 hover:bg-slate-800 disabled:bg-slate-300 rounded-lg shadow-sm transition-colors min-h-[48px] mt-2"
          >
            {isLoading ? 'Verifying Authorization...' : 'Sign In as Administrator'}
          </button>

          {/* Quick Demo Access */}
          <div className="pt-4 border-t border-slate-100">
            <span className="block text-[11px] font-semibold text-slate-400 uppercase tracking-wider text-center mb-2">
              Instant Administrator Testing
            </span>
            <button
              type="button"
              onClick={handleDemoAdminLogin}
              className="w-full p-2.5 text-xs font-medium text-purple-900 bg-purple-50 hover:bg-purple-100 border border-purple-200 rounded-lg transition-colors flex items-center justify-center gap-1.5"
            >
              <CheckCircle2 className="w-3.5 h-3.5 text-purple-600" />
              <span>Continue as Administrator (Demo Admin)</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
