import React, { useEffect, useState } from 'react';
import { mlService } from '../../services/mlService';
import { MlServiceHealth } from '../../types/explainability';
import { CheckCircle2, AlertTriangle, XCircle, RefreshCw } from 'lucide-react';

export const SystemHealthBadge: React.FC = () => {
  const [health, setHealth] = useState<MlServiceHealth | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  const fetchHealth = async () => {
    setLoading(true);
    try {
      const data = await mlService.getHealth();
      setHealth(data);
    } catch {
      setHealth({
        status: 'UNHEALTHY',
        service_name: 'CarePath ML Risk Service',
        service_version: '1.0.0',
        modelVersion: 'NONE',
        model_loaded: false,
        calibrated: false,
        featuresSupported: 18
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchHealth();
    const interval = setInterval(fetchHealth, 30000);
    return () => clearInterval(interval);
  }, []);

  if (loading && !health) {
    return (
      <div className="flex items-center gap-1.5 px-2.5 py-1 text-xs font-medium text-slate-500 bg-slate-100 rounded-full animate-pulse">
        <RefreshCw className="w-3 h-3 animate-spin" />
        <span>Checking ML Engine...</span>
      </div>
    );
  }

  if (health?.status === 'HEALTHY' && health.model_loaded) {
    return (
      <div
        className="flex items-center gap-1.5 px-2.5 py-1 text-xs font-medium text-emerald-700 bg-emerald-50 border border-emerald-200 rounded-full"
        title={`Model: ${health.modelVersion} | Calibrated: ${health.calibrated} | SHAP: ${health.shapExplainerType || 'TreeExplainer'}`}
      >
        <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
        <span>Online</span>
      </div>
    );
  }

  if (health?.status === 'DEGRADED') {
    return (
      <div className="flex items-center gap-1.5 px-2.5 py-1 text-xs font-medium text-amber-700 bg-amber-50 border border-amber-200 rounded-full">
        <AlertTriangle className="w-3.5 h-3.5 text-amber-600" />
        <span>Offline</span>
      </div>
    );
  }

  return (
    <div
      onClick={fetchHealth}
      className="flex items-center gap-1.5 px-2.5 py-1 text-xs font-medium text-rose-700 bg-rose-50 border border-rose-200 rounded-full cursor-pointer hover:bg-rose-100 transition-colors"
      title="Click to re-check connection"
    >
      <XCircle className="w-3.5 h-3.5 text-rose-600" />
      <span>Retry!</span>
    </div>
  );
};
