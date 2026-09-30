import React, { useState } from 'react';
import { ShapExplanation, FeatureAttribution } from '../../types/explainability';
import { TrendingUp, TrendingDown, Info, BarChart3, HelpCircle } from 'lucide-react';

interface ShapWaterfallProps {
  explanation: ShapExplanation;
  onOpenHowItWorks?: () => void;
}

export const ShapWaterfall: React.FC<ShapWaterfallProps> = ({
  explanation,
  onOpenHowItWorks,
}) => {
  const [activeTab, setActiveTab] = useState<'drivers' | 'protective' | 'all'>('drivers');

  const {
    top_risk_drivers = [],
    protective_factors = [],
    summary_narrative = '',
    base_value = 0,
  } = explanation || {};

  // Find max absolute SHAP value for proportional bar scaling
  const allAttributions = [...top_risk_drivers, ...protective_factors];
  const maxAbsShap = Math.max(0.1, ...allAttributions.map((a) => Math.abs(a.shap_value || 0)));

  return (
    <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
      {/* Header */}
      <div className="p-6 border-b border-slate-100 bg-slate-50/50">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <div className="flex items-center gap-2.5">
            <div className="flex items-center justify-center w-8 h-8 rounded-lg bg-blue-100 text-blue-700">
              <BarChart3 className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">Why Did the Model Predict This Risk Score?</h3>
              <p className="text-xs text-slate-500">
                TreeSHAP feature attributions decomposing individual patient biomarker contributions.
              </p>
            </div>
          </div>

          {onOpenHowItWorks && (
            <button
              onClick={onOpenHowItWorks}
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-clinical-600 hover:text-clinical-800 transition-colors"
            >
              <HelpCircle className="w-3.5 h-3.5" />
              <span>Attribution Guide</span>
            </button>
          )}
        </div>

        {/* Narrative Summary */}
        {summary_narrative && (
          <div className="mt-4 p-3 bg-white border border-slate-200 rounded-lg text-xs text-slate-700 leading-relaxed shadow-2xs">
            <span className="font-semibold text-slate-900">Decision Summary: </span>
            {summary_narrative}
          </div>
        )}
      </div>

      {/* Visual Attributions Section */}
      <div className="p-6 space-y-6">
        {/* Visual Diverging Bar Representation (Step 5) */}
        <div>
          <div className="flex items-center justify-between text-xs font-semibold text-slate-500 mb-3">
            <span className="flex items-center gap-1 text-rose-700">
              <TrendingUp className="w-3.5 h-3.5" /> Increases Modeled Risk (φ &gt; 0)
            </span>
            <span className="text-slate-400 font-normal">Reference Baseline: {base_value.toFixed(4)}</span>
            <span className="flex items-center gap-1 text-emerald-700">
              <TrendingDown className="w-3.5 h-3.5" /> Decreases Modeled Risk (φ &lt; 0)
            </span>
          </div>

          {/* Bar List */}
          <div className="space-y-3 pt-2">
            {allAttributions.map((attr) => {
              const isRisk = attr.direction === 'increases_risk';
              const widthPct = Math.min(100, Math.max(8, (Math.abs(attr.shap_value) / maxAbsShap) * 100));

              return (
                <div key={attr.feature} className="space-y-1">
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-medium text-slate-800 flex items-center gap-1.5">
                      {isRisk ? (
                        <TrendingUp className="w-3.5 h-3.5 text-rose-500" aria-label="Increases risk" />
                      ) : (
                        <TrendingDown className="w-3.5 h-3.5 text-emerald-500" aria-label="Decreases risk" />
                      )}
                      <span>{attr.display_name}</span>
                      <span className="text-slate-400 font-mono text-[11px]">({String(attr.value ?? '—')})</span>
                    </span>
                    <span
                      className={`font-mono text-xs font-semibold ${
                        isRisk ? 'text-rose-600' : 'text-emerald-600'
                      }`}
                    >
                      {attr.shap_value > 0 ? `+${attr.shap_value.toFixed(4)}` : attr.shap_value.toFixed(4)}
                    </span>
                  </div>

                  {/* Relative bar meter */}
                  <div className="w-full bg-slate-100 h-2.5 rounded-full overflow-hidden flex">
                    {isRisk ? (
                      <div
                        className="h-full bg-rose-500 rounded-full transition-all duration-500"
                        style={{ width: `${widthPct}%` }}
                        title={`${attr.display_name}: +${attr.shap_value}`}
                      />
                    ) : (
                      <div
                        className="h-full bg-emerald-500 rounded-full transition-all duration-500"
                        style={{ width: `${widthPct}%` }}
                        title={`${attr.display_name}: ${attr.shap_value}`}
                      />
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Detailed Cards: Risk Drivers & Protective Factors */}
        <div className="pt-6 border-t border-slate-100 grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* Top Risk Drivers */}
          <div className="p-4 bg-rose-50/40 border border-rose-100 rounded-xl space-y-3">
            <div className="flex items-center justify-between">
              <h4 className="text-xs font-bold uppercase tracking-wider text-rose-900 flex items-center gap-1.5">
                <TrendingUp className="w-4 h-4 text-rose-600" />
                Primary Risk Drivers
              </h4>
              <span className="text-[11px] font-semibold text-rose-700 bg-rose-100/80 px-2 py-0.5 rounded">
                {top_risk_drivers.length} factors
              </span>
            </div>

            {top_risk_drivers.length === 0 ? (
              <p className="text-xs text-slate-500 italic">No significant risk drivers identified above baseline.</p>
            ) : (
              <div className="space-y-2.5">
                {top_risk_drivers.map((driver) => (
                  <div key={driver.feature} className="p-3 bg-white border border-rose-200/60 rounded-lg text-xs shadow-2xs">
                    <div className="flex items-start justify-between gap-2 mb-1">
                      <span className="font-semibold text-slate-900">{driver.display_name}</span>
                      <span className="px-1.5 py-0.5 text-[10px] font-bold uppercase tracking-wider rounded bg-rose-100 text-rose-800 shrink-0">
                        {driver.impact} impact
                      </span>
                    </div>
                    <div className="flex items-baseline justify-between text-slate-500 text-[11px] mb-1">
                      <span>Observed: <strong className="text-slate-800">{String(driver.value ?? '—')}</strong></span>
                      <span className="font-mono text-rose-600 font-semibold">+{driver.shap_value.toFixed(4)}</span>
                    </div>
                    <p className="text-slate-600 text-[11px] leading-relaxed">{driver.narrative}</p>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Protective Factors */}
          <div className="p-4 bg-emerald-50/40 border border-emerald-100 rounded-xl space-y-3">
            <div className="flex items-center justify-between">
              <h4 className="text-xs font-bold uppercase tracking-wider text-emerald-900 flex items-center gap-1.5">
                <TrendingDown className="w-4 h-4 text-emerald-600" />
                Protective / Buffering Factors
              </h4>
              <span className="text-[11px] font-semibold text-emerald-700 bg-emerald-100/80 px-2 py-0.5 rounded">
                {protective_factors.length} factors
              </span>
            </div>

            {protective_factors.length === 0 ? (
              <p className="text-xs text-slate-500 italic">No protective factors identified below baseline.</p>
            ) : (
              <div className="space-y-2.5">
                {protective_factors.map((factor) => (
                  <div key={factor.feature} className="p-3 bg-white border border-emerald-200/60 rounded-lg text-xs shadow-2xs">
                    <div className="flex items-start justify-between gap-2 mb-1">
                      <span className="font-semibold text-slate-900">{factor.display_name}</span>
                      <span className="px-1.5 py-0.5 text-[10px] font-bold uppercase tracking-wider rounded bg-emerald-100 text-emerald-800 shrink-0">
                        {factor.impact} buffer
                      </span>
                    </div>
                    <div className="flex items-baseline justify-between text-slate-500 text-[11px] mb-1">
                      <span>Observed: <strong className="text-slate-800">{String(factor.value ?? '—')}</strong></span>
                      <span className="font-mono text-emerald-600 font-semibold">{factor.shap_value.toFixed(4)}</span>
                    </div>
                    <p className="text-slate-600 text-[11px] leading-relaxed">{factor.narrative}</p>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Clinical Guardrail Disclaimer */}
        <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg text-xs text-slate-500 flex items-start gap-2">
          <Info className="w-4 h-4 text-slate-400 mt-0.5 shrink-0" />
          <span>
            <strong>SHAP Interpretability Notice:</strong> Feature contributions describe how the trained model attributes
            the risk prediction relative to the population baseline. They indicate mathematical model influence and do
            <strong> not</strong> establish biological cause-and-effect.
          </span>
        </div>
      </div>
    </div>
  );
};
