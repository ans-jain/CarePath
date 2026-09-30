import { requestJson, BACKEND_BASE_URL } from './api';
import { SavedAssessment, ExplainRiskRequest, ExplainRiskResponse, RiskTier } from '../types/explainability';

function getStorageKey(patientId?: string): string {
  return `carepath_assessment_history_${patientId || 'default'}`;
}

export const assessmentService = {
  /**
   * Save an assessment to the patient profile history.
   * Persists to both backend PostgreSQL risk_assessments and client localStorage.
   */
  async saveAssessment(assessment: SavedAssessment): Promise<SavedAssessment> {
    const key = getStorageKey(assessment.patientId);

    // 1. Save locally first for instant availability
    try {
      const existing = this.getLocalHistory(assessment.patientId);
      // Prevent duplicates with same ID
      const filtered = existing.filter((item) => item.id !== assessment.id);
      const updated = [assessment, ...filtered];
      localStorage.setItem(key, JSON.stringify(updated));
    } catch (err) {
      console.warn('[AssessmentService] Could not write to localStorage:', err);
    }

    // 2. Persist to backend database if session token exists
    try {
      const payload = {
        modelVersion: assessment.modelVersion || 'calibrated_v1.0.0',
        overallRiskScore: assessment.overallRiskScore,
        riskCategory: assessment.riskCategory,
        confidenceLevel: assessment.confidenceLevel || 'LONGITUDINAL_ROBUST',
        featureSnapshot: JSON.stringify({
          request: assessment.request,
          response: assessment.response,
          timestamp: assessment.timestamp,
          patientName: assessment.patientName,
        }),
      };

      const backendUrl = assessment.patientId
        ? `${BACKEND_BASE_URL}/risk-assessments?patientId=${assessment.patientId}`
        : `${BACKEND_BASE_URL}/risk-assessments`;

      await requestJson(backendUrl, {
        method: 'POST',
        body: JSON.stringify(payload),
      });
    } catch (err) {
      console.warn('[AssessmentService] Backend persistence fallback to local profile cache:', err);
    }

    return assessment;
  },

  /**
   * Get all assessments recorded for the patient profile.
   */
  async getAssessmentHistory(patientId?: string): Promise<SavedAssessment[]> {
    const localItems = this.getLocalHistory(patientId);

    try {
      const backendUrl = patientId
        ? `${BACKEND_BASE_URL}/risk-assessments?patientId=${patientId}`
        : `${BACKEND_BASE_URL}/risk-assessments`;

      const response = await requestJson<{ content?: any[] } | any[]>(backendUrl);
      const records = Array.isArray(response) ? response : response?.content || [];

      const backendItems: SavedAssessment[] = [];
      for (const rec of records) {
        try {
          if (rec.featureSnapshot) {
            const parsed = typeof rec.featureSnapshot === 'string'
              ? JSON.parse(rec.featureSnapshot)
              : rec.featureSnapshot;

            if (parsed.request && parsed.response) {
              backendItems.push({
                id: rec.id || parsed.id || `assessment-${rec.assessmentTimestamp}`,
                patientId: rec.patientId || patientId,
                patientName: parsed.patientName,
                timestamp: rec.assessmentTimestamp || parsed.timestamp || new Date().toISOString(),
                overallRiskScore: Number(rec.overallRiskScore ?? parsed.response?.prediction?.overall_risk_score ?? 0),
                riskCategory: (rec.riskCategory || parsed.response?.prediction?.risk_category || 'MODERATE') as RiskTier,
                confidenceLevel: rec.confidenceLevel || parsed.response?.prediction?.confidence_level,
                modelVersion: rec.modelVersion || parsed.response?.prediction?.model_version || 'calibrated_v1.0.0',
                request: parsed.request,
                response: parsed.response,
              });
            }
          }
        } catch (e) {
          // Skip malformed record
        }
      }

      // Merge backend items and local items (avoid duplicate IDs)
      const mergedMap = new Map<string, SavedAssessment>();
      for (const item of backendItems) {
        mergedMap.set(item.id, item);
      }
      for (const item of localItems) {
        if (!mergedMap.has(item.id)) {
          mergedMap.set(item.id, item);
        }
      }

      const mergedList = Array.from(mergedMap.values()).sort(
        (a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
      );

      // Refresh local cache with merged list
      try {
        localStorage.setItem(getStorageKey(patientId), JSON.stringify(mergedList));
      } catch {}

      return mergedList;
    } catch (err) {
      // Backend not running or offline; return local history
      return localItems;
    }
  },

  /**
   * Synchronously get locally cached history
   */
  getLocalHistory(patientId?: string): SavedAssessment[] {
    try {
      const raw = localStorage.getItem(getStorageKey(patientId));
      if (!raw) return [];
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) {
        return parsed.sort(
          (a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
        );
      }
      return [];
    } catch {
      return [];
    }
  },

  /**
   * Clear historical assessments for a patient
   */
  clearLocalHistory(patientId?: string): void {
    try {
      localStorage.removeItem(getStorageKey(patientId));
    } catch {}
  },
};
