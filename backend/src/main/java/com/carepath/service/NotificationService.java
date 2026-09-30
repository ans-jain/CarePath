package com.carepath.service;

import com.carepath.api.dto.NotificationResponseDTO;
import com.carepath.api.exception.ResourceNotFoundException;
import com.carepath.domain.enums.NotificationChannel;
import com.carepath.domain.enums.NotificationStatus;
import com.carepath.domain.enums.NotificationType;
import com.carepath.domain.models.Notification;
import com.carepath.domain.models.User;
import com.carepath.domain.repository.NotificationRepository;
import com.carepath.domain.repository.UserRepository;
import com.carepath.service.notification.provider.NotificationDeliveryResult;
import com.carepath.service.notification.provider.NotificationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationPreferenceService preferenceService;
    private final Map<NotificationChannel, NotificationProvider> providerMap;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               NotificationPreferenceService preferenceService,
                               List<NotificationProvider> providers) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.preferenceService = preferenceService;
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(NotificationProvider::getChannel, p -> p));
    }

    @Transactional
    public Optional<Notification> createAndDispatchNotification(UUID userId, NotificationType type,
                                                                 NotificationChannel channel, String title,
                                                                 String message, String eventId, String metadata) {
        // 1. Preference check
        if (!preferenceService.isChannelAndTypeEnabled(userId, channel, type)) {
            log.info("[NOTIFICATION_SKIPPED] User ID: {} has opted out of channel {} or type {}", userId, channel, type);
            return Optional.empty();
        }

        // 2. Idempotency check: prevent duplicate notifications for same event and channel
        if (eventId != null && !eventId.isBlank()) {
            Optional<Notification> existing = notificationRepository.findByUserIdAndEventIdAndChannel(userId, eventId, channel);
            if (existing.isPresent()) {
                log.info("[NOTIFICATION_DUPLICATE_IGNORED] Notification with eventId '{}' and channel '{}' already exists for user {}",
                        eventId, channel, userId);
                return existing;
            }
        }

        // 3. User verification
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // 4. Persistence in PENDING state
        Notification notification = new Notification(user, type, channel, title, message, eventId, metadata);
        notification = notificationRepository.save(notification);
        log.info("[NOTIFICATION_CREATED] Notification ID: {}, User ID: {}, Type: {}, Channel: {}, EventId: {}",
                notification.getId(), userId, type, channel, eventId);

        // 5. Dispatch via Channel Provider
        NotificationProvider provider = providerMap.get(channel);
        if (provider != null) {
            log.info("[NOTIFICATION_QUEUED] Dispatching notification ID: {} via provider for channel: {}",
                    notification.getId(), channel);
            NotificationDeliveryResult result = provider.send(notification);
            if (result.isSuccess()) {
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(Instant.now());
                log.info("[NOTIFICATION_DELIVERED] Successfully delivered notification ID: {}, ProviderMsgId: {}",
                        notification.getId(), result.getProviderMessageId());
            } else {
                notification.setStatus(NotificationStatus.FAILED);
                log.warn("[NOTIFICATION_FAILED] Failed delivering notification ID: {}, Error: {}, Retryable: {}",
                        notification.getId(), result.getErrorMessage(), result.isRetryable());
            }
            notification = notificationRepository.save(notification);
        } else {
            log.warn("[NOTIFICATION_FAILED] No provider registered for channel: {}", channel);
            notification.setStatus(NotificationStatus.FAILED);
            notification = notificationRepository.save(notification);
        }

        return Optional.of(notification);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponseDTO> getUserNotifications(UUID userId, Pageable pageable, boolean unreadOnly) {
        Page<Notification> page = unreadOnly
                ? notificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId, pageable)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return page.map(NotificationResponseDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public NotificationResponseDTO markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));

        // Strict ownership enforcement
        if (!notification.getUser().getId().equals(userId)) {
            log.warn("[NOTIFICATION_ACCESS_DENIED] User ID: {} attempted to access notification ID: {} owned by {}",
                    userId, notificationId, notification.getUser().getId());
            throw new AccessDeniedException("Access denied: You do not own this notification.");
        }

        if (!notification.isRead()) {
            notification.markAsRead();
            notification = notificationRepository.save(notification);
            log.info("[NOTIFICATION_READ] Marked notification ID: {} as read by user ID: {}", notificationId, userId);
        }

        return NotificationResponseDTO.fromEntity(notification);
    }

    @Transactional
    public int markAllAsRead(UUID userId) {
        int updatedCount = notificationRepository.markAllAsReadForUser(userId, Instant.now());
        log.info("[NOTIFICATIONS_READ_ALL] Marked {} notifications as read for user ID: {}", updatedCount, userId);
        return updatedCount;
    }
}
