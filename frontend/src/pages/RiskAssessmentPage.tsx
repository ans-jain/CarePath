import React, { useState, useEffect } from 'react';
import { ExplainRiskRequest, ExplainRiskResponse, SavedAssessment } from '../types/explainability';
import { mlService } from '../services/mlService';
import { notificationService } from '../services/notificationService';
import { assessmentService } from '../services/assessmentService';
import { pdfReportService } from '../services/pdfReportService';
import { useAuth } from '../context/AuthContext';
import { PatientForm } from '../components/assessment/PatientForm';
import { RiskSummaryCard } from '../components/assessment/RiskSummaryCard';
import { ShapWaterfall } from '../components/assessment/ShapWaterfall';
import { CounterfactualCards } from '../components/assessment/CounterfactualCards';
import { AssessmentHistorySection } from '../components/assessment/AssessmentHistorySection';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { AlertBanner } from '../components/common/AlertBanner';
import {
  ArrowLeft,
  FileDown,
  History,
  Activity,
  CheckCircle2,
  Calendar,
  Sparkles,
  Share2,
} from 'lucide-react';
import { ShareReportModal } from '../components/assessment/ShareReportModal';

interface RiskAssessmentPageProps {
  onOpenHowItWorks: () => void;
}

export const RiskAssessmentPage: React.FC<RiskAssessmentPageProps> = ({ onOpenHowItWorks }) => {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState<'assessment' | 'history'>('assessment');
  const [result, setResult] = useState<ExplainRiskResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [lastRequest, setLastRequest] = useState<ExplainRiskRequest | null>(null);
  const [currentSavedAssessment, setCurrentSavedAssessment] = useState<SavedAssessment | null>(null);
  const [history, setHistory] = useState<SavedAssessment[]>([]);
  const [isDownloadingPdf, setIsDownloadingPdf] = useState<boolean>(false);
  const [isShareModalOpen, setIsShareModalOpen] = useState<boolean>(false);

  // Load patient assessment history
  useEffect(() => {
    loadHistory();
  }, [user?.id]);

  const loadHistory = async () => {
    try {
      const items = await assessmentService.getAssessmentHistory(user?.id);
      setHistory(items);
    } catch (err) {
      console.warn('Could not load assessment history:', err);
    }
  };

  const handleAssessmentSubmit = async (request: ExplainRiskRequest) => {
    setIsLoading(true);
    setError(null);
    setLastRequest(request);

    try {
      const response = await mlService.explainRisk(request);
      setResult(response);

      // Auto-save this completed assessment to the patient profile history
      const newRecord: SavedAssessment = {
        id: `assessment-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
        patientId: user?.id,
        patientName: user ? `${user.firstName} ${user.lastName}` : 'Sarah Jenkins',
        timestamp: new Date().toISOString(),
        overallRiskScore: response.prediction.overall_risk_score,
        riskCategory: response.prediction.risk_category,
        confidenceLevel: response.prediction.confidence_level || 'LONGITUDINAL_ROBUST',
        modelVersion: response.prediction.model_version || 'calibrated_v1.0.0',
        request,
        response,
      };

      await assessmentService.saveAssessment(newRecord);
      setCurrentSavedAssessment(newRecord);
      setHistory((prev) => [newRecord, ...prev.filter((p) => p.id !== newRecord.id)]);

      notificationService.recordAssessmentCompleted(
        'Risk Assessment Completed',
        'Your latest cardiometabolic risk assessment is ready for review and saved in your profile.'
      ).catch(() => {});
    } catch (err: any) {
      setError(
        err.message ||
          'Failed to evaluate risk. Please verify that the CarePath ML service is running on http://localhost:8000.'
      );
      setResult(null);
    } finally {
      setIsLoading(false);
    }
  };

  const handleReset = () => {
    setResult(null);
    setError(null);
    setCurrentSavedAssessment(null);
    setActiveTab('assessment');
  };

  const handleSelectFromHistory = (item: SavedAssessment) => {
    setResult(item.response);
    setLastRequest(item.request);
    setCurrentSavedAssessment(item);
    setActiveTab('assessment');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleDownloadPdf = () => {
    if (!result) return;
    setIsDownloadingPdf(true);

    try {
      const activeReq = lastRequest || currentSavedAssessment?.request || {
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

      pdfReportService.downloadAssessmentPdf({
        patientName: user ? `${user.firstName} ${user.lastName}` : 'Sarah Jenkins',
        patientEmail: user?.email,
        patientId: user?.id,
        request: activeReq,
        response: result,
        timestamp: currentSavedAssessment?.timestamp || new Date().toISOString(),
      });
    } catch (err) {
      console.error('Error generating PDF report:', err);
    } finally {
      setTimeout(() => setIsDownloadingPdf(false), 800);
    }
  };

  return (
    <div className="space-y-6 pb-12">
      {/* Top Page Header & Navigation View Selector */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">
            Cardiometabolic Risk Assessment
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Explainable decision-support risk stratification powered by Calibrated Gradient Boosting and TreeSHAP.
          </p>
        </div>

        {/* Tab Navigation: Run Assessment vs History */}
        <div className="flex items-center gap-1 bg-slate-200/80 p-1 rounded-xl self-start sm:self-auto">
          <button
            type="button"
            onClick={() => {
              setActiveTab('assessment');
            }}
            className={`inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs font-bold transition-all ${
              activeTab === 'assessment'
                ? 'bg-white text-slate-900 shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <Activity className="w-3.5 h-3.5 text-clinical-600" />
            <span>Assessment Tool</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('history')}
            className={`inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs font-bold transition-all ${
              activeTab === 'history'
                ? 'bg-white text-slate-900 shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <History className="w-3.5 h-3.5 text-slate-600" />
            <span>Profile History</span>
            {history.length > 0 && (
              <span className="px-1.5 py-0.5 text-[10px] font-extrabold rounded-full bg-clinical-100 text-clinical-800">
                {history.length}
              </span>
            )}
          </button>
        </div>
      </div>

      {/* Mandatory Non-Diagnostic Clinical Disclaimer Banner */}
      <AlertBanner
        type="disclaimer"
        message="CarePath provides decision-support risk signals, feature attributions, and simulated counterfactual targets. It does NOT diagnose diseases, prescribe medication, or replace a licensed physician. Consult a medical professional for clinical diagnosis."
      />

      {/* API Error State */}
      {error && (
        <AlertBanner
          type="error"
          title="Assessment Computation Error"
          message={error}
          onRetry={() => lastRequest && handleAssessmentSubmit(lastRequest)}
        />
      )}

      {/* Loading State */}
      {isLoading && (
        <LoadingSpinner
          label="Evaluating 18 Biomarker Dimensions & Computing TreeSHAP..."
          subLabel="Generating exact Shapley waterfall attributions, solving counterfactuals, and archiving to profile history..."
        />
      )}

      {/* VIEW 1: Assessment History Tab */}
      {!isLoading && activeTab === 'history' && (
        <AssessmentHistorySection
          history={history}
          onSelectAssessment={handleSelectFromHistory}
          onStartNewAssessment={() => {
            setActiveTab('assessment');
            setResult(null);
          }}
          onRefresh={loadHistory}
        />
      )}

      {/* VIEW 2: Assessment Tool Tab */}
      {!isLoading && activeTab === 'assessment' && (
        <>
          {/* Sub-view A: Biomarker Input Form */}
          {!result && (
            <PatientForm onSubmit={handleAssessmentSubmit} isLoading={isLoading} />
          )}

          {/* Sub-view B: Assessment Results with PDF Download & History Indicators */}
          {result && (
            <div className="space-y-6 animate-fadeIn">
              {/* Action Banner above Results */}
              <div className="p-4 bg-white rounded-xl border border-slate-200 shadow-2xs flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
                    <CheckCircle2 className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-bold text-slate-900">
                        Assessment Successfully Computed
                      </span>
                      <span className="px-2 py-0.5 text-[10px] font-bold bg-emerald-100 text-emerald-800 rounded">
                        Saved in Profile
                      </span>
                    </div>
                    <p className="text-xs text-slate-500">
                      Archived in your longitudinal history &bull; You can download a clinical PDF report for doctor consultations
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-2.5 self-start sm:self-auto">
                  <button
                    type="button"
                    onClick={handleDownloadPdf}
                    disabled={isDownloadingPdf}
                    className="inline-flex items-center gap-2 px-4 py-2.5 bg-clinical-600 hover:bg-clinical-700 text-white text-xs font-bold rounded-xl shadow-xs hover:shadow transition-all"
                  >
                    <FileDown className="w-4 h-4" />
                    <span>{isDownloadingPdf ? 'Generating PDF...' : 'Download Assessment (PDF)'}</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setIsShareModalOpen(true)}
                    className="inline-flex items-center gap-2 px-3.5 py-2.5 bg-clinical-50 hover:bg-clinical-100 text-clinical-700 text-xs font-bold rounded-xl border border-clinical-200 hover:border-clinical-300 transition-colors shadow-2xs"
                  >
                    <Share2 className="w-4 h-4 text-clinical-600" />
                    <span>Share Report</span>
                  </button>

                  <button
                    type="button"
                    onClick={handleReset}
                    className="inline-flex items-center gap-1.5 px-3.5 py-2 text-xs font-semibold text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-xl transition-colors"
                  >
                    <ArrowLeft className="w-3.5 h-3.5" />
                    <span>New Assessment</span>
                  </button>
                </div>
              </div>

              {/* 1. Risk Summary Component */}
              <RiskSummaryCard prediction={result.prediction} />

              {/* 2. SHAP Explanation & Waterfall Visualization */}
              <ShapWaterfall explanation={result.explanation} onOpenHowItWorks={onOpenHowItWorks} />

              {/* 3. Constrained Counterfactual Recommendations */}
              <CounterfactualCards
                counterfactuals={result.counterfactuals}
                originalRiskScore={result.prediction.overall_risk_score}
              />
            </div>
          )}
        </>
      )}

      {/* Share Report Modal */}
      <ShareReportModal
        isOpen={isShareModalOpen}
        onClose={() => setIsShareModalOpen(false)}
        report={currentSavedAssessment}
        onShareUpdated={loadHistory}
      />
    </div>
  );
};
