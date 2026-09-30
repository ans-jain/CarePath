import React from 'react';
import { CounterfactualResult } from '../../types/explainability';
import { Sliders, ArrowRight, CheckCircle2, AlertCircle, Sparkles, ShieldAlert } from 'lucide-react';

interface CounterfactualCardsProps {
  counterfactuals: CounterfactualResult;
  originalRiskScore: number;
}

export const CounterfactualCards: React.FC<CounterfactualCardsProps> = ({
  counterfactuals,
  originalRiskScore,
}) => {
  const { counterfactual_available, status, message, plans } = counterfactuals;

  return (
    <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
      {/* Header */}
      <div className="p-6 border-b border-slate-100 bg-slate-50/50">
        <div className="flex items-center gap-2.5">
          <div className="flex items-center justify-center w-8 h-8 rounded-lg bg-emerald-100 text-emerald-700">
            <Sliders className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-base font-bold text-slate-900">
              Potential Ways the Model's Risk Estimate Could Change
            </h3>
            <p className="text-xs text-slate-500">
              Actionable, physiologically constrained target scenarios simulated through the trained decision model.
            </p>
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="p-6 space-y-6">
        {/* If no counterfactual available */}
        {(!counterfactual_available || status === 'NO_VALID_COUNTERFACTUAL_FOUND' || plans.length === 0) ? (
          <div className="p-6 bg-slate-50 border border-slate-200 rounded-xl text-center space-y-2">
            <div className="inline-flex items-center justify-center w-10 h-10 rounded-full bg-slate-100 text-slate-500 mb-1">
              <CheckCircle2 className="w-5 h-5" />
            </div>
            <h4 className="text-sm font-semibold text-slate-800">
              No Valid Constrained Counterfactual Found
            </h4>
            <p className="text-xs text-slate-500 max-w-md mx-auto leading-relaxed">
              {message ||
                "Patient is currently within the low baseline tier, or no realistic single- or multi-factor biometric adjustment produced a significant risk reduction under clinical safety clamps."}
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
            {plans.map((plan, idx) => {
              const reductionPercent = (plan.risk_reduction * 100).toFixed(1);
              const simulatedPercent = (plan.simulated_risk_score * 100).toFixed(1);

              return (
                <div
                  key={plan.plan_id || idx}
                  className="flex flex-col justify-between p-5 bg-white border border-emerald-100 rounded-xl shadow-xs hover:border-emerald-300 transition-colors"
                >
                  <div className="space-y-4">
                    {/* Plan Header */}
                    <div>
                      <div className="flex items-center justify-between gap-2 mb-1">
                        <span className="text-[11px] font-bold uppercase tracking-wider text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200">
                          Scenario #{idx + 1}
                        </span>
                        {plan.tier_transition_achieved && (
                          <span className="inline-flex items-center gap-1 text-[10px] font-bold uppercase tracking-wider text-clinical-700 bg-clinical-50 px-2 py-0.5 rounded border border-clinical-200">
                            <Sparkles className="w-3 h-3 text-clinical-600" />
                            Tier Transition
                          </span>
                        )}
                      </div>
                      <h4 className="text-sm font-bold text-slate-900 mt-1">{plan.title}</h4>
                    </div>

                    {/* Simulated Risk Reduction Callout */}
                    <div className="p-3 bg-emerald-50/60 rounded-lg border border-emerald-100">
                      <div className="flex items-baseline justify-between">
                        <span className="text-xs text-slate-600">Simulated Risk:</span>
                        <div className="flex items-baseline gap-1.5">
                          <span className="text-sm font-bold text-slate-900">
                            {plan.simulated_risk_score.toFixed(4)}
                          </span>
                          <span className="text-xs text-slate-500">({simulatedPercent}%)</span>
                          <span className="text-xs font-bold text-emerald-700">
                            (-{reductionPercent}%)
                          </span>
                        </div>
                      </div>
                      <div className="flex justify-between items-center text-[11px] text-slate-500 mt-1">
                        <span>Target Category:</span>
                        <span className="font-semibold text-emerald-800">{plan.simulated_risk_category}</span>
                      </div>
                    </div>

                    {/* Changed Features List */}
                    <div className="space-y-2">
                      <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
                        Modeled Targets ({plan.changed_features.length} modifiable):
                      </span>
                      {plan.changed_features.map((cf) => (
                        <div
                          key={cf.feature}
                          className="p-2.5 bg-slate-50 rounded-lg border border-slate-200/80 text-xs space-y-1"
                        >
                          <div className="font-medium text-slate-800 flex items-center justify-between">
                            <span>{cf.display_name}</span>
                            <span className="font-mono text-emerald-600 font-semibold">
                              {cf.change_delta != null
                                ? (cf.change_delta > 0 ? `+${cf.change_delta}` : `${cf.change_delta}`)
                                : 'modified'} {cf.unit || ''}
                            </span>
                          </div>
                          <div className="flex items-center gap-2 text-slate-500 text-[11px]">
                            <span className="font-mono text-slate-700">{String(cf.current_value)}</span>
                            <ArrowRight className="w-3 h-3 text-slate-400" />
                            <span className="font-mono font-semibold text-emerald-700">
                              {String(cf.target_value)} {cf.unit || ''}
                            </span>
                          </div>
                        </div>
                      ))}
                    </div>

                    {/* Rationale */}
                    <p className="text-[11px] text-slate-600 leading-relaxed italic">
                      "{plan.rationale}"
                    </p>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* Disclaimer Notice (Step 7) */}
        <div className="p-4 bg-amber-50/70 border border-amber-200/80 rounded-lg text-xs text-amber-900 flex items-start gap-2.5">
          <ShieldAlert className="w-4 h-4 text-amber-700 shrink-0 mt-0.5" />
          <div className="leading-relaxed">
            <strong className="font-semibold text-amber-950">Important Clinical Disclaimer: </strong>
            These scenarios describe how changes in model inputs may affect the model's statistical prediction.
            They are model-generated simulations, <strong>not</strong> medical advice, prescriptions, or clinical orders.
            Consult a licensed physician before making lifestyle or medical changes.
          </div>
        </div>
      </div>
    </div>
  );
};
