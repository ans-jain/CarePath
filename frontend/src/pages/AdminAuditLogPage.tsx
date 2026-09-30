import React, { useState, useEffect, useCallback } from 'react';
import {
  ShieldAlert,
  ShieldCheck,
  Search,
  RefreshCw,
  Lock,
  UserX,
  FileText,
  AlertTriangle,
  ChevronLeft,
  ChevronRight,
  Database,
  KeyRound,
  Activity,
  CheckCircle2,
  Clock,
  ExternalLink,
  Stethoscope,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { adminService } from '../services/adminService';
import { AuditLogItem, SecurityOverview } from '../types/admin';
import { DoctorRegistrationRequestsSection } from '../components/admin/DoctorRegistrationRequestsSection';

export const AdminAuditLogPage: React.FC = () => {
  const { user, switchDemoPersona } = useAuth();
  const isAdmin = user?.role === 'ROLE_ADMIN';

  const [logs, setLogs] = useState<AuditLogItem[]>([]);
  const [overview, setOverview] = useState<SecurityOverview | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [currentPage, setCurrentPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [totalElements, setTotalElements] = useState<number>(0);

  // Filters
  const [actionTypeFilter, setActionTypeFilter] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [searchQuery, setSearchQuery] = useState<string>('');

  const fetchAuditData = useCallback(async () => {
    if (!isAdmin) return;
    setIsLoading(true);
    try {
      const [logsRes, overviewRes] = await Promise.all([
        adminService.getAuditLogs(
          currentPage,
          15,
          actionTypeFilter || undefined,
          statusFilter || undefined,
          searchQuery || undefined
        ),
        adminService.getSecurityOverview(),
      ]);
      setLogs(logsRes.content);
      setTotalPages(logsRes.totalPages);
      setTotalElements(logsRes.totalElements);
      setOverview(overviewRes);
    } catch (err) {
      console.error('Failed to load audit logs:', err);
    } finally {
      setIsLoading(false);
    }
  }, [isAdmin, currentPage, actionTypeFilter, statusFilter, searchQuery]);

  useEffect(() => {
    fetchAuditData();
  }, [fetchAuditData]);

  const handleFilterSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setCurrentPage(0);
    fetchAuditData();
  };

  const resetFilters = () => {
    setActionTypeFilter('');
    setStatusFilter('');
    setSearchQuery('');
    setCurrentPage(0);
  };

  const getActionBadge = (action: string) => {
    switch (action) {
      case 'FORBIDDEN_ACCESS_ATTEMPT':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-100 text-rose-800 border border-rose-200">
            <ShieldAlert className="w-3 h-3" />
            FORBIDDEN_ACCESS
          </span>
        );
      case 'LOGIN_SUCCESS':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 border border-emerald-200">
            <CheckCircle2 className="w-3 h-3" />
            LOGIN_SUCCESS
          </span>
        );
      case 'LOGIN_FAILURE':
      case 'LOGIN_FAILED_DISABLED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800 border border-amber-200">
            <AlertTriangle className="w-3 h-3" />
            {action}
          </span>
        );
      case 'ROLE_CHANGE':
      case 'USER_STATUS_CHANGE':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-purple-100 text-purple-800 border border-purple-200">
            <KeyRound className="w-3 h-3" />
            {action}
          </span>
        );
      case 'RISK_ASSESSMENT_COMPLETED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-sky-100 text-sky-800 border border-sky-200">
            <Activity className="w-3 h-3" />
            RISK_ASSESSMENT
          </span>
        );
      case 'VITALS_INGESTED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-teal-100 text-teal-800 border border-teal-200">
            <Database className="w-3 h-3" />
            VITALS_INGESTED
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-slate-100 text-slate-700 border border-slate-200">
            <FileText className="w-3 h-3" />
            {action}
          </span>
        );
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'SUCCESS':
        return (
          <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
            SUCCESS
          </span>
        );
      case 'FORBIDDEN':
        return (
          <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-rose-50 text-rose-700 border border-rose-300">
            BLOCKED (403)
          </span>
        );
      case 'FAILURE':
        return (
          <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-amber-50 text-amber-700 border border-amber-200">
            FAILURE
          </span>
        );
      default:
        return (
          <span className="px-2 py-0.5 rounded text-[11px] font-medium bg-slate-50 text-slate-600">
            {status}
          </span>
        );
    }
  };

  if (!isAdmin) {
    return (
      <div className="max-w-4xl mx-auto py-12 px-4 sm:px-6">
        <div className="bg-white rounded-xl border border-slate-200 p-8 shadow-xs text-center">
          <div className="w-16 h-16 bg-rose-50 text-rose-600 rounded-full flex items-center justify-center mx-auto mb-4 border border-rose-100">
            <Lock className="w-8 h-8" />
          </div>
          <h2 className="text-xl font-bold text-slate-900 mb-2">Restricted Security Domain</h2>
          <p className="text-sm text-slate-600 max-w-md mx-auto mb-6">
            The Audit Trail and Security Monitoring console requires elevated{' '}
            <code className="text-xs bg-slate-100 px-1.5 py-0.5 rounded font-mono text-slate-800">
              ROLE_ADMIN
            </code>{' '}
            credentials. Your current role is{' '}
            <code className="text-xs bg-amber-100 text-amber-900 px-1.5 py-0.5 rounded font-mono font-medium">
              {user?.role || 'ANONYMOUS'}
            </code>
            .
          </p>
          <div className="flex items-center justify-center gap-3">
            <button
              onClick={() => switchDemoPersona('ADMIN')}
              className="inline-flex items-center gap-2 px-4 py-2 bg-clinical-700 hover:bg-clinical-800 text-white rounded-lg text-sm font-medium shadow-xs transition-colors"
            >
              <KeyRound className="w-4 h-4" />
              Switch to Administrator Persona
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6 pb-12">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <div className="flex items-center gap-2.5">
            <div className="p-2 bg-slate-900 text-white rounded-lg shadow-xs">
              <ShieldCheck className="w-6 h-6" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Security & Audit Console</h1>
              <p className="text-xs text-slate-500 font-medium">
                Doctor Credential Verification &bull; Immutable HIPAA/GDPR Access Trail &bull; RBAC Governance
              </p>
            </div>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={fetchAuditData}
            disabled={isLoading}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 shadow-xs transition-colors"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
            Refresh Trail
          </button>
        </div>
      </div>

      {/* Dedicated Section: Doctor Registration Requests */}
      <DoctorRegistrationRequestsSection />

      {/* Section Header: Security & Audit Metrics */}
      <div className="pt-4 border-t border-slate-200">
        <h2 className="text-lg font-bold text-slate-900 mb-1">Security Monitoring & Audit Trail</h2>
        <p className="text-xs text-slate-500 mb-4">
          Real-time tamper-evident log of all authentication events, clinical assessments, and authorization checks.
        </p>
      </div>

      {/* Overview Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase text-slate-500 tracking-wider">Total Audit Events</span>
            <Database className="w-4 h-4 text-slate-400" />
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-slate-900">
              {overview?.totalAuditLogs ?? totalElements}
            </span>
            <span className="text-xs text-emerald-600 font-medium">Immutable</span>
          </div>
          <p className="text-[11px] text-slate-400 mt-1">Logged across all application tiers</p>
        </div>

        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase text-slate-500 tracking-wider">Access Violations</span>
            <ShieldAlert className="w-4 h-4 text-rose-500" />
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-rose-600">
              {overview?.failedAccessAttempts ?? 0}
            </span>
            <span className="text-xs text-rose-500 font-medium">Blocked</span>
          </div>
          <p className="text-[11px] text-slate-400 mt-1">Unassigned clinician or cross-patient probes</p>
        </div>

        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase text-slate-500 tracking-wider">Rate Limiter</span>
            <Activity className="w-4 h-4 text-teal-600" />
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-slate-900">Active</span>
            <span className="text-xs text-teal-600 font-medium">Sliding Window</span>
          </div>
          <p className="text-[11px] text-slate-400 mt-1">10/min auth &bull; 30/min ML &bull; 100/min API</p>
        </div>

        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase text-slate-500 tracking-wider">Security State</span>
            <CheckCircle2 className="w-4 h-4 text-emerald-600" />
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-emerald-600">
              {overview?.securityStatus || 'OPTIMAL'}
            </span>
            <span className="text-xs text-slate-500">RBAC Active</span>
          </div>
          <p className="text-[11px] text-slate-400 mt-1">Credentials scrubbed before disk storage</p>
        </div>
      </div>

      {/* Filter & Search Bar */}
      <form
        onSubmit={handleFilterSubmit}
        className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs flex flex-col md:flex-row gap-3 items-stretch md:items-center justify-between"
      >
        <div className="flex-1 flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Search by actor email, details, or entity..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-3 py-2 text-xs border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-clinical-500 focus:border-clinical-500"
            />
          </div>

          <div className="w-full sm:w-48">
            <select
              value={actionTypeFilter}
              onChange={(e) => setActionTypeFilter(e.target.value)}
              className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-clinical-500 bg-white"
            >
              <option value="">All Action Types</option>
              <option value="LOGIN_SUCCESS">LOGIN_SUCCESS</option>
              <option value="LOGIN_FAILURE">LOGIN_FAILURE</option>
              <option value="FORBIDDEN_ACCESS_ATTEMPT">FORBIDDEN_ACCESS_ATTEMPT</option>
              <option value="VITALS_INGESTED">VITALS_INGESTED</option>
              <option value="RISK_ASSESSMENT_COMPLETED">RISK_ASSESSMENT_COMPLETED</option>
              <option value="ROLE_CHANGE">ROLE_CHANGE</option>
              <option value="USER_STATUS_CHANGE">USER_STATUS_CHANGE</option>
              <option value="USER_REGISTERED">USER_REGISTERED</option>
            </select>
          </div>

          <div className="w-full sm:w-36">
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-clinical-500 bg-white"
            >
              <option value="">All Statuses</option>
              <option value="SUCCESS">SUCCESS</option>
              <option value="FORBIDDEN">FORBIDDEN</option>
              <option value="FAILURE">FAILURE</option>
            </select>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="submit"
            className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-lg text-xs font-semibold shadow-xs transition-colors"
          >
            Apply Filters
          </button>
          {(actionTypeFilter || statusFilter || searchQuery) && (
            <button
              type="button"
              onClick={resetFilters}
              className="px-3 py-2 text-xs font-medium text-slate-600 hover:text-slate-900 border border-slate-200 rounded-lg hover:bg-slate-50 transition-colors"
            >
              Clear
            </button>
          )}
        </div>
      </form>

      {/* Audit Log Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200 uppercase tracking-wider text-[10px]">
              <tr>
                <th className="py-3 px-4">Timestamp (UTC)</th>
                <th className="py-3 px-4">Actor</th>
                <th className="py-3 px-4">Action</th>
                <th className="py-3 px-4">Resource / Entity</th>
                <th className="py-3 px-4">Origin / Status</th>
                <th className="py-3 px-4">Details</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-slate-500">
                    <div className="flex flex-col items-center gap-2">
                      <RefreshCw className="w-5 h-5 text-clinical-600 animate-spin" />
                      <span>Loading audit trail events...</span>
                    </div>
                  </td>
                </tr>
              ) : logs.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-slate-500">
                    <ShieldCheck className="w-8 h-8 text-slate-300 mx-auto mb-2" />
                    <p className="font-medium text-slate-700">No audit events match current criteria</p>
                    <p className="text-xs text-slate-400 mt-1">Try broadening your search or resetting filters.</p>
                  </td>
                </tr>
              ) : (
                logs.map((log) => (
                  <tr key={log.id} className="hover:bg-slate-50/75 transition-colors">
                    {/* Timestamp */}
                    <td className="py-3 px-4 whitespace-nowrap text-slate-600">
                      <div className="flex items-center gap-1.5">
                        <Clock className="w-3.5 h-3.5 text-slate-400" />
                        <span className="font-mono text-[11px]">
                          {new Date(log.createdAt).toLocaleString(undefined, {
                            month: 'short',
                            day: 'numeric',
                            hour: '2-digit',
                            minute: '2-digit',
                            second: '2-digit',
                            hour12: false,
                          })}
                        </span>
                      </div>
                    </td>

                    {/* Actor */}
                    <td className="py-3 px-4 whitespace-nowrap">
                      <div className="font-medium text-slate-900">{log.actorEmail || 'System / Unauth'}</div>
                      {log.actorUserId && (
                        <div className="text-[10px] font-mono text-slate-400 truncate max-w-[140px]" title={log.actorUserId}>
                          {log.actorUserId}
                        </div>
                      )}
                    </td>

                    {/* Action */}
                    <td className="py-3 px-4 whitespace-nowrap">
                      {getActionBadge(log.actionType)}
                    </td>

                    {/* Resource / Entity */}
                    <td className="py-3 px-4">
                      {log.entityName ? (
                        <div>
                          <span className="font-semibold text-slate-800">{log.entityName}</span>
                          {log.entityId && (
                            <span className="block text-[10px] font-mono text-slate-400 truncate max-w-[120px]" title={log.entityId}>
                              ID: {log.entityId}
                            </span>
                          )}
                          {log.targetPatientId && (
                            <span className="block text-[10px] font-mono text-clinical-600 truncate max-w-[120px]" title={log.targetPatientId}>
                              Patient: {log.targetPatientId}
                            </span>
                          )}
                        </div>
                      ) : (
                        <span className="text-slate-400">&mdash;</span>
                      )}
                    </td>

                    {/* Origin / Status */}
                    <td className="py-3 px-4 whitespace-nowrap">
                      <div className="flex flex-col gap-1 items-start">
                        {getStatusBadge(log.status)}
                        <span className="font-mono text-[10px] text-slate-400">
                          {log.ipAddress || '127.0.0.1'}
                        </span>
                      </div>
                    </td>

                    {/* Details */}
                    <td className="py-3 px-4 max-w-xs text-slate-600 truncate" title={log.details || ''}>
                      {log.details || <span className="text-slate-300 italic">No details recorded</span>}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Bar */}
        <div className="px-4 py-3 bg-slate-50 border-t border-slate-200 flex items-center justify-between text-xs text-slate-600">
          <div>
            Showing <span className="font-semibold text-slate-900">{logs.length}</span> of{' '}
            <span className="font-semibold text-slate-900">{totalElements}</span> audit records
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
              disabled={currentPage === 0 || isLoading}
              className="p-1.5 rounded border border-slate-300 bg-white hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
              title="Previous Page"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <span className="font-medium text-slate-700">
              Page {currentPage + 1} of {Math.max(1, totalPages)}
            </span>
            <button
              onClick={() => setCurrentPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={currentPage >= totalPages - 1 || isLoading}
              className="p-1.5 rounded border border-slate-300 bg-white hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
              title="Next Page"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
