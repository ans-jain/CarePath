import React from 'react';
import { PredictRiskResponse, RiskTier } from '../../types/explainability';
import { ShieldCheck, Activity, Award, Clock } from 'lucide-react';

interface RiskSummaryCardProps {
  prediction: PredictRiskResponse;
  assessmentTimestamp?: string;
}

const TIER_CONFIG: Record<
  RiskTier,
  { label: string; badgeClass: string; barColor: string; description: string }
> = {
  LOW: {
    label: 'Low Cardiometabolic Risk',
    badgeClass: 'bg-emerald-50 text-emerald-800 border-emerald-200',
    barColor: 'bg-emerald-500',
    description: 'Patient biomarkers reside comfortably within empirical reference cohort ranges.',
  },
  MODERATE: {
    label: 'Moderate Risk',
    badgeClass: 'bg-amber-50 text-amber-800 border-amber-200',
    barColor: 'bg-amber-500',
    description: 'Early metabolic or vascular drift detected; monitored lifestyle support recommended.',
  },
  ELEVATED: {
    label: 'Elevated Risk',
    badgeClass: 'bg-orange-50 text-orange-800 border-orange-200',
    barColor: 'bg-orange-500',
    description: 'Multiple biometric signals deviate significantly from personal baseline corridors.',
  },
  HIGH: {
    label: 'High Risk',
    badgeClass: 'bg-rose-50 text-rose-800 border-rose-200',
    barColor: 'bg-rose-500',
    description: 'High statistical probability of cardiometabolic decompensation within 3-year horizon.',
  },
};

export const RiskSummaryCard: React.FC<RiskSummaryCardProps> = ({
  prediction,
  assessmentTimestamp = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
}) => {
  const tier: RiskTier = (prediction.risk_category || prediction.risk_tier || 'MODERATE') as RiskTier;
  const config = TIER_CONFIG[tier] || TIER_CONFIG.MODERATE;
  const scorePercent = (prediction.overall_risk_score * 100).toFixed(1);

  return (
    <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
      {/* Tier & Score Header */}
      <div className="p-6">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1.5">
              <span className={`inline-flex items-center gap-1.5 px-3 py-1 text-xs font-bold rounded-full border ${config.badgeClass}`}>
                <span className="w-2 h-2 rounded-full bg-current" />
                {config.label}
              </span>
              {prediction.confidence_level && (
                <span className="px-2.5 py-0.5 text-[11px] font-medium bg-slate-100 text-slate-700 rounded-full border border-slate-200">
                  {prediction.confidence_level === 'LONGITUDINAL_ROBUST' ? 'Longitudinal Robust' : 'Preliminary Intake'}
                </span>
              )}
            </div>
            <p className="text-sm text-slate-600 max-w-xl">{config.description}</p>
          </div>

          {/* Numeric Score Callout */}
          <div className="flex items-baseline gap-3 p-4 bg-slate-50 rounded-xl border border-slate-100 shrink-0">
            <div>
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block">Risk Signal</span>
              <div className="flex items-baseline gap-1">
                <span className="text-3xl font-extrabold text-slate-900 tracking-tight">{prediction.overall_risk_score.toFixed(4)}</span>
                <span className="text-sm font-semibold text-slate-500">({scorePercent}%)</span>
              </div>
            </div>
          </div>
        </div>

        {/* Continuous Risk Score Gauge / Bar */}
        <div className="mt-6 pt-5 border-t border-slate-100">
          <div className="flex justify-between text-xs font-medium text-slate-500 mb-1.5">
            <span>Model Risk Scale [0.00 – 1.00]</span>
            <span className="font-semibold text-slate-700">{scorePercent}% Calibrated Posterior Probability</span>
          </div>
          <div className="relative w-full h-3 bg-slate-100 rounded-full overflow-hidden">
            {/* Visual scale gradients */}
            <div
              className={`h-full ${config.barColor} transition-all duration-700 ease-out rounded-full`}
              style={{ width: `${Math.min(100, Math.max(0, prediction.overall_risk_score * 100))}%` }}
              role="progressbar"
              aria-valuenow={prediction.overall_risk_score}
              aria-valuemin={0}
              aria-valuemax={1}
            />
          </div>
          {/* Tier scale indicators */}
          <div className="flex justify-between text-[10px] text-slate-400 mt-1 font-mono">
            <span>0.0 (Low)</span>
            <span>0.25 (Moderate)</span>
            <span>0.50 (Elevated)</span>
            <span>0.75 (High)</span>
            <span>1.0</span>
          </div>
        </div>
      </div>

      {/* Metadata Footer */}
      <div className="px-6 py-3 bg-slate-50/70 border-t border-slate-100 flex flex-wrap items-center justify-between gap-3 text-xs text-slate-500">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-1.5" title="Model Identifier">
            <Activity className="w-3.5 h-3.5 text-slate-400" />
            <span>Model: <code className="font-mono text-slate-700">{prediction.model_version}</code></span>
          </div>
          <div className="flex items-center gap-1.5" title="Calibration Technique">
            <ShieldCheck className="w-3.5 h-3.5 text-emerald-500" />
            <span>Calibrated via Sigmoid / Platt Scaling</span>
          </div>
        </div>

        <div className="flex items-center gap-1.5 text-slate-400">
          <Clock className="w-3.5 h-3.5" />
          <span>Evaluated at {assessmentTimestamp}</span>
        </div>
      </div>
    </div>
  );
};
