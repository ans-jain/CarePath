import React, { useState } from 'react';
import { FeatureVectorInput, ExplainRiskRequest } from '../../types/explainability';
import { Sliders, Sparkles, ChevronDown, ChevronUp, AlertCircle, Play } from 'lucide-react';

interface PatientFormProps {
  onSubmit: (request: ExplainRiskRequest) => void;
  isLoading: boolean;
}

const PRESETS: Record<string, { label: string; desc: string; data: FeatureVectorInput }> = {
  highRisk: {
    label: 'High Risk (Hypertension & Dyslipidemia)',
    desc: 'Stage 2 systolic hypertension, impaired fasting glucose, elevated triglycerides',
    data: {
      age: 56.0,
      biological_sex: 'MALE',
      bmi: 31.5,
      smoking_status: 'CURRENT',
      systolic_bp_current: 148.0,
      diastolic_bp_current: 92.0,
      heart_rate_current: 82.0,
      fasting_glucose_current: 138.0,
      hba1c_current: 6.8,
      cholesterol_total: 230.0,
      cholesterol_hdl: 40.0,
      cholesterol_ldl: 150.0,
      triglycerides: 210.0,
      sleep_hours_current: 5.5,
      systolic_bp_ewma_delta: 7.5,
      fasting_glucose_ewma_delta: 11.2,
      systolic_bp_slope_14d: 0.65,
      fasting_glucose_slope_14d: 0.80,
    },
  },
  moderateRisk: {
    label: 'Moderate Risk (Early Metabolic Drift)',
    desc: 'Prehypertension, mild morning glucose elevation, elevated BMI',
    data: {
      age: 48.0,
      biological_sex: 'FEMALE',
      bmi: 27.2,
      smoking_status: 'NEVER',
      systolic_bp_current: 132.0,
      diastolic_bp_current: 84.0,
      heart_rate_current: 74.0,
      fasting_glucose_current: 112.0,
      hba1c_current: 5.8,
      cholesterol_total: 205.0,
      cholesterol_hdl: 48.0,
      cholesterol_ldl: 125.0,
      triglycerides: 160.0,
      sleep_hours_current: 6.5,
      systolic_bp_ewma_delta: 3.5,
      fasting_glucose_ewma_delta: 5.2,
      systolic_bp_slope_14d: 0.35,
      fasting_glucose_slope_14d: 0.45,
    },
  },
  lowRisk: {
    label: 'Low Baseline (Optimal Vitals)',
    desc: 'Normotensive blood pressure, euglycemic, optimal HDL buffer',
    data: {
      age: 32.0,
      biological_sex: 'MALE',
      bmi: 22.4,
      smoking_status: 'NEVER',
      systolic_bp_current: 116.0,
      diastolic_bp_current: 74.0,
      heart_rate_current: 64.0,
      fasting_glucose_current: 88.0,
      hba1c_current: 5.1,
      cholesterol_total: 175.0,
      cholesterol_hdl: 62.0,
      cholesterol_ldl: 92.0,
      triglycerides: 98.0,
      sleep_hours_current: 7.5,
      systolic_bp_ewma_delta: 0.0,
      fasting_glucose_ewma_delta: 0.0,
      systolic_bp_slope_14d: 0.0,
      fasting_glucose_slope_14d: 0.0,
    },
  },
};

export const PatientForm: React.FC<PatientFormProps> = ({ onSubmit, isLoading }) => {
  const [formData, setFormData] = useState<FeatureVectorInput>(PRESETS.highRisk.data);
  const [showAdvanced, setShowAdvanced] = useState<boolean>(false);
  const [topK, setTopK] = useState<number>(4);
  const [generateCounterfactuals, setGenerateCounterfactuals] = useState<boolean>(true);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const handlePresetSelect = (presetKey: string) => {
    if (PRESETS[presetKey]) {
      setFormData({ ...PRESETS[presetKey].data });
      setErrors({});
    }
  };

  const handleChange = (field: keyof FeatureVectorInput, value: any) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value === '' ? null : value,
    }));
    if (errors[field]) {
      setErrors((prev) => {
        const next = { ...prev };
        delete next[field];
        return next;
      });
    }
  };

  const validate = (): boolean => {
    const errs: Record<string, string> = {};

    if (!formData.age || formData.age < 18 || formData.age > 120) {
      errs.age = 'Age must be between 18 and 120 years';
    }
    if (!formData.bmi || formData.bmi < 10 || formData.bmi > 100) {
      errs.bmi = 'BMI must be between 10.0 and 100.0 kg/m²';
    }
    if (!formData.systolic_bp_current || formData.systolic_bp_current < 40 || formData.systolic_bp_current > 300) {
      errs.systolic_bp_current = 'Systolic BP must be between 40 and 300 mmHg';
    }
    if (!formData.diastolic_bp_current || formData.diastolic_bp_current < 30 || formData.diastolic_bp_current > 200) {
      errs.diastolic_bp_current = 'Diastolic BP must be between 30 and 200 mmHg';
    }
    if (
      formData.systolic_bp_current &&
      formData.diastolic_bp_current &&
      Number(formData.systolic_bp_current) <= Number(formData.diastolic_bp_current)
    ) {
      errs.systolic_bp_current = 'Systolic BP must exceed Diastolic BP';
    }
    if (!formData.heart_rate_current || formData.heart_rate_current < 20 || formData.heart_rate_current > 260) {
      errs.heart_rate_current = 'Resting Heart Rate must be between 20 and 260 bpm';
    }
    if (!formData.fasting_glucose_current || formData.fasting_glucose_current < 20 || formData.fasting_glucose_current > 800) {
      errs.fasting_glucose_current = 'Fasting Glucose must be between 20 and 800 mg/dL';
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    const request: ExplainRiskRequest = {
      patient_uuid: '00000000-0000-0000-0000-000000000001',
      evaluation_mode: 'LONGITUDINAL',
      features: {
        ...formData,
        age: Number(formData.age),
        bmi: Number(formData.bmi),
        systolic_bp_current: Number(formData.systolic_bp_current),
        diastolic_bp_current: Number(formData.diastolic_bp_current),
        heart_rate_current: Number(formData.heart_rate_current),
        fasting_glucose_current: Number(formData.fasting_glucose_current),
        hba1c_current: formData.hba1c_current != null ? Number(formData.hba1c_current) : null,
        cholesterol_total: formData.cholesterol_total != null ? Number(formData.cholesterol_total) : null,
        cholesterol_hdl: formData.cholesterol_hdl != null ? Number(formData.cholesterol_hdl) : null,
        cholesterol_ldl: formData.cholesterol_ldl != null ? Number(formData.cholesterol_ldl) : null,
        triglycerides: formData.triglycerides != null ? Number(formData.triglycerides) : null,
        sleep_hours_current: formData.sleep_hours_current != null ? Number(formData.sleep_hours_current) : 7.0,
        systolic_bp_ewma_delta: formData.systolic_bp_ewma_delta != null ? Number(formData.systolic_bp_ewma_delta) : 0.0,
        fasting_glucose_ewma_delta: formData.fasting_glucose_ewma_delta != null ? Number(formData.fasting_glucose_ewma_delta) : 0.0,
        systolic_bp_slope_14d: formData.systolic_bp_slope_14d != null ? Number(formData.systolic_bp_slope_14d) : 0.0,
        fasting_glucose_slope_14d: formData.fasting_glucose_slope_14d != null ? Number(formData.fasting_glucose_slope_14d) : 0.0,
      },
      top_k: topK,
      generate_counterfactuals: generateCounterfactuals,
    };

    onSubmit(request);
  };

  return (
    <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
      {/* Header & Presets Bar */}
      <div className="p-5 border-b border-slate-100 bg-slate-50/70">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <div>
            <h2 className="text-base font-bold text-slate-900">ASSESS YOUR HEALTH</h2>
            <div className="text-xs text-clinical-700 font-semibold mt-0.5">Patient Biomarker Assessment</div>
            <p className="text-xs text-slate-500 mt-0.5">
              Enter your health information to get a clear picture of your cardiometabolic risk.
            </p>
          </div>
          {/* Quick Presets */}
          <div className="flex items-center gap-1.5 flex-wrap">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider mr-1">Presets:</span>
            {Object.entries(PRESETS).map(([key, p]) => (
              <button
                key={key}
                type="button"
                onClick={() => handlePresetSelect(key)}
                className="px-2.5 py-1 text-xs font-medium rounded-md bg-white border border-slate-200 hover:border-clinical-400 hover:text-clinical-700 text-slate-700 transition-colors shadow-2xs"
                title={p.desc}
              >
                {p.label.split(' ')[0]}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Main Form */}
      <form onSubmit={handleSubmit} noValidate className="p-6 space-y-6">
        {/* Section 1: Demographics */}
        <div>
          <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-3">1. Patient Demographics</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* Age */}
            <div>
              <label htmlFor="input-age" className="block text-xs font-medium text-slate-700 mb-1">
                Age <span className="text-slate-400">(years)</span> *
              </label>
              <input
                id="input-age"
                type="number"
                step="1"
                min="18"
                max="120"
                value={formData.age ?? ''}
                onChange={(e) => handleChange('age', e.target.value)}
                className={`w-full px-3 py-2 text-sm border rounded-lg focus:ring-1 focus:ring-clinical-500 min-h-[44px] ${
                  errors.age ? 'border-rose-400 bg-rose-50/30' : 'border-slate-300'
                }`}
                required
              />
              {errors.age && <p className="text-xs text-rose-600 mt-1">{errors.age}</p>}
            </div>

            {/* Biological Sex */}
            <div>
              <label htmlFor="select-sex" className="block text-xs font-medium text-slate-700 mb-1">
                Biological Sex *
              </label>
              <select
                id="select-sex"
                value={formData.biological_sex}
                onChange={(e) => handleChange('biological_sex', e.target.value)}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-1 focus:ring-clinical-500 bg-white min-h-[44px]"
              >
                <option value="MALE">Male</option>
                <option value="FEMALE">Female</option>
              </select>
            </div>

            {/* BMI */}
            <div>
              <label htmlFor="input-bmi" className="block text-xs font-medium text-slate-700 mb-1">
                BMI <span className="text-slate-400">(kg/m²)</span> *
              </label>
              <input
                id="input-bmi"
                type="number"
                step="0.1"
                min="10"
                max="100"
                value={formData.bmi ?? ''}
                onChange={(e) => handleChange('bmi', e.target.value)}
                className={`w-full px-3 py-2 text-sm border rounded-lg focus:ring-1 focus:ring-clinical-500 min-h-[44px] ${
                  errors.bmi ? 'border-rose-400 bg-rose-50/30' : 'border-slate-300'
                }`}
                required
              />
              {errors.bmi && <p className="text-xs text-rose-600 mt-1">{errors.bmi}</p>}
            </div>

            {/* Smoking Status */}
            <div>
              <label htmlFor="select-smoking" className="block text-xs font-medium text-slate-700 mb-1">
                Smoking Status *
              </label>
              <select
                id="select-smoking"
                value={formData.smoking_status}
                onChange={(e) => handleChange('smoking_status', e.target.value)}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-1 focus:ring-clinical-500 bg-white min-h-[44px]"
              >
                <option value="NEVER">Never Smoked</option>
                <option value="FORMER">Former Smoker</option>
                <option value="CURRENT">Current Smoker</option>
              </select>
            </div>
          </div>
        </div>

        {/* Section 2: Resting Vitals */}
        <div>
          <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-3">2. Core Resting Vitals</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* Systolic BP */}
            <div>
              <label htmlFor="input-sbp" className="block text-xs font-medium text-slate-700 mb-1">
                Systolic BP <span className="text-slate-400">(mmHg)</span> *
              </label>
              <input
                id="input-sbp"
                type="number"
                step="1"
                min="40"
                max="300"
                value={formData.systolic_bp_current ?? ''}
                onChange={(e) => handleChange('systolic_bp_current', e.target.value)}
                className={`w-full px-3 py-2 text-sm border rounded-lg focus:ring-1 focus:ring-clinical-500 min-h-[44px] ${
                  errors.systolic_bp_current ? 'border-rose-400 bg-rose-50/30' : 'border-slate-300'
                }`}
                required
              />
              {errors.systolic_bp_current && <p className="text-xs text-rose-600 mt-1">{errors.systolic_bp_current}</p>}
            </div>

            {/* Diastolic BP */}
            <div>
              <label htmlFor="input-dbp" className="block text-xs font-medium text-slate-700 mb-1">
                Diastolic BP <span className="text-slate-400">(mmHg)</span> *
              </label>
              <input
                id="input-dbp"
                type="number"
                step="1"
                min="30"
                max="200"
                value={formData.diastolic_bp_current ?? ''}
                onChange={(e) => handleChange('diastolic_bp_current', e.target.value)}
                className={`w-full px-3 py-2 text-sm border rounded-lg focus:ring-1 focus:ring-clinical-500 min-h-[44px] ${
                  errors.diastolic_bp_current ? 'border-rose-400 bg-rose-50/30' : 'border-slate-300'
                }`}
                required
              />
              {errors.diastolic_bp_current && <p className="text-xs text-rose-600 mt-1">{errors.diastolic_bp_current}</p>}
            </div>

            {/* Heart Rate */}
            <div>
              <label htmlFor="input-hr" className="block text-xs font-medium text-slate-700 mb-1">
                Resting Heart Rate <span className="text-slate-400">(bpm)</span> *
              </label>
              <input
                id="input-hr"
                type="number"
                step="1"
                min="20"
                max="260"
                value={formData.heart_rate_current ?? ''}
                onChange={(e) => handleChange('heart_rate_current', e.target.value)}
                className={`w-full px-3 py-2 text-sm border rounded-lg focus:ring-1 focus:ring-clinical-500 min-h-[44px] ${
                  errors.heart_rate_current ? 'border-rose-400 bg-rose-50/30' : 'border-slate-300'
                }`}
                required
              />
              {errors.heart_rate_current && <p className="text-xs text-rose-600 mt-1">{errors.heart_rate_current}</p>}
            </div>

            {/* Fasting Glucose */}
            <div>
              <label htmlFor="input-glucose" className="block text-xs font-medium text-slate-700 mb-1">
                Fasting Glucose <span className="text-slate-400">(mg/dL)</span> *
              </label>
              <input
                id="input-glucose"
                type="number"
                step="1"
                min="20"
                max="800"
                value={formData.fasting_glucose_current ?? ''}
                onChange={(e) => handleChange('fasting_glucose_current', e.target.value)}
                className={`w-full px-3 py-2 text-sm border rounded-lg focus:ring-1 focus:ring-clinical-500 min-h-[44px] ${
                  errors.fasting_glucose_current ? 'border-rose-400 bg-rose-50/30' : 'border-slate-300'
                }`}
                required
              />
              {errors.fasting_glucose_current && <p className="text-xs text-rose-600 mt-1">{errors.fasting_glucose_current}</p>}
            </div>
          </div>
        </div>

        {/* Section 3: Lab Biomarkers */}
        <div>
          <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-3">
            3. Serum Biomarkers & Lifestyle <span className="text-slate-400 font-normal lowercase">(imputed if omitted)</span>
          </h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-3">
            {/* HbA1c */}
            <div>
              <label htmlFor="input-hba1c" className="block text-xs font-medium text-slate-700 mb-1">
                HbA1c <span className="text-slate-400">(%)</span>
              </label>
              <input
                id="input-hba1c"
                type="number"
                step="0.1"
                min="3"
                max="20"
                placeholder="e.g. 5.8"
                value={formData.hba1c_current ?? ''}
                onChange={(e) => handleChange('hba1c_current', e.target.value)}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-1 focus:ring-clinical-500 min-h-[44px]"
              />
            </div>

            {/* Total Chol */}
            <div>
              <label htmlFor="input-total-chol" className="block text-xs font-medium text-slate-700 mb-1">
                Total Chol <span className="text-slate-400">(mg/dL)</span>
              </label>
              <input
                id="input-total-chol"
                type="number"
                step="1"
                placeholder="e.g. 210"
                value={formData.cholesterol_total ?? ''}
                onChange={(e) => handleChange('cholesterol_total', e.target.value)}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-1 focus:ring-clinical-500 min-h-[44px]"
              />
            </div>

            {/* HDL */}
            <div>
              <label htmlFor="input-hdl" className="block text-xs font-medium text-slate-700 mb-1">
                HDL <span className="text-slate-400">(mg/dL)</span>
              </label>
              <input
                id="input-hdl"
                type="number"
                step="1"
                placeholder="e.g. 50"
                value={formData.cholesterol_hdl ?? ''}
                onChange={(e) => handleChange('cholesterol_hdl', e.target.value)}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-1 focus:ring-clinical-500 min-h-[44px]"
              />
            </div>

            {/* LDL */}
            <div>
              <label htmlFor="input-ldl" className="block text-xs font-medium text-slate-700 mb-1">
                LDL <span className="text-slate-400">(mg/dL)</span>
              </label>
              <input
                id="input-ldl"
                type="number"
                step="1"
                placeholder="e.g. 130"
                value={formData.cholesterol_ldl ?? ''}
                onChange={(e) => handleChange('cholesterol_ldl', e.target.value)}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-1 focus:ring-clinical-500 min-h-[44px]"
              />
            </div>

            {/* Triglycerides */}
            <div>
              <label htmlFor="input-tg" className="block text-xs font-medium text-slate-700 mb-1">
                Triglycerides <span className="text-slate-400">(mg/dL)</span>
              </label>
              <input
                id="input-tg"
                type="number"
                step="1"
                placeholder="e.g. 150"
                value={formData.triglycerides ?? ''}
                onChange={(e) => handleChange('triglycerides', e.target.value)}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-1 focus:ring-clinical-500 min-h-[44px]"
              />
            </div>

            {/* Sleep Hours */}
            <div>
              <label htmlFor="input-sleep" className="block text-xs font-medium text-slate-700 mb-1">
                Sleep <span className="text-slate-400">(hrs/night)</span>
              </label>
              <input
                id="input-sleep"
                type="number"
                step="0.5"
                min="1"
                max="24"
                placeholder="e.g. 7.0"
                value={formData.sleep_hours_current ?? ''}
                onChange={(e) => handleChange('sleep_hours_current', e.target.value)}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-1 focus:ring-clinical-500 min-h-[44px]"
              />
            </div>
          </div>
        </div>

        {/* Section 4: Advanced Longitudinal Trend Metrics (Accordion) */}
        <div className="pt-2 border-t border-slate-100">
          <button
            type="button"
            onClick={() => setShowAdvanced(!showAdvanced)}
            className="flex items-center gap-1.5 text-xs font-semibold text-slate-600 hover:text-slate-900 transition-colors"
          >
            {showAdvanced ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
            <span>4. Advanced Longitudinal Baseline & Trend Dynamics</span>
            <span className="text-[11px] text-slate-400 font-normal">(30-day EWMA deltas & 14-day OLS slopes)</span>
          </button>

          {showAdvanced && (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mt-3 pt-3 bg-slate-50/60 p-4 rounded-lg border border-slate-200">
              <div>
                <label htmlFor="input-sbp-ewma" className="block text-xs font-medium text-slate-700 mb-1">
                  Systolic BP EWMA Δ <span className="text-slate-400">(mmHg)</span>
                </label>
                <input
                  id="input-sbp-ewma"
                  type="number"
                  step="0.1"
                  value={formData.systolic_bp_ewma_delta ?? ''}
                  onChange={(e) => handleChange('systolic_bp_ewma_delta', e.target.value)}
                  className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-1 focus:ring-clinical-500 bg-white min-h-[44px]"
                />
              </div>

              <div>
                <label htmlFor="input-glu-ewma" className="block text-xs font-medium text-slate-700 mb-1">
                  Glucose EWMA Δ <span className="text-slate-400">(mg/dL)</span>
                </label>
                <input
                  id="input-glu-ewma"
                  type="number"
                  step="0.1"
                  value={formData.fasting_glucose_ewma_delta ?? ''}
                  onChange={(e) => handleChange('fasting_glucose_ewma_delta', e.target.value)}
                  className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-1 focus:ring-clinical-500 bg-white min-h-[44px]"
                />
              </div>

              <div>
                <label htmlFor="input-sbp-slope" className="block text-xs font-medium text-slate-700 mb-1">
                  Systolic 14d Slope <span className="text-slate-400">(mmHg/day)</span>
                </label>
                <input
                  id="input-sbp-slope"
                  type="number"
                  step="0.05"
                  value={formData.systolic_bp_slope_14d ?? ''}
                  onChange={(e) => handleChange('systolic_bp_slope_14d', e.target.value)}
                  className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-1 focus:ring-clinical-500 bg-white min-h-[44px]"
                />
              </div>

              <div>
                <label htmlFor="input-glu-slope" className="block text-xs font-medium text-slate-700 mb-1">
                  Glucose 14d Slope <span className="text-slate-400">(mg/dL/day)</span>
                </label>
                <input
                  id="input-glu-slope"
                  type="number"
                  step="0.05"
                  value={formData.fasting_glucose_slope_14d ?? ''}
                  onChange={(e) => handleChange('fasting_glucose_slope_14d', e.target.value)}
                  className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-1 focus:ring-clinical-500 bg-white min-h-[44px]"
                />
              </div>
            </div>
          )}
        </div>

        {/* Explainability Parameters & Submit */}
        <div className="pt-4 border-t border-slate-100 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="flex items-center gap-6">
            <div className="flex items-center gap-2">
              <label htmlFor="select-topk" className="text-xs font-medium text-slate-600">
                SHAP Top Drivers:
              </label>
              <select
                id="select-topk"
                value={topK}
                onChange={(e) => setTopK(Number(e.target.value))}
                className="px-2 py-1 text-xs border border-slate-300 rounded-md bg-white"
              >
                <option value={3}>Top 3</option>
                <option value={4}>Top 4</option>
                <option value={5}>Top 5</option>
                <option value={8}>Top 8</option>
              </select>
            </div>

            <label className="flex items-center gap-2 text-xs font-medium text-slate-600 cursor-pointer">
              <input
                type="checkbox"
                checked={generateCounterfactuals}
                onChange={(e) => setGenerateCounterfactuals(e.target.checked)}
                className="w-4 h-4 rounded text-clinical-600 border-slate-300 focus:ring-clinical-500"
              />
              <span>Generate Actionable Counterfactuals</span>
            </label>
          </div>

          <button
            type="submit"
            disabled={isLoading}
            className="inline-flex items-center justify-center gap-2 px-6 py-2.5 bg-clinical-600 hover:bg-clinical-700 disabled:bg-slate-300 text-white text-sm font-semibold rounded-lg shadow-sm transition-colors min-h-[48px]"
          >
            <Play className="w-4 h-4 fill-current" />
            <span>{isLoading ? 'Evaluating Risk & SHAP...' : 'Run Risk Assessment'}</span>
          </button>
        </div>
      </form>
    </div>
  );
};
