import React, { useState, useEffect, useRef } from 'react';
import { Bell, CheckCheck, Settings, X, AlertCircle, Clock, Check } from 'lucide-react';
import { NotificationItem } from '../../types/notification';
import { notificationService } from '../../services/notificationService';

interface NotificationDropdownProps {
  onOpenPreferences: () => void;
}

export const NotificationDropdown: React.FC<NotificationDropdownProps> = ({ onOpenPreferences }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'all' | 'unread'>('all');

  const dropdownRef = useRef<HTMLDivElement>(null);

  const fetchNotifications = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await notificationService.getNotifications(0, 20, activeTab === 'unread');
      setNotifications(data.content);
      setUnreadCount(data.unreadCount);
    } catch (err: any) {
      setError(err.message || 'Failed to load notifications');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchNotifications();
    // Poll unread count every 30s
    const interval = setInterval(async () => {
      try {
        const count = await notificationService.getUnreadCount();
        setUnreadCount(count);
      } catch (e) {}
    }, 30000);
    return () => clearInterval(interval);
  }, [activeTab]);

  // Click outside listener to close dropdown
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isOpen]);

  const handleMarkAsRead = async (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      await notificationService.markAsRead(id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, isRead: true, status: 'READ' } : n))
      );
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch (err: any) {
      console.error('Failed to mark notification as read:', err);
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await notificationService.markAllAsRead();
      setNotifications((prev) =>
        prev.map((n) => ({ ...n, isRead: true, status: 'READ' }))
      );
      setUnreadCount(0);
    } catch (err: any) {
      console.error('Failed to mark all notifications as read:', err);
    }
  };

  const formatRelativeTime = (timestamp: string) => {
    try {
      const diffMs = Date.now() - new Date(timestamp).getTime();
      const diffMins = Math.floor(diffMs / (1000 * 60));
      if (diffMins < 1) return 'Just now';
      if (diffMins < 60) return `${diffMins}m ago`;
      const diffHours = Math.floor(diffMins / 60);
      if (diffHours < 24) return `${diffHours}h ago`;
      const diffDays = Math.floor(diffHours / 24);
      if (diffDays === 1) return 'Yesterday';
      return `${diffDays}d ago`;
    } catch (e) {
      return 'Recently';
    }
  };

  return (
    <div className="relative" ref={dropdownRef}>
      {/* Bell Trigger Button */}
      <button
        type="button"
        onClick={() => {
          setIsOpen(!isOpen);
          if (!isOpen) fetchNotifications();
        }}
        className="relative p-2 text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-lg transition-colors"
        title="Notifications"
        aria-label="View notifications"
        aria-expanded={isOpen}
      >
        <Bell className="w-5 h-5" />
        {unreadCount > 0 && (
          <span
            className="absolute top-1 right-1 flex items-center justify-center min-w-[18px] h-[18px] px-1 text-[10px] font-bold text-white bg-rose-500 rounded-full ring-2 ring-white animate-pulse"
            aria-label={`${unreadCount} unread notifications`}
          >
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {/* Dropdown Menu Panel */}
      {isOpen && (
        <div
          role="region"
          aria-label="Notifications panel"
          className="absolute right-0 mt-2 w-80 sm:w-96 bg-white rounded-xl shadow-xl border border-slate-200 z-50 overflow-hidden animate-fadeIn"
        >
          {/* Header */}
          <div className="p-4 border-b border-slate-100 bg-slate-50/80 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <h3 className="text-sm font-bold text-slate-900">Notifications</h3>
              {unreadCount > 0 && (
                <span className="px-1.5 py-0.5 text-[11px] font-semibold bg-rose-100 text-rose-700 rounded-full">
                  {unreadCount} new
                </span>
              )}
            </div>

            <div className="flex items-center gap-1.5">
              {unreadCount > 0 && (
                <button
                  type="button"
                  onClick={handleMarkAllAsRead}
                  className="inline-flex items-center gap-1 px-2 py-1 text-xs font-semibold text-clinical-600 hover:text-clinical-800 hover:bg-slate-100 rounded transition-colors"
                  title="Mark all as read"
                >
                  <CheckCheck className="w-3.5 h-3.5" />
                  <span className="hidden sm:inline">Mark all read</span>
                </button>
              )}
              <button
                type="button"
                onClick={() => {
                  setIsOpen(false);
                  onOpenPreferences();
                }}
                className="p-1 text-slate-400 hover:text-slate-600 rounded hover:bg-slate-100 transition-colors"
                title="Notification Settings"
                aria-label="Notification Settings"
              >
                <Settings className="w-4 h-4" />
              </button>
              <button
                type="button"
                onClick={() => setIsOpen(false)}
                className="p-1 text-slate-400 hover:text-slate-600 rounded hover:bg-slate-100 transition-colors"
                aria-label="Close"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          </div>

          {/* Filter Tabs */}
          <div className="flex border-b border-slate-100 bg-white px-3 pt-2 text-xs font-medium">
            <button
              type="button"
              onClick={() => setActiveTab('all')}
              className={`pb-2 px-3 border-b-2 font-semibold transition-colors ${
                activeTab === 'all'
                  ? 'border-clinical-600 text-clinical-700'
                  : 'border-transparent text-slate-500 hover:text-slate-700'
              }`}
            >
              All
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('unread')}
              className={`pb-2 px-3 border-b-2 font-semibold transition-colors ${
                activeTab === 'unread'
                  ? 'border-clinical-600 text-clinical-700'
                  : 'border-transparent text-slate-500 hover:text-slate-700'
              }`}
            >
              Unread ({unreadCount})
            </button>
          </div>

          {/* Body / List */}
          <div className="max-h-80 overflow-y-auto divide-y divide-slate-100">
            {isLoading && (
              <div className="p-6 text-center text-xs text-slate-400 space-y-2">
                <div className="w-5 h-5 border-2 border-clinical-600 border-t-transparent rounded-full animate-spin mx-auto" />
                <span>Loading notifications...</span>
              </div>
            )}

            {!isLoading && error && (
              <div className="p-4 text-center space-y-2">
                <p className="text-xs text-rose-600">{error}</p>
                <button
                  type="button"
                  onClick={fetchNotifications}
                  className="px-2.5 py-1 text-xs text-slate-700 bg-slate-100 hover:bg-slate-200 rounded font-medium"
                >
                  Retry
                </button>
              </div>
            )}

            {!isLoading && !error && notifications.length === 0 && (
              <div className="p-8 text-center text-slate-400 space-y-1">
                <Bell className="w-8 h-8 text-slate-300 mx-auto mb-1 stroke-1" />
                <p className="text-xs font-semibold text-slate-600">No notifications</p>
                <p className="text-[11px] text-slate-400">
                  {activeTab === 'unread' ? 'You have read all your alerts.' : 'No activity records found.'}
                </p>
              </div>
            )}

            {!isLoading &&
              !error &&
              notifications.map((item) => (
                <div
                  key={item.id}
                  className={`p-3.5 flex items-start gap-3 transition-colors ${
                    !item.isRead ? 'bg-blue-50/40 hover:bg-blue-50/70' : 'hover:bg-slate-50'
                  }`}
                >
                  {/* Status Indicator */}
                  <div className="mt-1 shrink-0">
                    {!item.isRead ? (
                      <span className="block w-2 h-2 rounded-full bg-clinical-600" title="Unread" />
                    ) : (
                      <span className="block w-2 h-2 rounded-full bg-slate-300" title="Read" />
                    )}
                  </div>

                  {/* Content */}
                  <div className="flex-1 min-w-0 space-y-0.5">
                    <div className="flex items-start justify-between gap-2">
                      <p
                        className={`text-xs font-semibold truncate ${
                          !item.isRead ? 'text-slate-900' : 'text-slate-700'
                        }`}
                      >
                        {item.title}
                      </p>
                      <span className="text-[10px] text-slate-400 whitespace-nowrap flex items-center gap-0.5">
                        <Clock className="w-3 h-3 text-slate-300" />
                        {formatRelativeTime(item.createdAt)}
                      </span>
                    </div>
                    <p className="text-xs text-slate-600 leading-relaxed line-clamp-2">{item.message}</p>
                  </div>

                  {/* Individual Mark Read Button */}
                  {!item.isRead && (
                    <button
                      type="button"
                      onClick={(e) => handleMarkAsRead(item.id, e)}
                      className="p-1 text-slate-400 hover:text-clinical-600 rounded transition-colors shrink-0"
                      title="Mark as read"
                      aria-label="Mark notification as read"
                    >
                      <Check className="w-3.5 h-3.5" />
                    </button>
                  )}
                </div>
              ))}
          </div>

          {/* Footer */}
          <div className="p-2.5 bg-slate-50 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500 px-4">
            <span className="text-[11px] text-slate-400">CarePath Non-diagnostic alerts</span>
            <button
              type="button"
              onClick={() => {
                setIsOpen(false);
                onOpenPreferences();
              }}
              className="text-xs font-semibold text-clinical-600 hover:text-clinical-800"
            >
              Preferences &rarr;
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
