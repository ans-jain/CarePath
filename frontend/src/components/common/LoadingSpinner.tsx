import React from 'react';
import { Activity } from 'lucide-react';

interface LoadingSpinnerProps {
  label?: string;
  subLabel?: string;
}

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({
  label = 'Computing Cardiometabolic Risk...',
  subLabel = 'Analyzing your health data and generating personalized insights...',
}) => {
  return (
    <div
      role="status"
      aria-live="polite"
      className="flex flex-col items-center justify-center p-12 bg-white rounded-xl border border-slate-200 shadow-sm text-center"
    >
      <div className="relative flex items-center justify-center w-16 h-16 mb-4">
        <div className="absolute inset-0 rounded-full border-4 border-clinical-100 border-t-clinical-600 animate-spin" />
        <Activity className="w-7 h-7 text-clinical-600 animate-pulse" />
      </div>
      <h3 className="text-base font-semibold text-slate-800 mb-1">{label}</h3>
      <p className="text-xs text-slate-500 max-w-md">{subLabel}</p>
      <span className="sr-only">Loading risk assessment...</span>
    </div>
  );
};
