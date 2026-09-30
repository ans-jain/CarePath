import React from 'react';
import { Activity, Shield, Stethoscope, User, HelpCircle, LogOut } from 'lucide-react';
import { SystemHealthBadge } from '../dashboard/SystemHealthBadge';
import { NotificationDropdown } from '../notifications/NotificationDropdown';
import { useAuth } from '../../context/AuthContext';

interface NavbarProps {
  currentTab: 'assessment' | 'dashboard' | 'admin';
  onTabChange: (tab: 'assessment' | 'dashboard' | 'admin') => void;
  onOpenHowItWorks: () => void;
  onOpenPreferences: () => void;
  onLogout?: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  currentTab,
  onTabChange,
  onOpenHowItWorks,
  onOpenPreferences,
  onLogout,
}) => {
  const { user, logout } = useAuth();

  const handleLogout = () => {
    if (onLogout) {
      onLogout();
    } else {
      logout();
    }
  };

  const isPatient = user?.role === 'ROLE_PATIENT';
  const isDoctor = user?.role === 'ROLE_CLINICIAN' || user?.role === 'ROLE_DOCTOR';
  const isAdmin = user?.role === 'ROLE_ADMIN';

  return (
    <header className="sticky top-0 z-30 bg-white/95 backdrop-blur-md border-b border-slate-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo & Brand */}
          <div className="flex items-center gap-3">
            <div className="flex items-center justify-center w-10 h-10 rounded-lg bg-clinical-600 text-white shadow-sm">
              <Activity className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-lg font-bold tracking-tight text-slate-900">CarePath</span>
                <span className="px-1.5 py-0.5 text-[10px] font-semibold bg-clinical-100 text-clinical-800 rounded">
                  Clinical AI
                </span>
              </div>
              <p className="text-[11px] text-slate-500 font-medium">Cardiometabolic Risk & Explainability Platform</p>
            </div>
          </div>

          {/* Navigation Links - Role-Scoped */}
          <nav className="hidden md:flex items-center gap-1" aria-label="Main Navigation">
            {isPatient && (
              <button
                onClick={() => onTabChange('assessment')}
                className={`px-3.5 py-2 rounded-md text-sm font-medium transition-colors ${
                  currentTab === 'assessment'
                    ? 'bg-slate-100 text-slate-900 shadow-xs'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                }`}
              >
                Risk Assessment
              </button>
            )}
            {isDoctor && (
              <button
                onClick={() => onTabChange('dashboard')}
                className={`px-3.5 py-2 rounded-md text-sm font-medium transition-colors ${
                  currentTab === 'dashboard'
                    ? 'bg-slate-100 text-slate-900 shadow-xs'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                }`}
              >
                Dashboard / Shared Reports
              </button>
            )}
            {isAdmin && (
              <button
                onClick={() => onTabChange('admin')}
                className={`inline-flex items-center gap-1.5 px-3.5 py-2 rounded-md text-sm font-medium transition-colors ${
                  currentTab === 'admin'
                    ? 'bg-slate-900 text-white shadow-xs'
                    : 'text-slate-700 hover:text-slate-900 hover:bg-slate-100'
                }`}
              >
                <Shield className="w-4 h-4 text-emerald-500" />
                <span>Security & Audit</span>
              </button>
            )}
            {!isDoctor && (
              <button
                onClick={onOpenHowItWorks}
                className="inline-flex items-center gap-1.5 px-3 py-2 text-sm font-medium text-slate-600 hover:text-slate-900 hover:bg-slate-50 rounded-md transition-colors"
              >
                <HelpCircle className="w-4 h-4 text-slate-400" />
                <span>How It Works</span>
              </button>
            )}
          </nav>

          {/* Health status, Notifications & User Session */}
          <div className="flex items-center gap-2.5">
            <SystemHealthBadge />
            <NotificationDropdown onOpenPreferences={onOpenPreferences} />

            {/* Authenticated User Info & Sign Out (No role switching tabs) */}
            {user && (
              <div className="flex items-center gap-3 pl-3 border-l border-slate-200">
                <div className="hidden sm:flex flex-col text-right">
                  <span className="text-xs font-semibold text-slate-800">
                    {user.firstName} {user.lastName}
                  </span>
                  <span className="text-[10px] font-medium text-slate-500">
                    {isAdmin ? 'Administrator' : isDoctor ? 'Doctor' : 'Patient'}
                  </span>
                </div>
                <button
                  type="button"
                  onClick={handleLogout}
                  className="inline-flex items-center gap-1.5 px-2.5 py-1.5 text-xs font-medium text-slate-600 hover:text-rose-700 hover:bg-rose-50 rounded-lg transition-colors border border-transparent hover:border-rose-200"
                  title="Sign Out"
                  aria-label="Sign Out"
                >
                  <LogOut className="w-3.5 h-3.5 text-slate-500 hover:text-rose-600" />
                  <span className="hidden sm:inline">Sign Out</span>
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
};
