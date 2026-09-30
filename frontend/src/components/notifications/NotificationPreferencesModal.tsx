import React, { useState, useEffect } from 'react';
import { X, Bell, Mail, Activity, Clock, ShieldCheck, Check } from 'lucide-react';
import { NotificationPreferences } from '../../types/notification';
import { notificationService } from '../../services/notificationService';

interface NotificationPreferencesModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const NotificationPreferencesModal: React.FC<NotificationPreferencesModalProps> = ({
  isOpen,
  onClose,
}) => {
  const [preferences, setPreferences] = useState<NotificationPreferences | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      loadPreferences();
    }
  }, [isOpen]);

  const loadPreferences = async () => {
    setIsLoading(true);
    setError(null);
    setSaveSuccess(false);
    try {
      const prefs = await notificationService.getPreferences();
      setPreferences(prefs);
    } catch (err: any) {
      setError(err.message || 'Failed to load preferences');
    } finally {
      setIsLoading(false);
    }
  };

  const handleToggle = (key: keyof NotificationPreferences) => {
    if (!preferences) return;
    setPreferences({
      ...preferences,
      [key]: !preferences[key],
    });
    setSaveSuccess(false);
  };

  const handleSave = async () => {
    if (!preferences) return;
    setIsSaving(true);
    setError(null);
    try {
      const updated = await notificationService.updatePreferences({
        inAppEnabled: preferences.inAppEnabled,
        emailEnabled: preferences.emailEnabled,
        riskAssessmentCompletedEnabled: preferences.riskAssessmentCompletedEnabled,
        remindersEnabled: preferences.remindersEnabled,
        systemUpdatesEnabled: preferences.systemUpdatesEnabled,
      });
      setPreferences(updated);
      setSaveSuccess(true);
      setTimeout(() => setSaveSuccess(false), 3000);
    } catch (err: any) {
      setError(err.message || 'Failed to save preferences');
    } finally {
      setIsSaving(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="pref-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs"
    >
      <div className="relative w-full max-w-lg bg-white rounded-xl shadow-xl border border-slate-200 overflow-hidden flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 bg-slate-50/60">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-clinical-100 text-clinical-700 flex items-center justify-center">
              <Bell className="w-4 h-4" />
            </div>
            <div>
              <h2 id="pref-modal-title" className="text-base font-bold text-slate-900">
                Notification Preferences
              </h2>
              <p className="text-xs text-slate-500">Configure delivery channels and alert categories</p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-100 transition-colors"
            aria-label="Close"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 space-y-6 overflow-y-auto max-h-[75vh]">
          {isLoading ? (
            <div className="p-8 text-center text-xs text-slate-400 space-y-2">
              <div className="w-6 h-6 border-2 border-clinical-600 border-t-transparent rounded-full animate-spin mx-auto" />
              <span>Loading notification preferences...</span>
            </div>
          ) : error ? (
            <div className="p-4 bg-rose-50 border border-rose-200 rounded-lg text-xs text-rose-700 text-center">
              {error}
            </div>
          ) : preferences ? (
            <div className="space-y-6">
              {/* Delivery Channels */}
              <div className="space-y-3">
                <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400">
                  Delivery Channels
                </h3>

                {/* In-App Toggle */}
                <div className="flex items-center justify-between p-3.5 bg-slate-50 rounded-lg border border-slate-200/80">
                  <div className="flex items-start gap-3">
                    <Bell className="w-5 h-5 text-clinical-600 mt-0.5 shrink-0" />
                    <div>
                      <h4 className="text-xs font-semibold text-slate-900">In-App Notifications</h4>
                      <p className="text-[11px] text-slate-500 leading-relaxed">
                        Display persistent alerts and badge counters within the portal shell.
                      </p>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => handleToggle('inAppEnabled')}
                    className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-hidden ${
                      preferences.inAppEnabled ? 'bg-clinical-600' : 'bg-slate-300'
                    }`}
                    role="switch"
                    aria-checked={preferences.inAppEnabled}
                  >
                    <span
                      className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow-sm ring-0 transition duration-200 ease-in-out ${
                        preferences.inAppEnabled ? 'translate-x-5' : 'translate-x-0'
                      }`}
                    />
                  </button>
                </div>

                {/* Email Toggle */}
                <div className="flex items-center justify-between p-3.5 bg-slate-50 rounded-lg border border-slate-200/80">
                  <div className="flex items-start gap-3">
                    <Mail className="w-5 h-5 text-purple-600 mt-0.5 shrink-0" />
                    <div>
                      <h4 className="text-xs font-semibold text-slate-900">Email Notifications</h4>
                      <p className="text-[11px] text-slate-500 leading-relaxed">
                        Receive non-sensitive summary emails when assessments or reminders are triggered.
                      </p>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => handleToggle('emailEnabled')}
                    className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-hidden ${
                      preferences.emailEnabled ? 'bg-clinical-600' : 'bg-slate-300'
                    }`}
                    role="switch"
                    aria-checked={preferences.emailEnabled}
                  >
                    <span
                      className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow-sm ring-0 transition duration-200 ease-in-out ${
                        preferences.emailEnabled ? 'translate-x-5' : 'translate-x-0'
                      }`}
                    />
                  </button>
                </div>
              </div>

              {/* Alert Categories */}
              <div className="space-y-3 pt-2 border-t border-slate-100">
                <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400">
                  Notification Triggers
                </h3>

                {/* Assessment Completion Toggle */}
                <div className="flex items-center justify-between p-3.5 bg-slate-50 rounded-lg border border-slate-200/80">
                  <div className="flex items-start gap-3">
                    <Activity className="w-5 h-5 text-emerald-600 mt-0.5 shrink-0" />
                    <div>
                      <h4 className="text-xs font-semibold text-slate-900">Risk Assessment Completed</h4>
                      <p className="text-[11px] text-slate-500 leading-relaxed">
                        Notify me when a new ML cardiometabolic assessment evaluation finishes.
                      </p>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => handleToggle('riskAssessmentCompletedEnabled')}
                    className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-hidden ${
                      preferences.riskAssessmentCompletedEnabled ? 'bg-clinical-600' : 'bg-slate-300'
                    }`}
                    role="switch"
                    aria-checked={preferences.riskAssessmentCompletedEnabled}
                  >
                    <span
                      className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow-sm ring-0 transition duration-200 ease-in-out ${
                        preferences.riskAssessmentCompletedEnabled ? 'translate-x-5' : 'translate-x-0'
                      }`}
                    />
                  </button>
                </div>

                {/* Reminders Toggle */}
                <div className="flex items-center justify-between p-3.5 bg-slate-50 rounded-lg border border-slate-200/80">
                  <div className="flex items-start gap-3">
                    <Clock className="w-5 h-5 text-amber-600 mt-0.5 shrink-0" />
                    <div>
                      <h4 className="text-xs font-semibold text-slate-900">Scheduled Vitals Reminders</h4>
                      <p className="text-[11px] text-slate-500 leading-relaxed">
                        Remind me to record resting blood pressure, fasting glucose, and vitals.
                      </p>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => handleToggle('remindersEnabled')}
                    className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-hidden ${
                      preferences.remindersEnabled ? 'bg-clinical-600' : 'bg-slate-300'
                    }`}
                    role="switch"
                    aria-checked={preferences.remindersEnabled}
                  >
                    <span
                      className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow-sm ring-0 transition duration-200 ease-in-out ${
                        preferences.remindersEnabled ? 'translate-x-5' : 'translate-x-0'
                      }`}
                    />
                  </button>
                </div>

                {/* System Updates */}
                <div className="flex items-center justify-between p-3.5 bg-slate-50 rounded-lg border border-slate-200/80">
                  <div className="flex items-start gap-3">
                    <ShieldCheck className="w-5 h-5 text-blue-600 mt-0.5 shrink-0" />
                    <div>
                      <h4 className="text-xs font-semibold text-slate-900">System & Security Updates</h4>
                      <p className="text-[11px] text-slate-500 leading-relaxed">
                        Security notices, session notices, and model version updates.
                      </p>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => handleToggle('systemUpdatesEnabled')}
                    className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-hidden ${
                      preferences.systemUpdatesEnabled ? 'bg-clinical-600' : 'bg-slate-300'
                    }`}
                    role="switch"
                    aria-checked={preferences.systemUpdatesEnabled}
                  >
                    <span
                      className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow-sm ring-0 transition duration-200 ease-in-out ${
                        preferences.systemUpdatesEnabled ? 'translate-x-5' : 'translate-x-0'
                      }`}
                    />
                  </button>
                </div>
              </div>
            </div>
          ) : null}
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-slate-100 bg-slate-50/60 flex items-center justify-between">
          <div className="text-xs text-slate-500">
            {saveSuccess && (
              <span className="text-emerald-700 font-semibold flex items-center gap-1">
                <Check className="w-3.5 h-3.5" /> Preferences saved
              </span>
            )}
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-xs font-semibold text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg transition-colors"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={handleSave}
              disabled={isSaving || isLoading}
              className="px-4 py-2 text-xs font-semibold text-white bg-clinical-600 hover:bg-clinical-700 disabled:bg-slate-300 rounded-lg shadow-xs transition-colors"
            >
              {isSaving ? 'Saving...' : 'Save Preferences'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
