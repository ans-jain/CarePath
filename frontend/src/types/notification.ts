export type NotificationType =
  | 'RISK_ASSESSMENT_COMPLETED'
  | 'RISK_ASSESSMENT_FAILED'
  | 'REMINDER'
  | 'SYSTEM';

export type NotificationChannel = 'IN_APP' | 'EMAIL' | 'SMS' | 'PUSH';

export type NotificationStatus = 'PENDING' | 'SENT' | 'FAILED' | 'READ';

export interface NotificationItem {
  id: string;
  userId: string;
  type: NotificationType;
  channel: NotificationChannel;
  status: NotificationStatus;
  title: string;
  message: string;
  eventId?: string | null;
  metadata?: string | null;
  isRead: boolean;
  createdAt: string;
  sentAt?: string | null;
  readAt?: string | null;
}

export interface NotificationPageResponse {
  content: NotificationItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  unreadCount: number;
}

export interface NotificationPreferences {
  userId: string;
  inAppEnabled: boolean;
  emailEnabled: boolean;
  riskAssessmentCompletedEnabled: boolean;
  remindersEnabled: boolean;
  systemUpdatesEnabled: boolean;
  updatedAt?: string;
}

export interface NotificationPreferenceUpdateRequest {
  inAppEnabled?: boolean;
  emailEnabled?: boolean;
  riskAssessmentCompletedEnabled?: boolean;
  remindersEnabled?: boolean;
  systemUpdatesEnabled?: boolean;
}
