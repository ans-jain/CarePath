import React, { useState, useEffect, useCallback } from 'react';
import {
  X,
  Share2,
  Search,
  ShieldCheck,
  UserCheck,
  Building2,
  Stethoscope,
  Clock,
  CheckCircle2,
  AlertCircle,
  Eye,
  Trash2,
  Lock,
} from 'lucide-react';
import { SavedAssessment } from '../../types/explainability';
import { DoctorDirectoryItem, ReportShareItem } from '../../types/reportShare';
import { reportShareService } from '../../services/reportShareService';
import { useAuth } from '../../context/AuthContext';

interface ShareReportModalProps {
  isOpen: boolean;
  onClose: () => void;
  report: SavedAssessment | null;
  onShareUpdated?: () => void;
}

export const ShareReportModal: React.FC<ShareReportModalProps> = ({
  isOpen,
  onClose,
  report,
  onShareUpdated,
}) => {
  const { user } = useAuth();
  const [doctors, setDoctors] = useState<DoctorDirectoryItem[]>([]);
  const [shares, setShares] = useState<ReportShareItem[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [isLoadingDoctors, setIsLoadingDoctors] = useState(false);
  const [selectedDoctorForConfirm, setSelectedDoctorForConfirm] = useState<DoctorDirectoryItem | null>(null);
  const [isSubmittingShare, setIsSubmittingShare] = useState(false);
  const [revokingShareId, setRevokingShareId] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  const loadData = useCallback(async () => {
    if (!report) return;
    setIsLoadingDoctors(true);
    try {
      const [docList, shareList] = await Promise.all([
        reportShareService.getVerifiedDoctors(),
        reportShareService.getSharesForReport(report.id),
      ]);
      setDoctors(docList);
      setShares(shareList);
    } catch (err: any) {
      console.error('Failed to load sharing data:', err);
    } finally {
      setIsLoadingDoctors(false);
    }
  }, [report]);

  useEffect(() => {
    if (isOpen && report) {
      setFeedback(null);
      setSelectedDoctorForConfirm(null);
      loadData();
    }
  }, [isOpen, report, loadData]);

  if (!isOpen || !report) return null;

  const activeShares = shares.filter((s) => s.status !== 'REVOKED');
  const activeDoctorIds = new Set(activeShares.map((s) => s.doctorId));

  const filteredDoctors = doctors.filter((doc) => {
    const q = searchQuery.toLowerCase();
    return (
      doc.name.toLowerCase().includes(q) ||
      doc.specialization.toLowerCase().includes(q) ||
      doc.hospitalOrganization.toLowerCase().includes(q) ||
      doc.email.toLowerCase().includes(q)
    );
  });

  const handleConfirmShare = async () => {
    if (!selectedDoctorForConfirm || !report) return;
    setIsSubmittingShare(true);
    setFeedback(null);

    try {
      const patientName = user ? `${user.firstName} ${user.lastName}` : report.patientName || 'Sarah Jenkins';
      const patientId = user?.id || report.patientId || 'patient-uuid';

      await reportShareService.shareReport(
        report.id,
        selectedDoctorForConfirm.userId,
        selectedDoctorForConfirm,
        { name: patientName, id: patientId }
      );

      setFeedback({
        type: 'success',
        message: `Successfully shared report with ${selectedDoctorForConfirm.name}. They can now access this clinical assessment.`,
      });
      setSelectedDoctorForConfirm(null);
      await loadData();
      if (onShareUpdated) onShareUpdated();
    } catch (err: any) {
      setFeedback({
        type: 'error',
        message: err.message || 'Failed to share report. Please try again.',
      });
    } finally {
      setIsSubmittingShare(false);
    }
  };

  const handleRevokeShare = async (share: ReportShareItem) => {
    if (!window.confirm(`Revoke access for ${share.doctorName}? The doctor will immediately lose access to this report.`)) {
      return;
    }

    setRevokingShareId(share.id);
    setFeedback(null);

    try {
      await reportShareService.revokeShare(share.id);
      setFeedback({
        type: 'success',
        message: `Access revoked for ${share.doctorName}. This doctor can no longer view this report.`,
      });
      await loadData();
      if (onShareUpdated) onShareUpdated();
    } catch (err: any) {
      setFeedback({
        type: 'error',
        message: err.message || 'Failed to revoke access. Please try again.',
      });
    } finally {
      setRevokingShareId(null);
    }
  };

  const reportDateFormatted = new Date(report.timestamp).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });

  return (
    <div
      className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 sm:p-6"
      role="dialog"
      aria-modal="true"
      aria-labelledby="share-modal-title"
    >
      <div className="bg-white rounded-2xl max-w-2xl w-full max-h-[90vh] flex flex-col shadow-2xl border border-slate-200 overflow-hidden animate-in fade-in zoom-in-95 duration-150">
        {/* Header */}
        <div className="p-5 border-b border-slate-200 flex items-center justify-between bg-slate-50/70">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-clinical-100 text-clinical-700 flex items-center justify-center shadow-2xs">
              <Share2 className="w-5 h-5" />
            </div>
            <div>
              <h2 id="share-modal-title" className="text-base font-bold text-slate-900">
                Share Assessment Report
              </h2>
              <p className="text-xs text-slate-500">
                Generated {reportDateFormatted} &bull;{' '}
                <span className="font-semibold text-slate-700">
                  {report.riskCategory} Risk ({(report.overallRiskScore * 100).toFixed(1)}%)
                </span>
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-200/60 transition-colors"
            aria-label="Close"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Body */}
        <div className="p-6 overflow-y-auto space-y-6 flex-1 text-xs">
          {/* Privacy & Scoped Sharing Clinical Disclaimer */}
          <div className="p-4 bg-emerald-50/80 border border-emerald-200/80 rounded-xl flex items-start gap-3">
            <ShieldCheck className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" />
            <div className="space-y-1">
              <span className="font-bold text-emerald-900 block">Strictly Scoped Access</span>
              <p className="text-emerald-800 leading-relaxed">
                Only this specific report is shared with your chosen physician. The doctor will{' '}
                <strong>NOT</strong> have access to your full account, other historical assessments, or unshared data.
                You can revoke access at any time.
              </p>
            </div>
          </div>

          {/* Alert Feedback Banner */}
          {feedback && (
            <div
              className={`p-3.5 rounded-xl border flex items-center justify-between ${
                feedback.type === 'success'
                  ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
                  : 'bg-rose-50 text-rose-800 border-rose-200'
              }`}
            >
              <div className="flex items-center gap-2">
                {feedback.type === 'success' ? (
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                ) : (
                  <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
                )}
                <span>{feedback.message}</span>
              </div>
              <button
                onClick={() => setFeedback(null)}
                className="text-slate-400 hover:text-slate-600"
              >
                &times;
              </button>
            </div>
          )}

          {/* Section 1: Doctors who currently have access */}
          <div className="space-y-3">
            <div className="flex items-center justify-between pb-1 border-b border-slate-100">
              <h3 className="font-bold text-slate-900 flex items-center gap-2 text-xs uppercase tracking-wider text-slate-500">
                <Lock className="w-3.5 h-3.5 text-slate-500" />
                <span>Physicians With Current Access ({activeShares.length})</span>
              </h3>
            </div>

            {activeShares.length === 0 ? (
              <p className="text-slate-400 italic py-2">
                This assessment has not been shared with any physicians yet.
              </p>
            ) : (
              <div className="space-y-2">
                {activeShares.map((share) => (
                  <div
                    key={share.id}
                    className="p-3 bg-slate-50 border border-slate-200 rounded-xl flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3"
                  >
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-slate-900">{share.doctorName}</span>
                        <span
                          className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                            share.status === 'VIEWED'
                              ? 'bg-blue-100 text-blue-800'
                              : 'bg-amber-100 text-amber-800'
                          }`}
                        >
                          {share.status === 'VIEWED' ? 'Viewed by Doctor' : 'New / Unopened'}
                        </span>
                      </div>
                      <p className="text-[11px] text-slate-500 flex items-center gap-1.5">
                        <span>{share.doctorSpecialty || 'General Practice'}</span>
                        <span>&bull;</span>
                        <span>{share.doctorClinic || 'CarePath Network'}</span>
                      </p>
                      <p className="text-[10px] text-slate-400 flex items-center gap-1">
                        <Clock className="w-3 h-3" />
                        <span>Shared on {new Date(share.sharedAt).toLocaleDateString()}</span>
                      </p>
                    </div>

                    <button
                      type="button"
                      onClick={() => handleRevokeShare(share)}
                      disabled={revokingShareId === share.id}
                      className="inline-flex items-center gap-1 px-3 py-1.5 text-xs font-semibold text-rose-700 bg-white hover:bg-rose-50 border border-rose-200 hover:border-rose-300 rounded-lg transition-colors shadow-2xs self-start sm:self-auto"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                      <span>{revokingShareId === share.id ? 'Revoking...' : 'Revoke Access'}</span>
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Section 2: Share With a Doctor (Search & Select) */}
          <div className="space-y-3 pt-2">
            <h3 className="font-bold text-slate-900 flex items-center gap-2 text-xs uppercase tracking-wider text-slate-500">
              <Stethoscope className="w-3.5 h-3.5 text-clinical-600" />
              <span>Share With a Verified CarePath Doctor</span>
            </h3>

            {/* Search Input */}
            <div className="relative">
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search verified doctors by name, specialty, or clinic..."
                className="w-full pl-9 pr-3 py-2 text-xs bg-white border border-slate-200 rounded-xl focus:outline-hidden focus:ring-2 focus:ring-clinical-500"
              />
            </div>

            {/* Doctor Selection List */}
            {isLoadingDoctors ? (
              <div className="py-6 text-center text-slate-400">Loading registered doctors...</div>
            ) : filteredDoctors.length === 0 ? (
              <div className="py-6 text-center text-slate-400">
                No verified doctors found matching "{searchQuery}".
              </div>
            ) : (
              <div className="max-h-60 overflow-y-auto space-y-2 pr-1">
                {filteredDoctors.map((doc) => {
                  const isAlreadyShared = activeDoctorIds.has(doc.userId);
                  const isSelected = selectedDoctorForConfirm?.userId === doc.userId;

                  return (
                    <div
                      key={doc.userId}
                      className={`p-3.5 rounded-xl border transition-all flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 ${
                        isSelected
                          ? 'border-clinical-500 bg-clinical-50/50'
                          : 'border-slate-200 bg-white hover:border-slate-300'
                      }`}
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-slate-900 text-xs">{doc.name}</span>
                          <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800">
                            <UserCheck className="w-3 h-3" />
                            <span>Verified</span>
                          </span>
                        </div>
                        <p className="text-[11px] text-slate-600">{doc.specialization}</p>
                        <p className="text-[10px] text-slate-400 flex items-center gap-1">
                          <Building2 className="w-3 h-3" />
                          <span>{doc.hospitalOrganization}</span>
                          <span>&bull;</span>
                          <span>Lic: {doc.licenseNumber}</span>
                        </p>
                      </div>

                      {isAlreadyShared ? (
                        <span className="inline-flex items-center gap-1 px-2.5 py-1 text-[11px] font-bold text-slate-600 bg-slate-100 rounded-lg self-start sm:self-auto">
                          <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                          <span>Access Active</span>
                        </span>
                      ) : (
                        <button
                          type="button"
                          onClick={() => setSelectedDoctorForConfirm(doc)}
                          className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition-colors self-start sm:self-auto ${
                            isSelected
                              ? 'bg-clinical-700 text-white'
                              : 'bg-clinical-50 hover:bg-clinical-100 text-clinical-700 border border-clinical-200'
                          }`}
                        >
                          {isSelected ? 'Selected' : 'Select Doctor'}
                        </button>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Section 3: Confirm Share Drawer */}
          {selectedDoctorForConfirm && (
            <div className="p-4 bg-clinical-50 border border-clinical-200 rounded-xl space-y-3 animate-in fade-in duration-200">
              <div className="flex items-center justify-between">
                <span className="font-bold text-clinical-900 text-xs">
                  Confirm Report Sharing
                </span>
                <button
                  type="button"
                  onClick={() => setSelectedDoctorForConfirm(null)}
                  className="text-clinical-600 hover:text-clinical-800 text-[11px]"
                >
                  Cancel
                </button>
              </div>
              <p className="text-clinical-800 text-xs leading-relaxed">
                You are about to grant <strong>{selectedDoctorForConfirm.name}</strong> access to this specific
                cardiometabolic risk report (Generated {reportDateFormatted}). They will be able to review your
                biomarkers, TreeSHAP feature attributions, and counterfactual targets for clinical decision support.
              </p>
              <div className="flex items-center gap-2 pt-1">
                <button
                  type="button"
                  onClick={handleConfirmShare}
                  disabled={isSubmittingShare}
                  className="inline-flex items-center gap-1.5 px-4 py-2 bg-clinical-600 hover:bg-clinical-700 text-white text-xs font-bold rounded-lg shadow-xs transition-colors"
                >
                  <Share2 className="w-3.5 h-3.5" />
                  <span>{isSubmittingShare ? 'Sharing...' : 'Confirm & Share Report'}</span>
                </button>
                <button
                  type="button"
                  onClick={() => setSelectedDoctorForConfirm(null)}
                  className="px-3 py-2 text-xs font-semibold text-slate-600 hover:text-slate-800"
                >
                  Cancel
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="p-4 border-t border-slate-200 bg-slate-50 flex items-center justify-between">
          <span className="text-[11px] text-slate-400">
            CarePath Clinical Decision Support &bull; HIPAA / 21st Century Cures Act Compliant
          </span>
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-1.5 text-xs font-semibold text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg transition-colors"
          >
            Done
          </button>
        </div>
      </div>
    </div>
  );
};
