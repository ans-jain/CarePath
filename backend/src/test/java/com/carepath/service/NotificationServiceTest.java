package com.carepath.service;

import com.carepath.api.dto.NotificationResponseDTO;
import com.carepath.api.exception.ResourceNotFoundException;
import com.carepath.domain.enums.NotificationChannel;
import com.carepath.domain.enums.NotificationStatus;
import com.carepath.domain.enums.NotificationType;
import com.carepath.domain.enums.Role;
import com.carepath.domain.models.Notification;
import com.carepath.domain.models.User;
import com.carepath.domain.repository.NotificationRepository;
import com.carepath.domain.repository.UserRepository;
import com.carepath.service.notification.provider.InAppNotificationProvider;
import com.carepath.service.notification.provider.NotificationDeliveryResult;
import com.carepath.service.notification.provider.NotificationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationPreferenceService preferenceService;

    @Mock
    private NotificationProvider emailProvider;

    private InAppNotificationProvider inAppProvider;
    private NotificationService notificationService;

    private User sampleUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sampleUser = new User("sarah@carepath.io", "hashedPass", Role.ROLE_PATIENT, "Sarah", "Jenkins");
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(sampleUser, userId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        inAppProvider = new InAppNotificationProvider();

        lenient().when(emailProvider.getChannel()).thenReturn(NotificationChannel.EMAIL);

        notificationService = new NotificationService(
                notificationRepository,
                userRepository,
                preferenceService,
                List.of(inAppProvider, emailProvider)
        );
    }

    @Test
    @DisplayName("Successfully creates and delivers in-app notification when preferences allow")
    void createAndDispatchNotification_InApp_Success() {
        when(preferenceService.isChannelAndTypeEnabled(userId, NotificationChannel.IN_APP, NotificationType.RISK_ASSESSMENT_COMPLETED))
                .thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Notification> result = notificationService.createAndDispatchNotification(
                userId,
                NotificationType.RISK_ASSESSMENT_COMPLETED,
                NotificationChannel.IN_APP,
                "Risk Assessment Completed",
                "Your assessment is ready.",
                "event-123",
                "{}"
        );

        assertThat(result).isPresent();
        Notification notification = result.get();
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getSentAt()).isNotNull();
        assertThat(notification.getTitle()).isEqualTo("Risk Assessment Completed");
        verify(notificationRepository, atLeastOnce()).save(any(Notification.class));
    }

    @Test
    @DisplayName("Skips notification creation when user preferences disallow channel or type")
    void createAndDispatchNotification_SkippedByPreference() {
        when(preferenceService.isChannelAndTypeEnabled(userId, NotificationChannel.EMAIL, NotificationType.RISK_ASSESSMENT_COMPLETED))
                .thenReturn(false);

        Optional<Notification> result = notificationService.createAndDispatchNotification(
                userId,
                NotificationType.RISK_ASSESSMENT_COMPLETED,
                NotificationChannel.EMAIL,
                "Email Alert",
                "Message",
                "event-456",
                "{}"
        );

        assertThat(result).isEmpty();
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("Prevents duplicate dispatch when notification with same eventId and channel already exists")
    void createAndDispatchNotification_IdempotencyGuard() {
        when(preferenceService.isChannelAndTypeEnabled(userId, NotificationChannel.IN_APP, NotificationType.RISK_ASSESSMENT_COMPLETED))
                .thenReturn(true);

        Notification existingNotification = new Notification(
                sampleUser,
                NotificationType.RISK_ASSESSMENT_COMPLETED,
                NotificationChannel.IN_APP,
                "Existing",
                "Already sent",
                "dup-event-789",
                "{}"
        );
        existingNotification.setStatus(NotificationStatus.SENT);

        when(notificationRepository.findByUserIdAndEventIdAndChannel(userId, "dup-event-789", NotificationChannel.IN_APP))
                .thenReturn(Optional.of(existingNotification));

        Optional<Notification> result = notificationService.createAndDispatchNotification(
                userId,
                NotificationType.RISK_ASSESSMENT_COMPLETED,
                NotificationChannel.IN_APP,
                "New Attempt",
                "New Message",
                "dup-event-789",
                "{}"
        );

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Existing");
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException if user does not exist")
    void createAndDispatchNotification_UserNotFound() {
        when(preferenceService.isChannelAndTypeEnabled(userId, NotificationChannel.IN_APP, NotificationType.SYSTEM))
                .thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.createAndDispatchNotification(
                userId,
                NotificationType.SYSTEM,
                NotificationChannel.IN_APP,
                "System Notice",
                "Message",
                null,
                "{}"
        )).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Marks notification as read when owned by authenticated user")
    void markAsRead_Success() {
        UUID notifId = UUID.randomUUID();
        Notification notification = new Notification(
                sampleUser,
                NotificationType.SYSTEM,
                NotificationChannel.IN_APP,
                "Title",
                "Message",
                null,
                "{}"
        );

        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponseDTO response = notificationService.markAsRead(notifId, userId);

        assertThat(response.isRead()).isTrue();
        assertThat(response.getReadAt()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(NotificationStatus.READ);
    }

    @Test
    @DisplayName("Throws AccessDeniedException when user attempts to mark another user's notification as read")
    void markAsRead_AccessDenied() {
        UUID notifId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        User otherUser = new User("other@carepath.io", "hash", Role.ROLE_PATIENT, "Other", "User");
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(otherUser, otherUserId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Notification notification = new Notification(
                otherUser,
                NotificationType.SYSTEM,
                NotificationChannel.IN_APP,
                "Title",
                "Message",
                null,
                "{}"
        );

        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(notifId, userId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You do not own this notification");
    }

    @Test
    @DisplayName("Returns unread count accurately")
    void getUnreadCount_ReturnsCount() {
        when(notificationRepository.countByUserIdAndReadAtIsNull(userId)).thenReturn(3L);

        long count = notificationService.getUnreadCount(userId);
        assertThat(count).isEqualTo(3L);
    }

    @Test
    @DisplayName("Bulk marks all notifications as read")
    void markAllAsRead_UpdatesCount() {
        when(notificationRepository.markAllAsReadForUser(eq(userId), any())).thenReturn(5);

        int updated = notificationService.markAllAsRead(userId);
        assertThat(updated).isEqualTo(5);
    }

    @Test
    @DisplayName("Retrieves paginated user notifications")
    void getUserNotifications_ReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Notification notification = new Notification(
                sampleUser,
                NotificationType.REMINDER,
                NotificationChannel.IN_APP,
                "Reminder",
                "Log vitals",
                null,
                "{}"
        );
        Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1);

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)).thenReturn(page);

        Page<NotificationResponseDTO> result = notificationService.getUserNotifications(userId, pageable, false);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Reminder");
    }
}
