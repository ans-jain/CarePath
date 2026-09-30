import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import React from 'react';
import { NotificationPreferencesModal } from '../components/notifications/NotificationPreferencesModal';
import { notificationService } from '../services/notificationService';
import { NotificationPreferences } from '../types/notification';

vi.mock('../services/notificationService', () => ({
  notificationService: {
    getPreferences: vi.fn(),
    updatePreferences: vi.fn(),
  },
}));

describe('NotificationPreferencesModal', () => {
  const onClose = vi.fn();

  const mockPreferences: NotificationPreferences = {
    userId: 'user-1',
    inAppEnabled: true,
    emailEnabled: false,
    riskAssessmentCompletedEnabled: true,
    remindersEnabled: true,
    systemUpdatesEnabled: false,
    updatedAt: new Date().toISOString(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(notificationService.getPreferences).mockResolvedValue(mockPreferences);
    vi.mocked(notificationService.updatePreferences).mockResolvedValue({
      ...mockPreferences,
      emailEnabled: true,
    });
  });

  it('does not render when isOpen is false', () => {
    const { container } = render(
      <NotificationPreferencesModal isOpen={false} onClose={onClose} />
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders modal dialog with channels and categories when isOpen is true', async () => {
    render(<NotificationPreferencesModal isOpen={true} onClose={onClose} />);

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText('Notification Preferences')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('In-App Notifications')).toBeInTheDocument();
      expect(screen.getByText('Email Notifications')).toBeInTheDocument();
      expect(screen.getByText('Risk Assessment Completed')).toBeInTheDocument();
      expect(screen.getByText('Scheduled Vitals Reminders')).toBeInTheDocument();
      expect(screen.getByText('System & Security Updates')).toBeInTheDocument();
    });
  });

  it('loads and reflects initial toggle states', async () => {
    render(<NotificationPreferencesModal isOpen={true} onClose={onClose} />);

    await waitFor(() => {
      const switches = screen.getAllByRole('switch');
      expect(switches).toHaveLength(5);
      // InApp: true
      expect(switches[0]).toHaveAttribute('aria-checked', 'true');
      // Email: false
      expect(switches[1]).toHaveAttribute('aria-checked', 'false');
      // Risk Assessment: true
      expect(switches[2]).toHaveAttribute('aria-checked', 'true');
      // Reminders: true
      expect(switches[3]).toHaveAttribute('aria-checked', 'true');
      // System updates: false
      expect(switches[4]).toHaveAttribute('aria-checked', 'false');
    });
  });

  it('toggles switch state on click', async () => {
    render(<NotificationPreferencesModal isOpen={true} onClose={onClose} />);

    await waitFor(() => {
      expect(screen.getAllByRole('switch')).toHaveLength(5);
    });

    const emailSwitch = screen.getAllByRole('switch')[1];
    expect(emailSwitch).toHaveAttribute('aria-checked', 'false');

    fireEvent.click(emailSwitch);
    expect(emailSwitch).toHaveAttribute('aria-checked', 'true');
  });

  it('saves preferences and displays confirmation message', async () => {
    render(<NotificationPreferencesModal isOpen={true} onClose={onClose} />);

    await waitFor(() => {
      expect(screen.getAllByRole('switch')).toHaveLength(5);
    });

    const emailSwitch = screen.getAllByRole('switch')[1];
    fireEvent.click(emailSwitch); // turn on email

    const saveButton = screen.getByRole('button', { name: /save preferences/i });
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(notificationService.updatePreferences).toHaveBeenCalledWith({
        inAppEnabled: true,
        emailEnabled: true,
        riskAssessmentCompletedEnabled: true,
        remindersEnabled: true,
        systemUpdatesEnabled: false,
      });
      expect(screen.getByText(/preferences saved/i)).toBeInTheDocument();
    });
  });

  it('calls onClose when cancel or close button is clicked', async () => {
    render(<NotificationPreferencesModal isOpen={true} onClose={onClose} />);

    await waitFor(() => {
      expect(screen.getByText('In-App Notifications')).toBeInTheDocument();
    });

    const closeBtn = screen.getByLabelText('Close');
    fireEvent.click(closeBtn);
    expect(onClose).toHaveBeenCalledTimes(1);

    const cancelBtn = screen.getByRole('button', { name: /cancel/i });
    fireEvent.click(cancelBtn);
    expect(onClose).toHaveBeenCalledTimes(2);
  });
});
