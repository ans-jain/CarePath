import { describe, it, expect, vi, beforeEach } from 'vitest';
import { notificationService } from '../services/notificationService';
import * as apiModule from '../services/api';

describe('notificationService', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('fetches notifications from backend API when available', async () => {
    const mockApiResponse = {
      content: [
        {
          id: 'test-1',
          userId: 'user-1',
          type: 'RISK_ASSESSMENT_COMPLETED' as const,
          channel: 'IN_APP' as const,
          status: 'SENT' as const,
          title: 'Assessment Complete',
          message: 'Your assessment is ready',
          isRead: false,
          createdAt: new Date().toISOString(),
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
      unreadCount: 1,
    };

    vi.spyOn(apiModule, 'requestJson').mockResolvedValue(mockApiResponse);

    const result = await notificationService.getNotifications(0, 20, false);
    expect(result.content).toHaveLength(1);
    expect(result.content[0].title).toBe('Assessment Complete');
    expect(result.unreadCount).toBe(1);
  });

  it('falls back to demo notifications if backend request fails', async () => {
    vi.spyOn(apiModule, 'requestJson').mockRejectedValue(new Error('Network error'));

    const result = await notificationService.getNotifications(0, 20, false);
    expect(result.content.length).toBeGreaterThan(0);
    expect(result.totalElements).toBeGreaterThan(0);
    expect(result.first).toBe(true);
  });

  it('fetches unread count with backend API or demo fallback', async () => {
    vi.spyOn(apiModule, 'requestJson').mockResolvedValue({ unreadCount: 3 });

    const count = await notificationService.getUnreadCount();
    expect(count).toBe(3);

    // Test fallback when rejected
    vi.spyOn(apiModule, 'requestJson').mockRejectedValue(new Error('Network error'));
    const fallbackCount = await notificationService.getUnreadCount();
    expect(typeof fallbackCount).toBe('number');
  });

  it('marks an individual notification as read in fallback storage', async () => {
    vi.spyOn(apiModule, 'requestJson').mockRejectedValue(new Error('Offline'));

    // Populate demo data first
    const list = await notificationService.getNotifications();
    const targetId = list.content[0].id;

    const updated = await notificationService.markAsRead(targetId);
    expect(updated.id).toBe(targetId);
    expect(updated.isRead).toBe(true);
    expect(updated.status).toBe('READ');
  });

  it('marks all notifications as read in fallback storage', async () => {
    vi.spyOn(apiModule, 'requestJson').mockRejectedValue(new Error('Offline'));

    // Initialize list
    await notificationService.getNotifications();

    const res = await notificationService.markAllAsRead();
    expect(res.updatedCount).toBeGreaterThanOrEqual(0);

    const after = await notificationService.getNotifications();
    const remainingUnread = after.content.filter((n) => !n.isRead);
    expect(remainingUnread.length).toBe(0);
  });

  it('retrieves and updates user preferences in fallback mode', async () => {
    vi.spyOn(apiModule, 'requestJson').mockRejectedValue(new Error('Offline'));

    const prefs = await notificationService.getPreferences();
    expect(prefs.inAppEnabled).toBe(true);

    const updated = await notificationService.updatePreferences({
      inAppEnabled: false,
      emailEnabled: true,
      riskAssessmentCompletedEnabled: true,
      remindersEnabled: false,
      systemUpdatesEnabled: true,
    });

    expect(updated.inAppEnabled).toBe(false);
    expect(updated.emailEnabled).toBe(true);
    expect(updated.remindersEnabled).toBe(false);
  });

  it('records newly completed assessment notification to demo storage', async () => {
    const notif = await notificationService.recordAssessmentCompleted(
      'New Assessment Completed',
      'Evaluation finalized'
    );
    expect(notif.title).toBe('New Assessment Completed');
    expect(notif.isRead).toBe(false);

    vi.spyOn(apiModule, 'requestJson').mockRejectedValue(new Error('Offline'));
    const list = await notificationService.getNotifications();
    expect(list.content.some((n) => n.id === notif.id)).toBe(true);
  });
});
