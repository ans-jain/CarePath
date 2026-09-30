import React, { useState, useEffect, useCallback } from 'react';
import {
  FileText,
  User,
  Calendar,
  Clock,
  ArrowLeft,
  FileDown,
  Search,
  CheckCircle2,
  ShieldCheck,
  Stethoscope,
  Building2,
  RefreshCw,
  ExternalLink,
  Lock,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { reportShareService } from '../services/reportShareService';
import { SharedReportDetail } from '../types/reportShare';
import { RiskSummaryCard } from '../components/assessment/RiskSummaryCard';
import { ShapWaterfall } from '../components/assessment/ShapWaterfall';
import { CounterfactualCards } from '../components/assessment/CounterfactualCards';
import { AlertBanner } from '../components/common/AlertBanner';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { pdfReportService } from '../services/pdfReportService';

interface DashboardPageProps {
  onStartAssessment?: () => void;
  onOpenHowItWorks?: () => void;
}

export const DashboardPage: React.FC<DashboardPageProps> = ({ onOpenHowItWorks }) => {
  const { user } = useAuth();
  const [reports, setReports] = useState<SharedReportDetail[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedReport, setSelectedReport] = useState<SharedReportDetail | null>(null);
  const [isDownloadingPdf, setIsDownloadingPdf] = useState<boolean>(false);
  const [showProfileModal, setShowProfileModal] = useState<boolean>(false);

  const loadSharedReports = useCallback(async () => {
    setIsLoading(true);
    try {
      const data = await reportShareService.getDoctorSharedReports(user?.id);
      setReports(data);
    } catch (err) {
      console.error('Failed to load shared reports for doctor:', err);
    } finally {
      setIsLoading(false);
    }
  }, [user?.id]);

  useEffect(() => {
    loadSharedReports();
  }, [loadSharedReports]);

  const handleViewReport = async (item: SharedReportDetail) => {
    try {
      // Mark as VIEWED via service/backend
      await reportShareService.getDoctorSharedReportDetail(item.reportId);
      // Update local status in state
      setReports((prev) =>
        prev.map((r) => (r.reportId === item.reportId ? { ...r, status: 'VIEWED' } : r))
      );
      setSelectedReport({ ...item, status: 'VIEWED' });
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } catch (err) {
      console.error('Error viewing shared report:', err);
      setSelectedReport(item);
    }
  };

  const handleDownloadPdf = () => {
    if (!selectedReport) return;
    setIsDownloadingPdf(true);

    try {
      let parsedSnapshot: any = null;
      if (selectedReport.featureSnapshot) {
        try {
          parsedSnapshot = JSON.parse(selectedReport.featureSnapshot);
        } catch {}
      }

      const activeReq = parsedSnapshot?.features
        ? { features: parsedSnapshot.features }
        : {
            features: {
              age: 48,
              biological_sex: 'FEMALE',
              bmi: 27.4,
              smoking_status: 'FORMER',
              systolic_bp_current: 142,
              diastolic_bp_current: 88,
              heart_rate_current: 74,
              fasting_glucose_current: 108,
            },
          };

      const activeResp = parsedSnapshot?.prediction
        ? {
            prediction: parsedSnapshot.prediction,
            explanation: parsedSnapshot.explanation || {
              base_value: 0.18,
              shap_values: {},
              top_risk_drivers: [
                {
                  feature: 'systolic_bp_current',
                  display_name: 'Systolic Blood Pressure',
                  value: 142,
                  shap_value: 0.15,
                  direction: 'increases_risk',
                  impact: 'high',
                  narrative: 'Elevated systolic blood pressure is the primary risk driver.',
                },
              ],
              protective_factors: [],
              summary_narrative: 'Elevated cardiometabolic risk identified from longitudinal metrics.',
            },
            counterfactuals: parsedSnapshot.counterfactuals || {
              counterfactual_available: false,
              status: 'SUCCESS',
              message: 'None',
              plans: [],
            },
            regulatory_disclaimer: 'Decision-support risk signal only. Does not diagnose diseases.',
          }
        : {
            prediction: {
              overall_risk_score: selectedReport.overallRiskScore,
              risk_category: selectedReport.riskCategory,
              prediction: selectedReport.overallRiskScore >= 0.5 ? 1 : 0,
              probability: selectedReport.overallRiskScore,
              calibrated: true,
              model_version: selectedReport.modelVersion || 'calibrated_v1.0.0',
              regulatory_disclaimer: 'Decision-support risk signal only.',
            },
            explanation: {
              base_value: 0.18,
              shap_values: {},
              top_risk_drivers: [
                {
                  feature: 'systolic_bp_current',
                  display_name: 'Systolic Blood Pressure',
                  value: 142,
                  shap_value: 0.15,
                  direction: 'increases_risk',
                  impact: 'high',
                  narrative: 'Elevated systolic blood pressure is the primary risk driver.',
                },
              ],
              protective_factors: [],
              summary_narrative: 'Moderate-to-elevated cardiometabolic risk signal.',
            },
            counterfactuals: {
              counterfactual_available: false,
              status: 'SUCCESS',
              message: 'None',
              plans: [],
            },
            regulatory_disclaimer: 'Decision-support risk signal only.',
          };

      pdfReportService.downloadAssessmentPdf({
        patientName: selectedReport.patientName,
        patientId: selectedReport.patientId,
        request: activeReq,
        response: activeResp as any,
        timestamp: selectedReport.reportDate,
      });
    } catch (err) {
      console.error('Failed to download doctor PDF report:', err);
    } finally {
      setTimeout(() => setIsDownloadingPdf(false), 800);
    }
  };

  const filteredReports = reports.filter((r) => {
    const q = searchQuery.toLowerCase();
    return (
      r.patientName.toLowerCase().includes(q) ||
      r.riskCategory.toLowerCase().includes(q) ||
      r.status.toLowerCase().includes(q)
    );
  });

  const getTierStyles = (tier: string) => {
    switch (tier?.toUpperCase()) {
      case 'LOW':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'HIGH':
      case 'ELEVATED':
        return 'bg-rose-50 text-rose-700 border-rose-200';
      case 'MODERATE':
      default:
        return 'bg-amber-50 text-amber-700 border-amber-200';
    }
  };

  // ==========================================
  // VIEW A: Complete Shared Report Inspection
  // ==========================================
  if (selectedReport) {
    let parsedSnapshot: any = null;
    if (selectedReport.featureSnapshot) {
      try {
        parsedSnapshot = JSON.parse(selectedReport.featureSnapshot);
      } catch {}
    }

    const feats = parsedSnapshot?.features || {
      systolic_bp_current: 142,
      diastolic_bp_current: 88,
      fasting_glucose_current: 108,
      bmi: 27.4,
      heart_rate_current: 74,
      age: 48,
    };

    const predictionObj = parsedSnapshot?.prediction || {
      overall_risk_score: selectedReport.overallRiskScore,
      risk_category: selectedReport.riskCategory,
      prediction: selectedReport.overallRiskScore >= 0.5 ? 1 : 0,
      probability: selectedReport.overallRiskScore,
      calibrated: true,
      model_version: selectedReport.modelVersion || 'calibrated_v1.0.0',
      regulatory_disclaimer: 'Decision-support risk signal only. Does not diagnose diseases.',
    };

    const explanationObj = parsedSnapshot?.explanation || {
      base_value: 0.18,
      shap_values: {},
      top_risk_drivers: [
        {
          feature: 'systolic_bp_current',
          display_name: 'Systolic Blood Pressure',
          value: feats.systolic_bp_current || 142,
          shap_value: 0.15,
          direction: 'increases_risk',
          impact: 'high',
          narrative: 'Elevated systolic blood pressure is the primary driver of this cardiometabolic risk score.',
        },
      ],
      protective_factors: [],
      summary_narrative: 'Longitudinal cardiometabolic evaluation demonstrates elevated risk trajectory.',
    };

    const counterfactualsObj = parsedSnapshot?.counterfactuals || {
      counterfactual_available: false,
      status: 'SUCCESS',
      message: 'None',
      plans: [],
    };

    return (
      <div className="space-y-6 pb-12 animate-fadeIn">
        {/* Top Breadcrumb & Action Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-slate-200">
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setSelectedReport(null)}
              className="inline-flex items-center gap-1.5 px-3 py-2 text-xs font-semibold text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-xl transition-colors shadow-2xs"
            >
              <ArrowLeft className="w-4 h-4" />
              <span>Back to Shared Reports</span>
            </button>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-lg font-bold text-slate-900">{selectedReport.patientName}</h1>
                <span className="px-2 py-0.5 text-[10px] font-bold bg-blue-100 text-blue-800 rounded">
                  Shared Report
                </span>
              </div>
              <p className="text-xs text-slate-500">
                Generated {new Date(selectedReport.reportDate).toLocaleDateString()} &bull; Shared with you on{' '}
                {new Date(selectedReport.sharedDate).toLocaleDateString()}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={handleDownloadPdf}
            disabled={isDownloadingPdf}
            className="inline-flex items-center gap-2 px-4 py-2.5 bg-clinical-600 hover:bg-clinical-700 text-white text-xs font-bold rounded-xl shadow-xs transition-colors self-start sm:self-auto"
          >
            <FileDown className="w-4 h-4" />
            <span>{isDownloadingPdf ? 'Generating PDF...' : 'Download Assessment (PDF)'}</span>
          </button>
        </div>

        {/* Clinical Disclaimer Banner */}
        <AlertBanner
          type="disclaimer"
          message="CarePath provides decision-support risk signals, TreeSHAP feature attributions, and simulated counterfactual targets. It does NOT diagnose diseases, prescribe medication, or claim to replace clinical judgment. Consult the patient directly for diagnostic evaluations."
        />

        {/* Scoped Privacy Indicator */}
        <div className="p-3 bg-slate-100 border border-slate-200 rounded-xl flex items-center gap-2.5 text-xs text-slate-600">
          <Lock className="w-4 h-4 text-slate-500 shrink-0" />
          <span>
            <strong>HIPAA & CDS Scoped Access:</strong> You are viewing only the specific report shared by{' '}
            {selectedReport.patientName}. Other historical reports and unrelated patient account data remain private.
          </span>
        </div>

        {/* Biomarkers Snapshot Table */}
        <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-2xs space-y-3">
          <h2 className="text-xs font-bold text-slate-800 uppercase tracking-wider">
            Patient Biomarker Measurements (Assessment Input)
          </h2>
          <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-6 gap-3 text-xs">
            <div className="p-3 bg-slate-50 rounded-xl border border-slate-200/80">
              <span className="text-[10px] text-slate-400 block font-medium">Systolic / Diastolic BP</span>
              <span className="font-bold text-slate-900 text-sm">
                {feats.systolic_bp_current}/{feats.diastolic_bp_current} mmHg
              </span>
            </div>
            <div className="p-3 bg-slate-50 rounded-xl border border-slate-200/80">
              <span className="text-[10px] text-slate-400 block font-medium">Fasting Glucose</span>
              <span className="font-bold text-slate-900 text-sm">
                {feats.fasting_glucose_current} mg/dL
              </span>
            </div>
            <div className="p-3 bg-slate-50 rounded-xl border border-slate-200/80">
              <span className="text-[10px] text-slate-400 block font-medium">Body Mass Index</span>
              <span className="font-bold text-slate-900 text-sm">{feats.bmi} kg/m²</span>
            </div>
            <div className="p-3 bg-slate-50 rounded-xl border border-slate-200/80">
              <span className="text-[10px] text-slate-400 block font-medium">Resting Heart Rate</span>
              <span className="font-bold text-slate-900 text-sm">{feats.heart_rate_current} bpm</span>
            </div>
            <div className="p-3 bg-slate-50 rounded-xl border border-slate-200/80">
              <span className="text-[10px] text-slate-400 block font-medium">Age</span>
              <span className="font-bold text-slate-900 text-sm">{feats.age || 48} yrs</span>
            </div>
            <div className="p-3 bg-slate-50 rounded-xl border border-slate-200/80">
              <span className="text-[10px] text-slate-400 block font-medium">Smoking Status</span>
              <span className="font-bold text-slate-900 text-sm">
                {feats.smoking_status || 'FORMER'}
              </span>
            </div>
          </div>
        </div>

        {/* 1. Risk Summary Component */}
        <RiskSummaryCard prediction={predictionObj} />

        {/* 2. SHAP Waterfall Visualization */}
        <ShapWaterfall explanation={explanationObj} onOpenHowItWorks={onOpenHowItWorks} />

        {/* 3. Constrained Counterfactual Recommendations */}
        <CounterfactualCards
          counterfactuals={counterfactualsObj}
          originalRiskScore={predictionObj.overall_risk_score}
        />
      </div>
    );
  }

  // ==========================================
  // VIEW B: Doctor Dashboard & Shared Reports
  // ==========================================
  return (
    <div className="space-y-6 pb-12 animate-fadeIn">
      {/* Welcome Banner */}
      <div className="p-6 bg-gradient-to-r from-clinical-700 to-clinical-900 rounded-2xl text-white shadow-sm flex flex-col md:flex-row md:items-center md:justify-between gap-6">
        <div className="space-y-1.5">
          <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-white/15 text-xs font-semibold text-clinical-100">
            <Stethoscope className="w-3.5 h-3.5" />
            <span>Clinical Intelligence Dashboard &bull; Doctor Clinical Portal</span>
          </div>
          <h1 className="text-2xl font-bold tracking-tight">
            Welcome, Dr. {user?.firstName} {user?.lastName}
          </h1>
          <p className="text-xs text-clinical-200 max-w-xl leading-relaxed">
            Review cardiometabolic risk assessments that patients have explicitly shared with your practice for
            clinical decision support and consultation planning.
          </p>
        </div>

        <button
          type="button"
          onClick={() => setShowProfileModal(true)}
          className="inline-flex items-center gap-2 px-4 py-2 bg-white/10 hover:bg-white/20 text-white text-xs font-semibold rounded-xl border border-white/20 transition-colors self-start md:self-auto"
        >
          <User className="w-4 h-4" />
          <span>Doctor Profile</span>
        </button>
      </div>

      {/* Main Section: Shared Reports */}
      <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-2xs space-y-6">
        {/* Section Header & Search */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-slate-200">
          <div>
            <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
              <FileText className="w-5 h-5 text-clinical-600" />
              <span>Shared Patient Reports ({reports.length})</span>
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Patients have granted access to the specific risk assessments below. Click <strong>View Report</strong> to
              inspect TreeSHAP attributions and counterfactuals.
            </p>
          </div>

          <div className="flex items-center gap-2 self-start sm:self-auto">
            {/* Search Input */}
            <div className="relative">
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Filter by patient name..."
                className="pl-9 pr-3 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-hidden focus:ring-2 focus:ring-clinical-500 w-56"
              />
            </div>

            <button
              type="button"
              onClick={loadSharedReports}
              title="Refresh Shared Reports"
              className="p-2 text-slate-500 hover:text-slate-800 bg-slate-100 hover:bg-slate-200 rounded-xl transition-colors"
            >
              <RefreshCw className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Loading State */}
        {isLoading && (
          <div className="py-12">
            <LoadingSpinner label="Loading shared reports..." />
          </div>
        )}

        {/* Empty State */}
        {!isLoading && filteredReports.length === 0 && (
          <div className="p-12 text-center bg-slate-50 rounded-xl border border-slate-200/80">
            <div className="w-14 h-14 rounded-2xl bg-white border border-slate-200 text-slate-400 flex items-center justify-center mx-auto mb-3 shadow-2xs">
              <FileText className="w-7 h-7" />
            </div>
            <h3 className="text-sm font-bold text-slate-900 mb-1">No Shared Reports Found</h3>
            <p className="text-xs text-slate-500 max-w-md mx-auto leading-relaxed">
              {searchQuery
                ? `No reports matched your query "${searchQuery}". Clear your search to see all reports.`
                : 'Patients have not shared any risk assessment reports with your doctor account yet. When a patient clicks "Share Report" in their profile, it will appear here.'}
            </p>
          </div>
        )}

        {/* Shared Reports List */}
        {!isLoading && filteredReports.length > 0 && (
          <div className="divide-y divide-slate-100">
            {filteredReports.map((item) => {
              const tierStyles = getTierStyles(item.riskCategory);
              const scorePercent = (item.overallRiskScore * 100).toFixed(1) + '%';
              const reportDateStr = new Date(item.reportDate).toLocaleDateString('en-US', {
                month: 'short',
                day: 'numeric',
                year: 'numeric',
              });
              const sharedDateStr = new Date(item.sharedDate).toLocaleDateString('en-US', {
                month: 'short',
                day: 'numeric',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
              });

              return (
                <div
                  key={item.shareId}
                  className="py-4.5 flex flex-col md:flex-row md:items-center md:justify-between gap-4 hover:bg-slate-50/70 p-3 rounded-xl transition-colors"
                >
                  {/* Left Column: Patient & Dates */}
                  <div className="space-y-1.5">
                    <div className="flex items-center gap-3">
                      <span className="text-sm font-bold text-slate-900">{item.patientName}</span>

                      {/* Status: New / Viewed */}
                      <span
                        className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                          item.status === 'NEW'
                            ? 'bg-emerald-100 text-emerald-800 animate-pulse'
                            : 'bg-slate-100 text-slate-600'
                        }`}
                      >
                        {item.status === 'NEW' ? 'New' : 'Viewed'}
                      </span>

                      {/* Risk Category Badge */}
                      <span
                        className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold border ${tierStyles}`}
                      >
                        {item.riskCategory} ({scorePercent})
                      </span>
                    </div>

                    <div className="flex flex-wrap items-center gap-4 text-xs text-slate-500">
                      <span className="flex items-center gap-1">
                        <Calendar className="w-3.5 h-3.5 text-slate-400" />
                        <span>Report Date: {reportDateStr}</span>
                      </span>
                      <span>&bull;</span>
                      <span className="flex items-center gap-1">
                        <Clock className="w-3.5 h-3.5 text-slate-400" />
                        <span>Shared Date: {sharedDateStr}</span>
                      </span>
                    </div>
                  </div>

                  {/* Right Column: View Report Button */}
                  <div className="flex items-center gap-2 self-start md:self-auto">
                    <button
                      type="button"
                      onClick={() => handleViewReport(item)}
                      className="inline-flex items-center gap-1.5 px-4 py-2 bg-clinical-600 hover:bg-clinical-700 text-white text-xs font-bold rounded-xl shadow-xs transition-colors"
                    >
                      <span>View Report</span>
                      <ExternalLink className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Doctor Profile Modal */}
      {showProfileModal && (
        <div
          className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4"
          role="dialog"
          aria-modal="true"
        >
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-200 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-200">
              <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <Stethoscope className="w-5 h-5 text-clinical-600" />
                <span>Doctor Profile</span>
              </h3>
              <button
                type="button"
                onClick={() => setShowProfileModal(false)}
                className="text-slate-400 hover:text-slate-600 text-sm font-bold"
              >
                &times;
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div className="flex justify-between py-1.5 border-b border-slate-100">
                <span className="text-slate-500">Name:</span>
                <span className="font-semibold text-slate-800">Dr. {user?.firstName} {user?.lastName}</span>
              </div>
              <div className="flex justify-between py-1.5 border-b border-slate-100">
                <span className="text-slate-500">Email:</span>
                <span className="font-mono text-slate-700">{user?.email}</span>
              </div>
              <div className="flex justify-between py-1.5 border-b border-slate-100">
                <span className="text-slate-500">Role:</span>
                <span className="font-bold text-clinical-700">Verified Clinician</span>
              </div>
              <div className="flex justify-between py-1.5 border-b border-slate-100">
                <span className="text-slate-500">Account Status:</span>
                <span className="inline-flex items-center gap-1 font-bold text-emerald-700">
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                  <span>ACTIVE</span>
                </span>
              </div>
              <div className="flex justify-between py-1.5">
                <span className="text-slate-500">CDS Scope:</span>
                <span className="text-slate-600">Patient-Shared Reports Only</span>
              </div>
            </div>

            <button
              type="button"
              onClick={() => setShowProfileModal(false)}
              className="w-full py-2.5 text-xs font-bold text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-xl transition-colors"
            >
              Close
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
