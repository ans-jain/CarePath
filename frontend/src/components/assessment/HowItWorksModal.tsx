import React from 'react';
import { X, Activity, Cpu, Sparkles, Sliders, ShieldCheck } from 'lucide-react';

interface HowItWorksModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const HowItWorksModal: React.FC<HowItWorksModalProps> = ({ isOpen, onClose }) => {
  if (!isOpen) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="how-it-works-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs"
    >
      <div className="relative w-full max-w-2xl bg-white rounded-xl shadow-xl border border-slate-200 overflow-hidden max-h-[90vh] flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 bg-slate-50/50">
          <div className="flex items-center gap-2.5">
            <Cpu className="w-5 h-5 text-clinical-600" />
            <h2 id="how-it-works-title" className="text-lg font-bold text-slate-900">
              How CarePath Explainable AI Works
            </h2>
          </div>
          <button
            onClick={onClose}
            className="p-1 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-100 transition-colors"
            aria-label="Close dialog"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 overflow-y-auto space-y-6 text-sm text-slate-600">
          {/* Section 1 */}
          <div className="flex items-start gap-3.5">
            <div className="w-8 h-8 rounded-lg bg-blue-100 text-blue-700 flex items-center justify-center shrink-0 mt-0.5">
              <Activity className="w-4 h-4" />
            </div>
            <div>
              <h3 className="font-semibold text-slate-900 text-sm mb-1">1. Calibrated Risk Prediction</h3>
              <p className="leading-relaxed">
                CarePath ingests an 18-dimensional biometric feature vector combining demographics, resting vitals,
                lipid panels, and 30-day longitudinal trends. A Gradient Boosted decision tree ensemble outputs a
                continuous probability score calibrated via Platt scaling, mapping into standard clinical decision-support
                tiers (<strong>Low</strong>, <strong>Moderate</strong>, <strong>Elevated</strong>, and <strong>High</strong>).
              </p>
            </div>
          </div>

          {/* Section 2 */}
          <div className="flex items-start gap-3.5">
            <div className="w-8 h-8 rounded-lg bg-purple-100 text-purple-700 flex items-center justify-center shrink-0 mt-0.5">
              <Sparkles className="w-4 h-4" />
            </div>
            <div>
              <h3 className="font-semibold text-slate-900 text-sm mb-1">2. SHAP (TreeExplainer) Attributions</h3>
              <p className="leading-relaxed">
                Rather than treating the ML model as a black box, CarePath calculates exact Shapley additive explanations
                (SHAP). Features with positive SHAP values (<strong>Risk Drivers</strong>) increase the predicted risk
                relative to cohort baselines, while negative values (<strong>Protective Factors</strong>) pull the prediction
                lower.
              </p>
              <div className="mt-2 p-2.5 bg-slate-50 border border-slate-200 rounded text-xs text-slate-500">
                <strong>Non-Causal Note:</strong> SHAP explains mathematical model decision weighting; it does not prove clinical causality.
              </div>
            </div>
          </div>

          {/* Section 3 */}
          <div className="flex items-start gap-3.5">
            <div className="w-8 h-8 rounded-lg bg-emerald-100 text-emerald-700 flex items-center justify-center shrink-0 mt-0.5">
              <Sliders className="w-4 h-4" />
            </div>
            <div>
              <h3 className="font-semibold text-slate-900 text-sm mb-1">3. Constrained Counterfactual Targets</h3>
              <p className="leading-relaxed">
                To identify actionable next steps, the engine evaluates realistic, modifiable lifestyle targets
                (such as blood pressure normalisation, glycemic control, or sleep regularisation) within strict
                physiological safety clamps. Immutable features like age or sex are never manipulated.
              </p>
            </div>
          </div>

          {/* Section 4: Clinical Guardrail */}
          <div className="flex items-start gap-3.5 p-3.5 bg-amber-50/70 border border-amber-200/80 rounded-lg">
            <ShieldCheck className="w-5 h-5 text-amber-700 shrink-0 mt-0.5" />
            <div>
              <h3 className="font-semibold text-amber-950 text-xs mb-0.5">Clinical Decision Support Guardrail</h3>
              <p className="text-xs text-amber-900 leading-relaxed">
                CarePath is a decision-support and trend-monitoring tool. Outputs represent statistical risk estimates,
                not medical diagnoses or pharmaceutical prescriptions. Always consult a licensed healthcare professional
                for clinical decisions.
              </p>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-3.5 border-t border-slate-100 bg-slate-50/50 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium text-white bg-clinical-600 hover:bg-clinical-700 rounded-lg transition-colors"
          >
            Got It
          </button>
        </div>
      </div>
    </div>
  );
};
