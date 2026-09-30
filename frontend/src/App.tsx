import React, { useState, useEffect } from 'react';
import { useAuth } from './context/AuthContext';
import { Navbar } from './components/common/Navbar';
import { RiskAssessmentPage } from './pages/RiskAssessmentPage';
import { DashboardPage } from './pages/DashboardPage';
import { AdminAuditLogPage } from './pages/AdminAuditLogPage';
import { AuthEntryPage } from './pages/AuthEntryPage';
import { PatientAuthPage } from './pages/PatientAuthPage';
import { DoctorAuthPage } from './pages/DoctorAuthPage';
import { AdminAuthPage } from './pages/AdminAuthPage';
import { HowItWorksModal } from './components/assessment/HowItWorksModal';
import { NotificationPreferencesModal } from './components/notifications/NotificationPreferencesModal';
import { ShieldCheck, ShieldAlert, Activity } from 'lucide-react';

export const App: React.FC = () => {
  const { isAuthenticated, isLoading, user, logout } = useAuth();
  
  // URL routing state
  const [currentPath, setCurrentPath] = useState<string>(() => {
    if (typeof window !== 'undefined' && window.location.pathname) {
      return window.location.pathname;
    }
    return '/';
  });

  const [doctorAuthMode, setDoctorAuthMode] = useState<'login' | 'register'>('login');
  const [patientAuthMode, setPatientAuthMode] = useState<'login' | 'register'>('login');
  const [currentTab, setCurrentTab] = useState<'assessment' | 'dashboard' | 'admin'>('assessment');
  const [isHowItWorksOpen, setIsHowItWorksOpen] = useState<boolean>(false);
  const [isPreferencesOpen, setIsPreferencesOpen] = useState<boolean>(false);

  // Sync browser back/forward buttons
  useEffect(() => {
    const handlePopState = () => {
      setCurrentPath(window.location.pathname);
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  const navigateTo = (path: string) => {
    if (typeof window !== 'undefined') {
      window.history.pushState({}, '', path);
    }
    setCurrentPath(path);
  };

  const handleLogout = () => {
    logout();
    navigateTo('/');
  };

  // Synchronize initial authenticated dashboard based on user role when on root '/'
  useEffect(() => {
    if (isAuthenticated && user && (currentPath === '/' || currentPath === '')) {
      if (user.role === 'ROLE_PATIENT') {
        setCurrentTab('assessment');
        navigateTo('/patient/dashboard');
      } else if (user.role === 'ROLE_CLINICIAN' || user.role === 'ROLE_DOCTOR') {
        setCurrentTab('dashboard');
        navigateTo('/doctor/dashboard');
      } else if (user.role === 'ROLE_ADMIN') {
        setCurrentTab('admin');
        navigateTo('/admin/dashboard');
      }
    }
  }, [isAuthenticated, user, currentPath]);

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50">
        <div className="flex flex-col items-center gap-3">
          <Activity className="w-8 h-8 text-clinical-600 animate-spin" />
          <span className="text-sm font-medium text-slate-600">Initializing CarePath Clinical Platform...</span>
        </div>
      </div>
    );
  }

  // Handle Unauthenticated routes
  if (!isAuthenticated) {
    if (currentPath === '/auth/patient') {
      return (
        <div className="min-h-screen bg-slate-50 flex flex-col justify-between">
          <main className="flex-1 flex items-center justify-center">
            <PatientAuthPage
              initialMode={patientAuthMode}
              onBackToRoles={() => navigateTo('/')}
              onAuthSuccess={() => navigateTo('/patient/dashboard')}
            />
          </main>
          <footer className="py-4 text-center text-xs text-slate-400 border-t border-slate-200 bg-white">
            CarePath Health Platform &bull; Non-diagnostic Clinical Decision Support
          </footer>
        </div>
      );
    }

    if (currentPath === '/auth/doctor') {
      return (
        <div className="min-h-screen bg-slate-50 flex flex-col justify-between">
          <main className="flex-1 flex items-center justify-center">
            <DoctorAuthPage
              initialMode={doctorAuthMode}
              onBackToRoles={() => navigateTo('/')}
              onAuthSuccess={() => navigateTo('/doctor/dashboard')}
            />
          </main>
          <footer className="py-4 text-center text-xs text-slate-400 border-t border-slate-200 bg-white">
            CarePath Health Platform &bull; Non-diagnostic Clinical Decision Support
          </footer>
        </div>
      );
    }

    if (currentPath === '/auth/admin') {
      return (
        <div className="min-h-screen bg-slate-50 flex flex-col justify-between">
          <main className="flex-1 flex items-center justify-center">
            <AdminAuthPage
              onBackToRoles={() => navigateTo('/')}
              onAuthSuccess={() => navigateTo('/admin/dashboard')}
            />
          </main>
          <footer className="py-4 text-center text-xs text-slate-400 border-t border-slate-200 bg-white">
            CarePath Health Platform &bull; Non-diagnostic Clinical Decision Support
          </footer>
        </div>
      );
    }

    // Default unauthenticated entry: Role Selection page (/ or /auth or any protected route attempted without login)
    return (
      <div className="min-h-screen bg-slate-50 flex flex-col justify-between">
        <main className="flex-1 flex items-center justify-center">
          <AuthEntryPage
            onSelectRole={(role, mode) => {
              if (role === 'doctor') {
                setDoctorAuthMode(mode === 'register' ? 'register' : 'login');
              } else if (role === 'patient') {
                setPatientAuthMode(mode === 'register' ? 'register' : 'login');
              } else {
                setDoctorAuthMode('login');
                setPatientAuthMode('login');
              }
              navigateTo(`/auth/${role}`);
            }}
          />
        </main>
        <footer className="py-4 text-center text-xs text-slate-400 border-t border-slate-200 bg-white">
          CarePath Health Platform &bull; Non-diagnostic Clinical Decision Support &bull; Calibrated ML v1.0.0
        </footer>
      </div>
    );
  }

  // --- Role Protection Guard for Authenticated Users ---
  const isPatient = user?.role === 'ROLE_PATIENT';
  const isDoctor = user?.role === 'ROLE_CLINICIAN' || user?.role === 'ROLE_DOCTOR';
  const isAdmin = user?.role === 'ROLE_ADMIN';

  // Check forbidden direct URL access
  let accessDenied = false;
  let homeRoute = isPatient ? '/patient/dashboard' : isDoctor ? '/doctor/dashboard' : '/admin/dashboard';

  if (currentPath === '/doctor/dashboard' && isPatient) {
    accessDenied = true;
  } else if (currentPath === '/admin/dashboard' && !isAdmin) {
    accessDenied = true;
  } else if (currentPath === '/patient/dashboard' && isDoctor) {
    accessDenied = true;
  }

  // If user visits /auth while logged in, allow them to view role selection or continue to dashboard
  if (currentPath === '/auth') {
    return (
      <div className="min-h-screen bg-slate-50 flex flex-col justify-between">
        <main className="flex-1 flex items-center justify-center">
          <AuthEntryPage
            onSelectRole={(role, mode) => {
              if (role === 'doctor' && mode === 'register') {
                setDoctorAuthMode('register');
              } else {
                setDoctorAuthMode('login');
              }
              navigateTo(`/auth/${role}`);
            }}
          />
        </main>
        <footer className="py-4 text-center text-xs text-slate-400 border-t border-slate-200 bg-white">
          Logged in as <span className="font-semibold text-slate-700">{user?.email}</span> ({user?.role}) &bull;{' '}
          <button
            onClick={() => navigateTo(homeRoute)}
            className="text-clinical-600 font-bold hover:underline"
          >
            Return to Dashboard
          </button>
        </footer>
      </div>
    );
  }

  // Render Access Denied Guard if forbidden access attempted
  if (accessDenied) {
    return (
      <div className="min-h-screen bg-slate-50 flex flex-col justify-between selection:bg-clinical-500 selection:text-white">
        <Navbar
          currentTab={currentTab}
          onTabChange={(tab) => {
            if (tab === 'assessment' && isPatient) {
              setCurrentTab('assessment');
              navigateTo('/patient/dashboard');
            } else if (tab === 'dashboard' && isDoctor) {
              setCurrentTab('dashboard');
              navigateTo('/doctor/dashboard');
            } else if (tab === 'admin' && isAdmin) {
              setCurrentTab('admin');
              navigateTo('/admin/dashboard');
            }
          }}
          onOpenHowItWorks={() => setIsHowItWorksOpen(true)}
          onOpenPreferences={() => setIsPreferencesOpen(true)}
          onLogout={handleLogout}
        />
        <main className="flex-1 flex items-center justify-center p-4">
          <div className="max-w-md w-full bg-white rounded-2xl border border-rose-200 p-8 shadow-sm text-center">
            <div className="w-16 h-16 bg-rose-50 border border-rose-200 text-rose-600 rounded-full flex items-center justify-center mx-auto mb-4">
              <ShieldAlert className="w-8 h-8" />
            </div>
            <h2 className="text-xl font-bold text-slate-900 mb-2">Access Denied</h2>
            <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-800 text-sm font-semibold mb-4">
              You do not have permission to access this area.
            </div>
            <p className="text-xs text-slate-500 mb-6 leading-relaxed">
              Your account role (<span className="font-mono font-semibold text-slate-700">{user?.role}</span>) does not have authorization to view this clinical domain.
            </p>
            <button
              onClick={() => {
                if (isPatient) {
                  setCurrentTab('assessment');
                  navigateTo('/patient/dashboard');
                } else if (isDoctor) {
                  setCurrentTab('dashboard');
                  navigateTo('/doctor/dashboard');
                } else {
                  setCurrentTab('admin');
                  navigateTo('/admin/dashboard');
                }
              }}
              className="w-full py-2.5 px-4 text-xs font-bold text-white bg-slate-900 hover:bg-slate-800 rounded-lg transition-colors shadow-xs min-h-[44px]"
            >
              Return to Your Dashboard
            </button>
          </div>
        </main>
        {/* Educational Explainability Modal */}
        <HowItWorksModal
          isOpen={isHowItWorksOpen}
          onClose={() => setIsHowItWorksOpen(false)}
        />
        {/* User Notification Preferences Modal */}
        <NotificationPreferencesModal
          isOpen={isPreferencesOpen}
          onClose={() => setIsPreferencesOpen(false)}
        />
        <footer className="py-4 text-center text-xs text-slate-400 border-t border-slate-200 bg-white">
          CarePath Health Platform &bull; Non-diagnostic Clinical Decision Support &bull; RBAC Protection Active
        </footer>
      </div>
    );
  }

  // Determine active view based strictly on role authorization and path/tab
  const showAdmin = isAdmin && (currentPath === '/admin/dashboard' || currentTab === 'admin');
  const showDoctor = isDoctor && (currentPath === '/doctor/dashboard' || currentTab === 'dashboard');
  const showPatient = isPatient && (currentPath === '/patient/dashboard' || currentTab === 'assessment');

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-between selection:bg-clinical-500 selection:text-white">
      {/* Sticky Top Header Navigation */}
      <Navbar
        currentTab={currentTab}
        onTabChange={(tab) => {
          if (tab === 'admin' && isAdmin) {
            setCurrentTab('admin');
            navigateTo('/admin/dashboard');
          } else if (tab === 'dashboard' && isDoctor) {
            setCurrentTab('dashboard');
            navigateTo('/doctor/dashboard');
          } else if (tab === 'assessment' && isPatient) {
            setCurrentTab('assessment');
            navigateTo('/patient/dashboard');
          }
        }}
        onOpenHowItWorks={() => setIsHowItWorksOpen(true)}
        onOpenPreferences={() => setIsPreferencesOpen(true)}
        onLogout={handleLogout}
      />

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 pt-8">
        {showAdmin ? (
          <AdminAuditLogPage />
        ) : showDoctor ? (
          <DashboardPage
            onStartAssessment={() => {
              if (isPatient) {
                setCurrentTab('assessment');
                navigateTo('/patient/dashboard');
              } else {
                setIsHowItWorksOpen(true);
              }
            }}
            onOpenHowItWorks={() => setIsHowItWorksOpen(true)}
          />
        ) : (
          <RiskAssessmentPage onOpenHowItWorks={() => setIsHowItWorksOpen(true)} />
        )}
      </main>

      {/* Educational Explainability Modal */}
      <HowItWorksModal
        isOpen={isHowItWorksOpen}
        onClose={() => setIsHowItWorksOpen(false)}
      />

      {/* User Notification Preferences Modal */}
      <NotificationPreferencesModal
        isOpen={isPreferencesOpen}
        onClose={() => setIsPreferencesOpen(false)}
      />

      {/* Clinical Footer */}
      <footer className="mt-12 bg-white border-t border-slate-200 py-6 text-xs text-slate-500">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <ShieldCheck className="w-4 h-4 text-emerald-600" />
            <span className="font-semibold text-slate-700">Strict Clinical Guardrail:</span>
            <span>
              Decision-support risk signals only. Does not diagnose diseases or prescribe medication.
            </span>
          </div>
          <div className="flex items-center gap-4 text-slate-400">
            <span>Model: Calibrated Gradient Boosting (TreeSHAP)</span>
            <span>&bull;</span>
            <span>&copy; {new Date().getFullYear()} CarePath Health Inc.</span>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default App;
