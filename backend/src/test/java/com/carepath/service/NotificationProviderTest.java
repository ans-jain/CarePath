package com.carepath.service;

import com.carepath.domain.enums.NotificationChannel;
import com.carepath.domain.enums.NotificationType;
import com.carepath.domain.enums.Role;
import com.carepath.domain.models.Notification;
import com.carepath.domain.models.User;
import com.carepath.service.notification.provider.EmailNotificationProvider;
import com.carepath.service.notification.provider.InAppNotificationProvider;
import com.carepath.service.notification.provider.NotificationDeliveryResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationProviderTest {

    @Test
    @DisplayName("InAppNotificationProvider delivers notification and records timestamp")
    void inAppProvider_DeliversNotification() {
        InAppNotificationProvider provider = new InAppNotificationProvider();
        assertThat(provider.getChannel()).isEqualTo(NotificationChannel.IN_APP);

        User user = new User("sarah@carepath.io", "hash", Role.ROLE_PATIENT, "Sarah", "Jenkins");
        Notification notification = new Notification(user, NotificationType.SYSTEM, NotificationChannel.IN_APP,
                "Welcome", "Welcome to CarePath", null, "{}");

        NotificationDeliveryResult result = provider.send(notification);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getProviderMessageId()).isNotNull();
        assertThat(notification.getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("EmailNotificationProvider in dev/console mode simulates email dispatch without throwing")
    void emailProvider_ConsoleMode_Success() {
        EmailNotificationProvider provider = new EmailNotificationProvider("console", "noreply@carepath.io");
        assertThat(provider.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(provider.getProviderMode()).isEqualTo("console");
        assertThat(provider.getFromAddress()).isEqualTo("noreply@carepath.io");

        User user = new User("patient@carepath.io", "hash", Role.ROLE_PATIENT, "Sarah", "Jenkins");
        Notification notification = new Notification(user, NotificationType.RISK_ASSESSMENT_COMPLETED,
                NotificationChannel.EMAIL, "Risk Assessment Completed", "Assessment ready.", null, "{}");

        NotificationDeliveryResult result = provider.send(notification);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getProviderMessageId()).startsWith("dev-email-");
        assertThat(notification.getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("EmailNotificationProvider fails safely when recipient email is missing")
    void emailProvider_MissingRecipientEmail_FailsSafely() {
        EmailNotificationProvider provider = new EmailNotificationProvider("console", "noreply@carepath.io");

        User userWithoutEmail = new User(null, "hash", Role.ROLE_PATIENT, "Anonymous", "Patient");
        Notification notification = new Notification(userWithoutEmail, NotificationType.SYSTEM,
                NotificationChannel.EMAIL, "Alert", "No email user", null, "{}");

        NotificationDeliveryResult result = provider.send(notification);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Recipient email address is missing");
        assertThat(result.isRetryable()).isFalse();
    }
}
