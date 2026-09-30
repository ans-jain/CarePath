import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { authService } from '../services/authService';
import {
  Stethoscope,
  Lock,
  Mail,
  User as UserIcon,
  Phone,
  Building2,
  FileBadge2,
  ArrowLeft,
  CheckCircle2,
  Clock,
  AlertCircle,
  XCircle,
  Briefcase
} from 'lucide-react';

interface DoctorAuthPageProps {
  initialMode?: 'login' | 'register';
  onBackToRoles: () => void;
  onAuthSuccess: () => void;
}

export const DoctorAuthPage: React.FC<DoctorAuthPageProps> = ({
  initialMode = 'login',
  onBackToRoles,
  onAuthSuccess,
}) => {
  const { login, switchDemoPersona } = useAuth();
  const [isRegister, setIsRegister] = useState<boolean>(initialMode === 'register');
  
  // Registration form fields
  const [firstName, setFirstName] = useState<string>('Marcus');
  const [lastName, setLastName] = useState<string>('Vance');
  const [email, setEmail] = useState<string>('dr.marcus@carepath.io');
  const [password, setPassword] = useState<string>('SecurePass123!');
  const [confirmPassword, setConfirmPassword] = useState<string>('SecurePass123!');
  const [medicalLicenseNumber, setMedicalLicenseNumber] = useState<string>('MD-984321');
  const [specialization, setSpecialization] = useState<string>('Cardiology');
  const [hospitalAffiliation, setHospitalAffiliation] = useState<string>('Metropolitan General Hospital');
  const [phone, setPhone] = useState<string>('+15552000002');

  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [registrationSubmitted, setRegistrationSubmitted] = useState<boolean>(false);
  const [registrationDetails, setRegistrationDetails] = useState<{
    email: string;
    doctorName: string;
    status: string;
  } | null>(null);

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
      if (!medicalLicenseNumber.trim()) {
        setError('Medical License / Registration Number is required.');
        return;
      }
      if (!specialization.trim()) {
        setError('Specialization is required.');
        return;
      }
      if (!hospitalAffiliation.trim()) {
        setError('Hospital or Organization affiliation is required.');
        return;
      }
      if (!phone.trim()) {
        setError('Phone number is required.');
        return;
      }

      setIsLoading(true);
      try {
        const response = await authService.registerDoctor({
          email: email.trim().toLowerCase(),
          password,
          firstName: firstName.trim(),
          lastName: lastName.trim(),
          phone: phone.trim(),
          licenseNumber: medicalLicenseNumber.trim(),
          specialization: specialization.trim(),
          hospitalOrganization: hospitalAffiliation.trim(),
        });

        setRegistrationDetails({
          email: response.email,
          doctorName: `Dr. ${firstName.trim()} ${lastName.trim()}`,
          status: response.status,
        });
        setRegistrationSubmitted(true);
      } catch (err: any) {
        setError(err.message || 'Doctor registration failed. Please review your information.');
      } finally {
        setIsLoading(false);
      }
    } else {
      // Login flow
      setIsLoading(true);
      try {
        await login({ email: email.trim().toLowerCase(), password });
        onAuthSuccess();
      } catch (err: any) {
        // Backend returns exact clinical messages for PENDING and REJECTED
        const errMsg = err.message || '';
        if (errMsg.includes('pending admin approval') || errMsg.includes('DOCTOR_PENDING_APPROVAL')) {
          setError('Your doctor registration is still pending admin approval. You will be able to access your account once an administrator approves your registration.');
        } else if (errMsg.includes('not approved') || errMsg.includes('DOCTOR_REGISTRATION_REJECTED')) {
          setError('Your doctor registration request was not approved.');
        } else {
          setError(errMsg || 'Invalid doctor credentials. Please verify your email and password.');
        }
      } finally {
        setIsLoading(false);
      }
    }
  };

  const handleDemoDoctorLogin = () => {
    switchDemoPersona('CLINICIAN');
    onAuthSuccess();
  };

  return (
    <div className="min-h-[85vh] flex items-center justify-center p-4">
      <div className="w-full max-w-lg bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
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
            <div className="w-10 h-10 rounded-xl bg-clinical-600 text-white flex items-center justify-center shadow-xs">
              <Stethoscope className="w-5 h-5" />
            </div>
            <div>
              <h1 className="text-lg font-bold text-slate-900">Clinician & Doctor Portal</h1>
              <p className="text-xs text-slate-500">
                {isRegister
                  ? 'Submit clinical credentials for administrative verification'
                  : 'Sign in to review patient risk trajectories & explainability models'}
              </p>
            </div>
          </div>

          {!registrationSubmitted && (
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
                Doctor Sign In
              </button>
              <button
                type="button"
                onClick={() => {
                  setIsRegister(true);
                  setError(null);
                }}
                className={`py-1.5 text-xs font-bold rounded-md transition-all ${
                  isRegister ? 'bg-white text-clinical-900 shadow-xs' : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                Register as Doctor
              </button>
            </div>
          )}
        </div>

        {/* Content View */}
        {registrationSubmitted ? (
          <div className="p-8 text-center space-y-6">
            <div className="w-16 h-16 bg-amber-50 border-2 border-amber-200 text-amber-600 rounded-full flex items-center justify-center mx-auto shadow-xs">
              <Clock className="w-8 h-8 animate-pulse" />
            </div>

            <div className="space-y-2">
              <h2 className="text-xl font-bold text-slate-900">Registration Submitted</h2>
              <p className="text-xs font-medium text-amber-700 bg-amber-50 py-1.5 px-3 rounded-full inline-block border border-amber-200">
                Status: PENDING ADMIN APPROVAL
              </p>
              <div className="p-4 bg-slate-50 border border-slate-200 rounded-xl text-left text-xs text-slate-600 space-y-2 mt-4">
                <p className="font-semibold text-slate-800">
                  Your registration request has been submitted for admin approval. You will be able to access your account once an administrator approves your registration.
                </p>
                <div className="pt-2 border-t border-slate-200 grid grid-cols-2 gap-2 text-[11px]">
                  <div>
                    <span className="text-slate-400 block">Doctor:</span>
                    <span className="font-semibold text-slate-700">{registrationDetails?.doctorName}</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block">Email:</span>
                    <span className="font-semibold text-slate-700">{registrationDetails?.email}</span>
                  </div>
                </div>
              </div>
            </div>

            <div className="flex flex-col sm:flex-row items-center gap-3 pt-2">
              <button
                type="button"
                onClick={() => {
                  setRegistrationSubmitted(false);
                  setIsRegister(false);
                  setError(null);
                }}
                className="w-full py-2.5 px-4 text-xs font-bold text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-lg transition-colors min-h-[44px]"
              >
                Return to Doctor Sign In
              </button>
              <button
                type="button"
                onClick={onBackToRoles}
                className="w-full py-2.5 px-4 text-xs font-bold text-white bg-clinical-600 hover:bg-clinical-700 rounded-lg transition-colors min-h-[44px]"
              >
                Back to Role Selection
              </button>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="p-6 space-y-4">
            {error && (
              <div className="p-3.5 bg-rose-50 border border-rose-200 rounded-xl text-xs text-rose-800 flex items-start gap-2.5">
                <AlertCircle className="w-4 h-4 text-rose-600 mt-0.5 shrink-0" />
                <div className="leading-relaxed font-medium">{error}</div>
              </div>
            )}

            {isRegister && (
              <>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-medium text-slate-700 mb-1">First Name *</label>
                    <div className="relative">
                      <UserIcon className="w-4 h-4 text-slate-400 absolute left-3 top-3.5" />
                      <input
                        type="text"
                        required
                        value={firstName}
                        onChange={(e) => setFirstName(e.target.value)}
                        className="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 rounded-lg min-h-[44px] focus:ring-1 focus:ring-clinical-500"
                        placeholder="Marcus"
                      />
                    </div>
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-slate-700 mb-1">Last Name *</label>
                    <div className="relative">
                      <UserIcon className="w-4 h-4 text-slate-400 absolute left-3 top-3.5" />
                      <input
                        type="text"
                        required
                        value={lastName}
                        onChange={(e) => setLastName(e.target.value)}
                        className="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 rounded-lg min-h-[44px] focus:ring-1 focus:ring-clinical-500"
                        placeholder="Vance"
                      />
                    </div>
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-medium text-slate-700 mb-1">
                      Medical License / Reg No. *
                    </label>
                    <div className="relative">
                      <FileBadge2 className="w-4 h-4 text-slate-400 absolute left-3 top-3.5" />
                      <input
                        type="text"
                        required
                        value={medicalLicenseNumber}
                        onChange={(e) => setMedicalLicenseNumber(e.target.value)}
                        className="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 rounded-lg min-h-[44px] focus:ring-1 focus:ring-clinical-500 font-mono text-xs"
                        placeholder="MD-123456"
                      />
                    </div>
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-slate-700 mb-1">Specialization *</label>
                    <div className="relative">
                      <Briefcase className="w-4 h-4 text-slate-400 absolute left-3 top-3.5" />
                      <input
                        type="text"
                        required
                        value={specialization}
                        onChange={(e) => setSpecialization(e.target.value)}
                        className="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 rounded-lg min-h-[44px] focus:ring-1 focus:ring-clinical-500"
                        placeholder="Cardiology / Internal Med"
                      />
                    </div>
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-medium text-slate-700 mb-1">Hospital / Organization *</label>
                    <div className="relative">
                      <Building2 className="w-4 h-4 text-slate-400 absolute left-3 top-3.5" />
                      <input
                        type="text"
                        required
                        value={hospitalAffiliation}
                        onChange={(e) => setHospitalAffiliation(e.target.value)}
                        className="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 rounded-lg min-h-[44px] focus:ring-1 focus:ring-clinical-500"
                        placeholder="City General Hospital"
                      />
                    </div>
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-slate-700 mb-1">Phone Number *</label>
                    <div className="relative">
                      <Phone className="w-4 h-4 text-slate-400 absolute left-3 top-3.5" />
                      <input
                        type="tel"
                        required
                        value={phone}
                        onChange={(e) => setPhone(e.target.value)}
                        className="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 rounded-lg min-h-[44px] focus:ring-1 focus:ring-clinical-500"
                        placeholder="+1 (555) 000-0000"
                      />
                    </div>
                  </div>
                </div>
              </>
            )}

            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">Clinical Email Address *</label>
              <div className="relative">
                <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-3.5" />
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 rounded-lg min-h-[44px] focus:ring-1 focus:ring-clinical-500"
                  placeholder="doctor@hospital.org"
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
                  className="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 rounded-lg min-h-[44px] focus:ring-1 focus:ring-clinical-500"
                  placeholder="••••••••"
                />
              </div>
              {isRegister && (
                <p className="text-[11px] text-slate-500 mt-1">
                  At least 8 characters with 1 uppercase, 1 lowercase, 1 digit, and 1 special symbol (e.g. <span className="font-mono text-slate-700">SecurePass123!</span>).
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
                    className="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 rounded-lg min-h-[44px] focus:ring-1 focus:ring-clinical-500"
                    placeholder="••••••••"
                  />
                </div>
              </div>
            )}

            {isRegister && (
              <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg text-xs text-slate-600 flex items-start gap-2">
                <Clock className="w-4 h-4 text-amber-500 shrink-0 mt-0.5" />
                <span>
                  Doctor accounts require administrative review and credential verification before dashboard access is unlocked.
                </span>
              </div>
            )}

            <button
              type="submit"
              disabled={isLoading}
              className="w-full py-2.5 text-sm font-bold text-white bg-clinical-600 hover:bg-clinical-700 disabled:bg-slate-300 rounded-lg shadow-sm transition-colors min-h-[48px]"
            >
              {isLoading
                ? 'Processing...'
                : isRegister
                ? 'Submit Doctor Registration for Approval'
                : 'Sign In to Doctor Dashboard'}
            </button>

            {/* Quick Demo Access for Dr. Marcus */}
            <div className="pt-4 border-t border-slate-100">
              <span className="block text-[11px] font-semibold text-slate-400 uppercase tracking-wider text-center mb-2">
                Instant Clinician Testing
              </span>
              <button
                type="button"
                onClick={handleDemoDoctorLogin}
                className="w-full p-2.5 text-xs font-medium text-clinical-900 bg-clinical-50 hover:bg-clinical-100 border border-clinical-200 rounded-lg transition-colors flex items-center justify-center gap-1.5"
              >
                <CheckCircle2 className="w-3.5 h-3.5 text-clinical-600" />
                <span>Continue as Dr. Marcus (Active Demo Doctor)</span>
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};
