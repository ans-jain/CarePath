import React from 'react';
import { ShieldAlert, AlertCircle, Info, RefreshCcw } from 'lucide-react';

interface AlertBannerProps {
  type?: 'disclaimer' | 'error' | 'warning' | 'info';
  title?: string;
  message: string;
  onRetry?: () => void;
  className?: string;
}

export const AlertBanner: React.FC<AlertBannerProps> = ({
  type = 'disclaimer',
  title,
  message,
  onRetry,
  className = '',
}) => {
  if (type === 'error') {
    return (
      <div
        role="alert"
        className={`p-4 bg-rose-50 border border-rose-200 rounded-lg text-rose-800 ${className}`}
      >
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-start gap-2.5">
            <AlertCircle className="w-5 h-5 text-rose-600 mt-0.5 shrink-0" aria-hidden="true" />
            <div>
              {title && <h3 className="font-semibold text-rose-900 text-sm">{title}</h3>}
              <p className="text-sm text-rose-700 mt-0.5">{message}</p>
            </div>
          </div>
          {onRetry && (
            <button
              onClick={onRetry}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-rose-700 bg-rose-100 hover:bg-rose-200 rounded-md transition-colors shrink-0"
              aria-label="Retry operation"
            >
              <RefreshCcw className="w-3.5 h-3.5" />
              <span>Retry</span>
            </button>
          )}
        </div>
      </div>
    );
  }

  if (type === 'warning') {
    return (
      <div
        role="alert"
        className={`p-4 bg-amber-50 border border-amber-200 rounded-lg text-amber-800 ${className}`}
      >
        <div className="flex items-start gap-2.5">
          <AlertCircle className="w-5 h-5 text-amber-600 mt-0.5 shrink-0" aria-hidden="true" />
          <div>
            {title && <h3 className="font-semibold text-amber-900 text-sm">{title}</h3>}
            <p className="text-sm text-amber-700 mt-0.5">{message}</p>
          </div>
        </div>
      </div>
    );
  }

  // Clinical non-diagnostic disclaimer
  return (
    <div
      role="region"
      aria-label="Clinical Disclaimer"
      className={`p-3.5 bg-blue-50/80 border border-blue-200/80 rounded-lg text-blue-900 ${className}`}
    >
      <div className="flex items-start gap-2.5">
        <ShieldAlert className="w-4 h-4 text-blue-700 mt-0.5 shrink-0" aria-hidden="true" />
        <div className="text-xs text-blue-800 leading-relaxed">
          <span className="font-semibold text-blue-950">Clinical Decision Support Guardrail: </span>
          {message}
        </div>
      </div>
    </div>
  );
};
