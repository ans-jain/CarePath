import { requestJson, BACKEND_BASE_URL } from './api';
import {
  NotificationItem,
  NotificationPageResponse,
  NotificationPreferences,
  NotificationPreferenceUpdateRequest,
} from '../types/notification';

// Demo initial notification state for seamless frontend demonstration
const DEMO_STORAGE_KEY = 'carepath_demo_notifications';
const PREFS_STORAGE_KEY = 'carepath_demo_notification_prefs';

function getStoredDemoNotifications(): NotificationItem[] {
  try {
    const raw = localStorage.getItem(DEMO_STORAGE_KEY);
    if (raw) return JSON.parse(raw);
  } catch (e) {
    // fallback
  }

  const initial: NotificationItem[] = [
    {
      id: 'notif-1',
      userId: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
      type: 'RISK_ASSESSMENT_COMPLETED',
      channel: 'IN_APP',
      status: 'SENT',
      title: 'Risk Assessment Completed',
      message: 'Your latest cardiometabolic risk assessment is ready for review.',
      isRead: false,
      createdAt: new Date(Date.now() - 5 * 60 * 1000).toISOString(),
      sentAt: new Date(Date.now() - 5 * 60 * 1000).toISOString(),
    },
    {
      id: 'notif-2',
      userId: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
      type: 'REMINDER',
      channel: 'IN_APP',
      status: 'READ',
      title: 'Daily Vitals Log Reminder',
      message: 'Please remember to record your morning resting blood pressure and vitals.',
      isRead: true,
      createdAt: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
      sentAt: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
      readAt: new Date(Date.now() - 20 * 60 * 60 * 1000).toISOString(),
    },
  ];
  try {
    localStorage.setItem(DEMO_STORAGE_KEY, JSON.stringify(initial));
  } catch (e) {}
  return initial;
}

function getStoredDemoPreferences(): NotificationPreferences {
  try {
    const raw = localStorage.getItem(PREFS_STORAGE_KEY);
    if (raw) return JSON.parse(raw);
  } catch (e) {}

  const initial: NotificationPreferences = {
    userId: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
    inAppEnabled: true,
    emailEnabled: true,
    riskAssessmentCompletedEnabled: true,
    remindersEnabled: true,
    systemUpdatesEnabled: true,
    updatedAt: new Date().toISOString(),
  };
  try {
    localStorage.setItem(PREFS_STORAGE_KEY, JSON.stringify(initial));
  } catch (e) {}
  return initial;
}

export const notificationService = {
  async getNotifications(
    page: number = 0,
    size: number = 20,
    unreadOnly: boolean = false
  ): Promise<NotificationPageResponse> {
    try {
      const url = `${BACKEND_BASE_URL}/notifications?page=${page}&size=${size}&unreadOnly=${unreadOnly}`;
      return await requestJson<NotificationPageResponse>(url, { method: 'GET' });
    } catch (err) {
      // Offline / Demo fallback
      const stored = getStoredDemoNotifications();
      const filtered = unreadOnly ? stored.filter((n) => !n.isRead) : stored;
      const unreadCount = stored.filter((n) => !n.isRead).length;

      return {
        content: filtered,
        page,
        size,
        totalElements: filtered.length,
        totalPages: 1,
        first: true,
        last: true,
        unreadCount,
      };
    }
  },

  async getUnreadCount(): Promise<number> {
    try {
      const url = `${BACKEND_BASE_URL}/notifications/unread-count`;
      const res = await requestJson<{ unreadCount: number }>(url, { method: 'GET' });
      return res.unreadCount;
    } catch (err) {
      const stored = getStoredDemoNotifications();
      return stored.filter((n) => !n.isRead).length;
    }
  },

  async markAsRead(id: string): Promise<NotificationItem> {
    try {
      const url = `${BACKEND_BASE_URL}/notifications/${id}/read`;
      return await requestJson<NotificationItem>(url, { method: 'PATCH' });
    } catch (err) {
      const stored = getStoredDemoNotifications();
      const idx = stored.findIndex((n) => n.id === id);
      if (idx !== -1) {
        stored[idx].isRead = true;
        stored[idx].status = 'READ';
        stored[idx].readAt = new Date().toISOString();
        localStorage.setItem(DEMO_STORAGE_KEY, JSON.stringify(stored));
        return stored[idx];
      }
      throw new Error('Notification not found');
    }
  },

  async markAllAsRead(): Promise<{ updatedCount: number }> {
    try {
      const url = `${BACKEND_BASE_URL}/notifications/read-all`;
      return await requestJson<{ updatedCount: number }>(url, { method: 'PATCH' });
    } catch (err) {
      const stored = getStoredDemoNotifications();
      let count = 0;
      stored.forEach((n) => {
        if (!n.isRead) {
          n.isRead = true;
          n.status = 'READ';
          n.readAt = new Date().toISOString();
          count++;
        }
      });
      localStorage.setItem(DEMO_STORAGE_KEY, JSON.stringify(stored));
      return { updatedCount: count };
    }
  },

  async getPreferences(): Promise<NotificationPreferences> {
    try {
      const url = `${BACKEND_BASE_URL}/notification-preferences`;
      return await requestJson<NotificationPreferences>(url, { method: 'GET' });
    } catch (err) {
      return getStoredDemoPreferences();
    }
  },

  async updatePreferences(
    request: NotificationPreferenceUpdateRequest
  ): Promise<NotificationPreferences> {
    try {
      const url = `${BACKEND_BASE_URL}/notification-preferences`;
      return await requestJson<NotificationPreferences>(url, {
        method: 'PUT',
        body: JSON.stringify(request),
      });
    } catch (err) {
      const current = getStoredDemoPreferences();
      const updated: NotificationPreferences = {
        ...current,
        ...request,
        updatedAt: new Date().toISOString(),
      };
      localStorage.setItem(PREFS_STORAGE_KEY, JSON.stringify(updated));
      return updated;
    }
  },

  async recordAssessmentCompleted(title: string, message: string): Promise<NotificationItem> {
    const stored = getStoredDemoNotifications();
    const newNotif: NotificationItem = {
      id: 'notif-' + Date.now(),
      userId: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
      type: 'RISK_ASSESSMENT_COMPLETED',
      channel: 'IN_APP',
      status: 'SENT',
      title,
      message,
      isRead: false,
      createdAt: new Date().toISOString(),
      sentAt: new Date().toISOString(),
    };
    stored.unshift(newNotif);
    localStorage.setItem(DEMO_STORAGE_KEY, JSON.stringify(stored));
    return newNotif;
  },
};
