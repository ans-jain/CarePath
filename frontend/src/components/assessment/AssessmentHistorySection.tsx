import React, { useState } from 'react';
import { SavedAssessment } from '../../types/explainability';
import { pdfReportService } from '../../services/pdfReportService';
import { useAuth } from '../../context/AuthContext';
import {
  History,
  FileDown,
  ExternalLink,
  Calendar,
  Activity,
  CheckCircle2,
  Clock,
  Sparkles,
  ArrowRight,
  ShieldCheck,
  TrendingUp,
  TrendingDown,
  Share2,
} from 'lucide-react';
import { ShareReportModal } from './ShareReportModal';

interface AssessmentHistorySectionProps {
  history: SavedAssessment[];
  onSelectAssessment: (assessment: SavedAssessment) => void;
  onStartNewAssessment: () => void;
  onRefresh?: () => void;
}

export const AssessmentHistorySection: React.FC<AssessmentHistorySectionProps> = ({
  history,
  onSelectAssessment,
  onStartNewAssessment,
  onRefresh,
}) => {
  const { user } = useAuth();
  const [downloadingId, setDownloadingId] = useState<string | null>(null);
  const [selectedReportForShare, setSelectedReportForShare] = useState<SavedAssessment | null>(null);

  const handleShareReport = (e: React.MouseEvent, item: SavedAssessment) => {
    e.stopPropagation();
    setSelectedReportForShare(item);
  };

  const handleDownloadPdf = (e: React.MouseEvent, item: SavedAssessment) => {
    e.stopPropagation();
    setDownloadingId(item.id);

    try {
      pdfReportService.downloadAssessmentPdf({
        patientName: item.patientName || (user ? `${user.firstName} ${user.lastName}` : 'Sarah Jenkins'),
        patientEmail: user?.email,
        patientId: item.patientId || user?.id,
        request: item.request,
        response: item.response,
        timestamp: item.timestamp,
      });
    } catch (err) {
      console.error('Failed to generate assessment PDF:', err);
    } finally {
      setTimeout(() => setDownloadingId(null), 1000);
    }
  };

  const getTierStyles = (tier: string) => {
    switch (tier?.toUpperCase()) {
      case 'LOW':
        return {
          bg: 'bg-emerald-50 text-emerald-700 border-emerald-200',
          dot: 'bg-emerald-500',
          iconColor: 'text-emerald-600',
        };
      case 'HIGH':
      case 'ELEVATED':
        return {
          bg: 'bg-rose-50 text-rose-700 border-rose-200',
          dot: 'bg-rose-500',
          iconColor: 'text-rose-600',
        };
      case 'MODERATE':
      default:
        return {
          bg: 'bg-amber-50 text-amber-700 border-amber-200',
          dot: 'bg-amber-500',
          iconColor: 'text-amber-600',
        };
    }
  };

  if (history.length === 0) {
    return (
      <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center shadow-2xs">
        <div className="w-16 h-16 rounded-2xl bg-clinical-50 border border-clinical-200 text-clinical-600 flex items-center justify-center mx-auto mb-4">
          <History className="w-8 h-8" />
        </div>
        <h3 className="text-lg font-bold text-slate-900 mb-1">No Assessment History Yet</h3>
        <p className="text-xs text-slate-500 max-w-md mx-auto mb-6 leading-relaxed">
          You haven't completed any cardiometabolic risk assessments yet. When you run an assessment,
          your multi-modal biometric results, TreeSHAP attributions, and downloadable PDF reports will be permanently archived here.
        </p>
        <button
          type="button"
          onClick={onStartNewAssessment}
          className="inline-flex items-center gap-2 px-5 py-2.5 bg-clinical-600 hover:bg-clinical-700 text-white text-xs font-bold rounded-xl shadow-xs transition-colors"
        >
          <span>Run Your First Assessment</span>
          <ArrowRight className="w-4 h-4" />
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 pb-2 border-b border-slate-200">
        <div>
          <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
            <History className="w-5 h-5 text-clinical-600" />
            <span>Patient Assessment History ({history.length})</span>
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Past evaluations archived in your profile. Select any assessment to view detailed TreeSHAP attributions or download a doctor-ready PDF report.
          </p>
        </div>

        <button
          type="button"
          onClick={onStartNewAssessment}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-clinical-600 hover:bg-clinical-700 text-white text-xs font-semibold rounded-lg shadow-2xs transition-colors self-start sm:self-auto"
        >
          <Activity className="w-3.5 h-3.5" />
          <span>New Assessment</span>
        </button>
      </div>

      <div className="grid grid-cols-1 gap-4">
        {history.map((item, index) => {
          const tier = item.riskCategory || 'MODERATE';
          const styles = getTierStyles(tier);
          const scorePercent = (item.overallRiskScore * 100).toFixed(1) + '%';
          const feats = item.request.features;
          const topDriver = item.response.explanation.top_risk_drivers?.[0];
          const dateStr = new Date(item.timestamp).toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
          });

          return (
            <div
              key={item.id || index}
              onClick={() => onSelectAssessment(item)}
              className="bg-white rounded-xl border border-slate-200 hover:border-clinical-400 hover:shadow-md transition-all p-5 cursor-pointer group"
            >
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 pb-3 border-b border-slate-100">
                <div className="flex items-center gap-2.5">
                  <div className="w-8 h-8 rounded-lg bg-slate-100 text-slate-600 flex items-center justify-center">
                    <Calendar className="w-4 h-4" />
                  </div>
                  <div>
                    <span className="text-xs font-bold text-slate-800 block">{dateStr}</span>
                    <span className="text-[11px] text-slate-400">
                      Model: {item.modelVersion || 'Calibrated v1.0.0'} &bull; {item.confidenceLevel || 'Longitudinal Robust'}
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <span
                    className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold border ${styles.bg}`}
                  >
                    <span className={`w-2 h-2 rounded-full ${styles.dot}`} />
                    <span>{tier} Risk ({scorePercent})</span>
                  </span>

                  <button
                    type="button"
                    onClick={(e) => handleShareReport(e, item)}
                    title="Share Assessment Report with a Doctor"
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-clinical-50 hover:bg-clinical-100 text-clinical-700 text-xs font-semibold rounded-lg transition-colors border border-clinical-200 hover:border-clinical-300 shadow-2xs"
                  >
                    <Share2 className="w-3.5 h-3.5 text-clinical-600" />
                    <span>Share Report</span>
                  </button>

                  <button
                    type="button"
                    onClick={(e) => handleDownloadPdf(e, item)}
                    disabled={downloadingId === item.id}
                    title="Download Assessment Report as PDF"
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold rounded-lg transition-colors border border-slate-200 hover:border-slate-300"
                  >
                    <FileDown className="w-3.5 h-3.5 text-clinical-600" />
                    <span>{downloadingId === item.id ? 'Generating...' : 'Download PDF'}</span>
                  </button>
                </div>
              </div>

              {/* Biomarkers summary row */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-3 text-xs">
                <div className="p-2 bg-slate-50 rounded-lg">
                  <span className="text-[10px] text-slate-400 block font-medium">Blood Pressure</span>
                  <span className="font-semibold text-slate-800">
                    {feats.systolic_bp_current}/{feats.diastolic_bp_current} mmHg
                  </span>
                </div>
                <div className="p-2 bg-slate-50 rounded-lg">
                  <span className="text-[10px] text-slate-400 block font-medium">Fasting Glucose</span>
                  <span className="font-semibold text-slate-800">
                    {feats.fasting_glucose_current} mg/dL
                  </span>
                </div>
                <div className="p-2 bg-slate-50 rounded-lg">
                  <span className="text-[10px] text-slate-400 block font-medium">Body Mass Index</span>
                  <span className="font-semibold text-slate-800">
                    {feats.bmi} kg/m²
                  </span>
                </div>
                <div className="p-2 bg-slate-50 rounded-lg">
                  <span className="text-[10px] text-slate-400 block font-medium">Heart Rate</span>
                  <span className="font-semibold text-slate-800">
                    {feats.heart_rate_current} bpm
                  </span>
                </div>
              </div>

              {/* Narrative excerpt & CTA */}
              <div className="mt-3 pt-2.5 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 text-xs border-t border-slate-50">
                <p className="text-[11px] text-slate-500 italic line-clamp-1">
                  {topDriver
                    ? `Primary Risk Driver: ${topDriver.display_name} (+${topDriver.shap_value.toFixed(2)})`
                    : item.response.explanation.summary_narrative}
                </p>
                <div className="flex items-center gap-1 text-clinical-600 font-semibold group-hover:text-clinical-700 text-xs shrink-0">
                  <span>Inspect Full Attributions</span>
                  <ExternalLink className="w-3.5 h-3.5 group-hover:translate-x-0.5 transition-transform" />
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Share Report Modal */}
      <ShareReportModal
        isOpen={Boolean(selectedReportForShare)}
        onClose={() => setSelectedReportForShare(null)}
        report={selectedReportForShare}
        onShareUpdated={onRefresh}
      />
    </div>
  );
};
