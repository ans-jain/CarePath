import React from 'react';
import { Heart, Stethoscope, ShieldCheck, Activity, ArrowRight, Shield, UserPlus, LogIn } from 'lucide-react';

interface AuthEntryPageProps {
  onSelectRole: (role: 'patient' | 'doctor' | 'admin', mode?: 'login' | 'register') => void;
}

export const AuthEntryPage: React.FC<AuthEntryPageProps> = ({ onSelectRole }) => {
  return (
    <div className="min-h-[85vh] flex flex-col justify-center items-center py-10 px-4 sm:px-6 lg:px-8">
      {/* Brand Header */}
      <div className="text-center max-w-2xl mx-auto mb-10">
        <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-clinical-600 text-white mb-4 shadow-md ring-4 ring-clinical-50">
          <Activity className="w-8 h-8" />
        </div>
        <h1 className="text-3xl font-extrabold text-slate-900 tracking-tight sm:text-4xl">
          Welcome to CarePath
        </h1>
        <p className="mt-2 text-base text-slate-600 font-medium">
          Choose how you want to access the platform
        </p>
        <p className="mt-1 text-xs text-slate-400">
          Enterprise Longitudinal Cardiometabolic Risk & Decision-Support System
        </p>
      </div>

      {/* Role Selection Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 max-w-5xl w-full">
        {/* 1. Patient Card */}
        <div className="relative bg-white rounded-2xl border-2 border-slate-200 hover:border-emerald-500 hover:shadow-lg transition-all duration-200 p-7 flex flex-col justify-between group">
          <div>
            <div className="flex items-center justify-between mb-4">
              <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center group-hover:scale-105 transition-transform">
                <Heart className="w-6 h-6" />
              </div>
              <span className="text-[11px] font-bold uppercase tracking-wider px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
                Patient Portal
              </span>
            </div>
            <h2 className="text-xl font-bold text-slate-900 mb-2">Patient</h2>
            <p className="text-xs text-slate-600 leading-relaxed min-h-[48px]">
              Access your health profile, risk predictions, explanations and recommendations.
            </p>
            <ul className="mt-4 space-y-1.5 text-xs text-slate-500 border-t border-slate-100 pt-4">
              <li className="flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                Track longitudinal vitals & metrics
              </li>
              <li className="flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                Explainable risk scores & TreeSHAP
              </li>
              <li className="flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                Feasible counterfactual recommendations
              </li>
            </ul>
          </div>

          <div className="pt-6 mt-6 border-t border-slate-100 space-y-2">
            <button
              type="button"
              onClick={() => onSelectRole('patient', 'login')}
              className="w-full inline-flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl text-xs font-bold text-white bg-emerald-600 hover:bg-emerald-700 shadow-sm hover:shadow transition-all group-hover:translate-x-0.5 min-h-[42px]"
            >
              <LogIn className="w-3.5 h-3.5" />
              <span>Continue as Patient (Sign In)</span>
            </button>
            <button
              type="button"
              onClick={() => onSelectRole('patient', 'register')}
              className="w-full inline-flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl text-xs font-bold text-emerald-700 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 transition-all min-h-[42px]"
            >
              <UserPlus className="w-3.5 h-3.5" />
              <span>Register as Patient</span>
            </button>
          </div>
        </div>

        {/* 2. Doctor Card */}
        <div className="relative bg-white rounded-2xl border-2 border-slate-200 hover:border-clinical-500 hover:shadow-lg transition-all duration-200 p-7 flex flex-col justify-between group">
          <div>
            <div className="flex items-center justify-between mb-4">
              <div className="w-12 h-12 rounded-xl bg-clinical-50 text-clinical-600 flex items-center justify-center group-hover:scale-105 transition-transform">
                <Stethoscope className="w-6 h-6" />
              </div>
              <span className="text-[11px] font-bold uppercase tracking-wider px-2.5 py-1 rounded-full bg-clinical-50 text-clinical-700 border border-clinical-200">
                Clinician Portal
              </span>
            </div>
            <h2 className="text-xl font-bold text-slate-900 mb-2">Doctor</h2>
            <p className="text-xs text-slate-600 leading-relaxed min-h-[48px]">
              Review patient risk scores, analyze explainable AI factors, inspect counterfactuals, and manage high-risk cases.
            </p>
            <ul className="mt-4 space-y-1.5 text-xs text-slate-500 border-t border-slate-100 pt-4">
              <li className="flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-clinical-500" />
                Multi-patient trajectory monitoring
              </li>
              <li className="flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-clinical-500" />
                Clinical anomaly & alert management
              </li>
              <li className="flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-clinical-500" />
                Requires verified medical license
              </li>
            </ul>
          </div>

          <div className="pt-6 mt-6 border-t border-slate-100 space-y-2">
            <button
              type="button"
              onClick={() => onSelectRole('doctor', 'login')}
              className="w-full inline-flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl text-xs font-bold text-white bg-clinical-600 hover:bg-clinical-700 shadow-sm hover:shadow transition-all group-hover:translate-x-0.5 min-h-[42px]"
            >
              <LogIn className="w-3.5 h-3.5" />
              <span>Sign In as Doctor</span>
            </button>
            <button
              type="button"
              onClick={() => onSelectRole('doctor', 'register')}
              className="w-full inline-flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl text-xs font-bold text-clinical-700 bg-clinical-50 hover:bg-clinical-100 border border-clinical-200 transition-all min-h-[42px]"
            >
              <UserPlus className="w-3.5 h-3.5" />
              <span>Register as Doctor</span>
            </button>
          </div>
        </div>

        {/* 3. Admin Card */}
        <div className="relative bg-white rounded-2xl border-2 border-slate-200 hover:border-slate-800 hover:shadow-lg transition-all duration-200 p-7 flex flex-col justify-between group">
          <div>
            <div className="flex items-center justify-between mb-4">
              <div className="w-12 h-12 rounded-xl bg-slate-100 text-slate-800 flex items-center justify-center group-hover:scale-105 transition-transform">
                <ShieldCheck className="w-6 h-6" />
              </div>
              <span className="text-[11px] font-bold uppercase tracking-wider px-2.5 py-1 rounded-full bg-slate-100 text-slate-700 border border-slate-300">
                Governance
              </span>
            </div>
            <h2 className="text-xl font-bold text-slate-900 mb-2">Admin</h2>
            <p className="text-xs text-slate-600 leading-relaxed min-h-[48px]">
              Manage doctor approvals, oversee platform audit logs, monitor system health, and enforce clinical safety controls.
            </p>
            <ul className="mt-4 space-y-1.5 text-xs text-slate-500 border-t border-slate-100 pt-4">
              <li className="flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-slate-700" />
                Review & approve doctor licenses
              </li>
              <li className="flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-slate-700" />
                Immutable HIPAA/GDPR audit ledger
              </li>
              <li className="flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-slate-700" />
                Strictly restricted personnel access
              </li>
            </ul>
          </div>

          <div className="pt-6 mt-6 border-t border-slate-100">
            <button
              type="button"
              onClick={() => onSelectRole('admin', 'login')}
              className="w-full inline-flex items-center justify-center gap-2 py-3 px-4 rounded-xl text-sm font-bold text-white bg-slate-900 hover:bg-slate-800 shadow-sm hover:shadow transition-all group-hover:translate-x-0.5 min-h-[46px]"
            >
              <span>Sign In as Administrator</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      {/* Security & Disclaimer Footer */}
      <div className="mt-12 text-center text-xs text-slate-400 max-w-lg mx-auto flex items-center justify-center gap-2">
        <Shield className="w-4 h-4 text-slate-400 shrink-0" />
        <span>CarePath is a clinical decision-support platform &bull; Non-diagnostic &bull; Strict HIPAA RBAC Enforced</span>
      </div>
    </div>
  );
};
