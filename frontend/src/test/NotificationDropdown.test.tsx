import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import React from 'react';
import { NotificationDropdown } from '../components/notifications/NotificationDropdown';
import { notificationService } from '../services/notificationService';
import { NotificationPageResponse } from '../types/notification';

vi.mock('../services/notificationService', () => ({
  notificationService: {
    getNotifications: vi.fn(),
    getUnreadCount: vi.fn(),
    markAsRead: vi.fn(),
    markAllAsRead: vi.fn(),
  },
}));

describe('NotificationDropdown', () => {
  const onOpenPreferences = vi.fn();

  const mockResponse: NotificationPageResponse = {
    content: [
      {
        id: 'notif-1',
        userId: 'user-1',
        type: 'RISK_ASSESSMENT_COMPLETED',
        channel: 'IN_APP',
        status: 'SENT',
        title: 'Risk Assessment Completed',
        message: 'Your latest cardiometabolic risk assessment is ready for review.',
        isRead: false,
        createdAt: new Date().toISOString(),
      },
      {
        id: 'notif-2',
        userId: 'user-1',
        type: 'REMINDER',
        channel: 'IN_APP',
        status: 'READ',
        title: 'Daily Vitals Log Reminder',
        message: 'Please remember to record your morning resting blood pressure and vitals.',
        isRead: true,
        createdAt: new Date(Date.now() - 3600000).toISOString(),
      },
    ],
    page: 0,
    size: 20,
    totalElements: 2,
    totalPages: 1,
    first: true,
    last: true,
    unreadCount: 1,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(notificationService.getNotifications).mockResolvedValue(mockResponse);
    vi.mocked(notificationService.getUnreadCount).mockResolvedValue(1);
  });

  it('renders notification bell and displays unread badge count', async () => {
    render(<NotificationDropdown onOpenPreferences={onOpenPreferences} />);

    expect(screen.getByRole('button', { name: /view notifications/i })).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('1')).toBeInTheDocument();
    });
  });

  it('opens dropdown panel on click and displays notification list', async () => {
    render(<NotificationDropdown onOpenPreferences={onOpenPreferences} />);

    const bellBtn = screen.getByRole('button', { name: /view notifications/i });
    fireEvent.click(bellBtn);

    await waitFor(() => {
      expect(screen.getByRole('region', { name: /notifications panel/i })).toBeInTheDocument();
      expect(screen.getByText('Risk Assessment Completed')).toBeInTheDocument();
      expect(screen.getByText('Daily Vitals Log Reminder')).toBeInTheDocument();
    });
  });

  it('filters notifications between All and Unread tabs', async () => {
    render(<NotificationDropdown onOpenPreferences={onOpenPreferences} />);

    fireEvent.click(screen.getByRole('button', { name: /view notifications/i }));

    await waitFor(() => {
      expect(screen.getByText('Risk Assessment Completed')).toBeInTheDocument();
    });

    const unreadTab = screen.getByRole('button', { name: /unread \(1\)/i });
    fireEvent.click(unreadTab);

    await waitFor(() => {
      expect(notificationService.getNotifications).toHaveBeenCalledWith(0, 20, true);
    });
  });

  it('marks an individual notification as read when clicking mark-read button', async () => {
    vi.mocked(notificationService.markAsRead).mockResolvedValue({
      ...mockResponse.content[0],
      isRead: true,
      status: 'READ',
    });

    render(<NotificationDropdown onOpenPreferences={onOpenPreferences} />);
    fireEvent.click(screen.getByRole('button', { name: /view notifications/i }));

    await waitFor(() => {
      expect(screen.getByText('Risk Assessment Completed')).toBeInTheDocument();
    });

    const markReadBtn = screen.getByTitle('Mark as read');
    fireEvent.click(markReadBtn);

    await waitFor(() => {
      expect(notificationService.markAsRead).toHaveBeenCalledWith('notif-1');
    });
  });

  it('marks all notifications as read when clicking Mark all read button', async () => {
    vi.mocked(notificationService.markAllAsRead).mockResolvedValue({ updatedCount: 1 });

    render(<NotificationDropdown onOpenPreferences={onOpenPreferences} />);
    fireEvent.click(screen.getByRole('button', { name: /view notifications/i }));

    await waitFor(() => {
      expect(screen.getByTitle('Mark all as read')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTitle('Mark all as read'));

    await waitFor(() => {
      expect(notificationService.markAllAsRead).toHaveBeenCalled();
    });
  });

  it('triggers onOpenPreferences callback and closes dropdown when settings clicked', async () => {
    render(<NotificationDropdown onOpenPreferences={onOpenPreferences} />);
    fireEvent.click(screen.getByRole('button', { name: /view notifications/i }));

    await waitFor(() => {
      expect(screen.getByLabelText('Notification Settings')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByLabelText('Notification Settings'));

    expect(onOpenPreferences).toHaveBeenCalled();
    expect(screen.queryByRole('region', { name: /notifications panel/i })).not.toBeInTheDocument();
  });

  it('displays empty state when there are no notifications', async () => {
    vi.mocked(notificationService.getNotifications).mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
      unreadCount: 0,
    });

    render(<NotificationDropdown onOpenPreferences={onOpenPreferences} />);
    fireEvent.click(screen.getByRole('button', { name: /view notifications/i }));

    await waitFor(() => {
      expect(screen.getByText('No notifications')).toBeInTheDocument();
      expect(screen.getByText('No activity records found.')).toBeInTheDocument();
    });
  });
});
