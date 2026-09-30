import React, { useState, useEffect, useCallback } from 'react';
import {
  Stethoscope,
  CheckCircle2,
  XCircle,
  Clock,
  RefreshCw,
  Search,
  FileBadge2,
  Building2,
  Phone,
  Mail,
  AlertCircle,
  ShieldCheck,
  UserCheck,
  UserX
} from 'lucide-react';
import { adminService } from '../../services/adminService';
import { DoctorRegistrationRequestItem, AccountStatus } from '../../types/admin';

export const DoctorRegistrationRequestsSection: React.FC = () => {
  const [requests, setRequests] = useState<DoctorRegistrationRequestItem[]>([]);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [actionInProgress, setActionInProgress] = useState<string | null>(null);
  const [notification, setNotification] = useState<{
    type: 'success' | 'error';
    message: string;
  } | null>(null);

  const fetchRequests = useCallback(async () => {
    setIsLoading(true);
    try {
      const data = await adminService.getDoctorRequests(
        statusFilter === 'ALL' ? undefined : (statusFilter as AccountStatus)
      );
      setRequests(data);
    } catch (err: any) {
      console.error('Failed to load doctor registration requests:', err);
    } finally {
      setIsLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => {
    fetchRequests();
  }, [fetchRequests]);

  const handleApprove = async (doctor: DoctorRegistrationRequestItem) => {
    if (!window.confirm(`Are you sure you want to APPROVE ${doctor.name}? This will activate their doctor account immediately.`)) {
      return;
    }

    setActionInProgress(doctor.userId);
    setNotification(null);
    try {
      await adminService.approveDoctorRequest(doctor.userId);
      setNotification({
        type: 'success',
        message: `Successfully approved ${doctor.name}. Account is now ACTIVE and can log into the Doctor Dashboard.`,
      });
      // Update local state immediately
      setRequests((prev) =>
        prev.map((r) => (r.userId === doctor.userId ? { ...r, status: 'ACTIVE' } : r))
      );
    } catch (err: any) {
      setNotification({
        type: 'error',
        message: err.message || `Failed to approve ${doctor.name}. Please try again.`,
      });
    } finally {
      setActionInProgress(null);
    }
  };

  const handleReject = async (doctor: DoctorRegistrationRequestItem) => {
    if (!window.confirm(`Are you sure you want to REJECT ${doctor.name}? They will not be able to log in or access the platform.`)) {
      return;
    }

    setActionInProgress(doctor.userId);
    setNotification(null);
    try {
      await adminService.rejectDoctorRequest(doctor.userId);
      setNotification({
        type: 'success',
        message: `Doctor registration for ${doctor.name} has been REJECTED.`,
      });
      // Update local state immediately
      setRequests((prev) =>
        prev.map((r) => (r.userId === doctor.userId ? { ...r, status: 'REJECTED' } : r))
      );
    } catch (err: any) {
      setNotification({
        type: 'error',
        message: err.message || `Failed to reject ${doctor.name}. Please try again.`,
      });
    } finally {
      setActionInProgress(null);
    }
  };

  const filteredRequests = requests.filter((req) => {
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      const matchName = req.name.toLowerCase().includes(q);
      const matchEmail = req.email.toLowerCase().includes(q);
      const matchLicense = (req.licenseNumber || '').toLowerCase().includes(q);
      const matchSpec = (req.specialization || '').toLowerCase().includes(q);
      const matchHosp = (req.hospitalOrganization || '').toLowerCase().includes(q);
      if (!matchName && !matchEmail && !matchLicense && !matchSpec && !matchHosp) {
        return false;
      }
    }
    return true;
  });

  const pendingCount = requests.filter((r) => r.status === 'PENDING').length;
  const activeCount = requests.filter((r) => r.status === 'ACTIVE').length;
  const rejectedCount = requests.filter((r) => r.status === 'REJECTED').length;

  return (
    <div className="space-y-6">
      {/* Top Banner & Stats */}
      <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-xs">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <span className="p-2 bg-clinical-100 text-clinical-800 rounded-lg">
                <Stethoscope className="w-5 h-5" />
              </span>
              <h2 className="text-lg font-bold text-slate-900">Doctor Registration Requests</h2>
            </div>
            <p className="text-xs text-slate-500 mt-1">
              Verify medical licenses, specializations, and hospital affiliations before granting access to patient risk data.
            </p>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={fetchRequests}
              disabled={isLoading}
              className="inline-flex items-center gap-1.5 px-3 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold rounded-lg transition-colors"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
              <span>Refresh</span>
            </button>
          </div>
        </div>

        {/* Counter cards */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mt-6 pt-6 border-t border-slate-100">
          <div className="p-3.5 bg-amber-50/70 border border-amber-200/80 rounded-xl flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-lg bg-amber-500 text-white flex items-center justify-center shadow-xs">
                <Clock className="w-5 h-5" />
              </div>
              <div>
                <div className="text-xs font-semibold text-amber-900">Pending Review</div>
                <div className="text-[11px] text-amber-700">Requires Admin Decision</div>
              </div>
            </div>
            <span className="text-2xl font-bold text-amber-900">{pendingCount}</span>
          </div>

          <div className="p-3.5 bg-emerald-50/70 border border-emerald-200/80 rounded-xl flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-lg bg-emerald-600 text-white flex items-center justify-center shadow-xs">
                <CheckCircle2 className="w-5 h-5" />
              </div>
              <div>
                <div className="text-xs font-semibold text-emerald-900">Active Doctors</div>
                <div className="text-[11px] text-emerald-700">Approved & Verified</div>
              </div>
            </div>
            <span className="text-2xl font-bold text-emerald-900">{activeCount}</span>
          </div>

          <div className="p-3.5 bg-rose-50/70 border border-rose-200/80 rounded-xl flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-lg bg-rose-500 text-white flex items-center justify-center shadow-xs">
                <XCircle className="w-5 h-5" />
              </div>
              <div>
                <div className="text-xs font-semibold text-rose-900">Rejected Requests</div>
                <div className="text-[11px] text-rose-700">Access Denied</div>
              </div>
            </div>
            <span className="text-2xl font-bold text-rose-900">{rejectedCount}</span>
          </div>
        </div>
      </div>

      {/* Notification Toast */}
      {notification && (
        <div
          className={`p-4 rounded-xl border text-xs flex items-center justify-between shadow-xs ${
            notification.type === 'success'
              ? 'bg-emerald-50 border-emerald-200 text-emerald-900'
              : 'bg-rose-50 border-rose-200 text-rose-900'
          }`}
        >
          <div className="flex items-center gap-2">
            {notification.type === 'success' ? (
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            ) : (
              <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
            )}
            <span className="font-medium">{notification.message}</span>
          </div>
          <button
            onClick={() => setNotification(null)}
            className="text-xs font-semibold opacity-70 hover:opacity-100 ml-4"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Filter and Search Bar */}
      <div className="bg-white rounded-xl border border-slate-200 p-4 shadow-xs flex flex-col sm:flex-row items-center justify-between gap-4">
        {/* Status Filters */}
        <div className="flex items-center gap-1.5 w-full sm:w-auto overflow-x-auto">
          <button
            onClick={() => setStatusFilter('ALL')}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
              statusFilter === 'ALL'
                ? 'bg-slate-900 text-white'
                : 'text-slate-600 hover:bg-slate-100'
            }`}
          >
            All Requests ({requests.length})
          </button>
          <button
            onClick={() => setStatusFilter('PENDING')}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors flex items-center gap-1.5 ${
              statusFilter === 'PENDING'
                ? 'bg-amber-500 text-white'
                : 'text-slate-600 hover:bg-slate-100'
            }`}
          >
            <span>Pending</span>
            {pendingCount > 0 && (
              <span className="px-1.5 py-0.2 rounded-full text-[10px] bg-white/30 font-bold">
                {pendingCount}
              </span>
            )}
          </button>
          <button
            onClick={() => setStatusFilter('ACTIVE')}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
              statusFilter === 'ACTIVE'
                ? 'bg-emerald-600 text-white'
                : 'text-slate-600 hover:bg-slate-100'
            }`}
          >
            Active ({activeCount})
          </button>
          <button
            onClick={() => setStatusFilter('REJECTED')}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
              statusFilter === 'REJECTED'
                ? 'bg-rose-600 text-white'
                : 'text-slate-600 hover:bg-slate-100'
            }`}
          >
            Rejected ({rejectedCount})
          </button>
        </div>

        {/* Search input */}
        <div className="relative w-full sm:w-72">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search doctor, license, hospital..."
            className="w-full pl-9 pr-3 py-1.5 text-xs border border-slate-300 rounded-lg focus:ring-1 focus:ring-slate-800"
          />
        </div>
      </div>

      {/* Table Section */}
      <div className="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-xs">
        {isLoading ? (
          <div className="p-12 text-center text-slate-500 text-sm">
            <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-clinical-600" />
            Loading doctor registration requests...
          </div>
        ) : filteredRequests.length === 0 ? (
          <div className="p-12 text-center">
            <div className="w-12 h-12 rounded-full bg-slate-100 text-slate-400 flex items-center justify-center mx-auto mb-3">
              <Stethoscope className="w-6 h-6" />
            </div>
            <h3 className="text-sm font-semibold text-slate-800">No doctor requests found</h3>
            <p className="text-xs text-slate-500 mt-1">
              There are no doctor registrations matching your current filter criteria.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold uppercase tracking-wider text-[11px]">
                  <th className="py-3.5 px-4">Doctor Name</th>
                  <th className="py-3.5 px-4">Medical License</th>
                  <th className="py-3.5 px-4">Specialization</th>
                  <th className="py-3.5 px-4">Hospital / Affiliation</th>
                  <th className="py-3.5 px-4">Contact</th>
                  <th className="py-3.5 px-4">Submitted</th>
                  <th className="py-3.5 px-4">Status</th>
                  <th className="py-3.5 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredRequests.map((doctor) => {
                  const isProcessing = actionInProgress === doctor.userId;
                  return (
                    <tr key={doctor.userId} className="hover:bg-slate-50/80 transition-colors">
                      <td className="py-3 px-4">
                        <div className="font-semibold text-slate-900">{doctor.name}</div>
                        <div className="text-[11px] text-slate-500 flex items-center gap-1 mt-0.5">
                          <Mail className="w-3 h-3 text-slate-400" />
                          <span>{doctor.email}</span>
                        </div>
                      </td>

                      <td className="py-3 px-4">
                        <div className="inline-flex items-center gap-1 px-2 py-0.5 rounded bg-slate-100 text-slate-800 font-mono font-medium border border-slate-200 text-[11px]">
                          <FileBadge2 className="w-3 h-3 text-slate-500" />
                          <span>{doctor.licenseNumber || 'N/A'}</span>
                        </div>
                      </td>

                      <td className="py-3 px-4 text-slate-700 font-medium">
                        {doctor.specialization || 'General Practitioner'}
                      </td>

                      <td className="py-3 px-4 text-slate-700">
                        <div className="flex items-center gap-1">
                          <Building2 className="w-3 h-3 text-slate-400 shrink-0" />
                          <span className="truncate max-w-[160px]">{doctor.hospitalOrganization || 'Independent'}</span>
                        </div>
                      </td>

                      <td className="py-3 px-4 text-slate-600">
                        <div className="flex items-center gap-1 font-mono text-[11px]">
                          <Phone className="w-3 h-3 text-slate-400 shrink-0" />
                          <span>{doctor.phone || 'N/A'}</span>
                        </div>
                      </td>

                      <td className="py-3 px-4 text-slate-500 text-[11px]">
                        {doctor.registrationDate
                          ? new Date(doctor.registrationDate).toLocaleDateString(undefined, {
                              year: 'numeric',
                              month: 'short',
                              day: 'numeric',
                            })
                          : 'Recently'}
                      </td>

                      <td className="py-3 px-4">
                        {doctor.status === 'PENDING' ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-amber-100 text-amber-800 border border-amber-200">
                            <Clock className="w-3 h-3 text-amber-600 animate-pulse" />
                            Pending Review
                          </span>
                        ) : doctor.status === 'ACTIVE' ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-100 text-emerald-800 border border-emerald-200">
                            <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                            Approved
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-rose-100 text-rose-800 border border-rose-200">
                            <XCircle className="w-3 h-3 text-rose-600" />
                            Rejected
                          </span>
                        )}
                      </td>

                      <td className="py-3 px-4 text-right">
                        {doctor.status === 'PENDING' ? (
                          <div className="flex items-center justify-end gap-1.5">
                            <button
                              type="button"
                              onClick={() => handleApprove(doctor)}
                              disabled={isProcessing}
                              className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-bold text-white bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 rounded-md transition-colors shadow-xs"
                              title="Approve Doctor Registration"
                            >
                              <UserCheck className="w-3.5 h-3.5" />
                              <span>Approve</span>
                            </button>
                            <button
                              type="button"
                              onClick={() => handleReject(doctor)}
                              disabled={isProcessing}
                              className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-bold text-rose-700 bg-rose-50 hover:bg-rose-100 border border-rose-200 disabled:opacity-50 rounded-md transition-colors"
                              title="Reject Doctor Registration"
                            >
                              <UserX className="w-3.5 h-3.5" />
                              <span>Reject</span>
                            </button>
                          </div>
                        ) : doctor.status === 'ACTIVE' ? (
                          <button
                            type="button"
                            onClick={() => handleReject(doctor)}
                            disabled={isProcessing}
                            className="inline-flex items-center gap-1 px-2 py-0.5 text-[11px] font-medium text-slate-500 hover:text-rose-700 hover:bg-rose-50 rounded transition-colors"
                            title="Revoke doctor access"
                          >
                            <span>Revoke Access</span>
                          </button>
                        ) : (
                          <button
                            type="button"
                            onClick={() => handleApprove(doctor)}
                            disabled={isProcessing}
                            className="inline-flex items-center gap-1 px-2 py-0.5 text-[11px] font-medium text-slate-500 hover:text-emerald-700 hover:bg-emerald-50 rounded transition-colors"
                            title="Re-approve doctor"
                          >
                            <span>Re-approve</span>
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
