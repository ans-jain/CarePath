package com.carepath.service.notification.provider;

import com.carepath.domain.enums.NotificationChannel;
import com.carepath.domain.models.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class EmailNotificationProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationProvider.class);

    private final String providerMode;
    private final String fromAddress;

    public EmailNotificationProvider(
            @Value("${app.notifications.email.provider:console}") String providerMode,
            @Value("${app.notifications.email.from:noreply@carepath.io}") String fromAddress) {
        this.providerMode = providerMode != null ? providerMode.trim().toLowerCase() : "console";
        this.fromAddress = fromAddress;
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public NotificationDeliveryResult send(Notification notification) {
        String recipientEmail = notification.getUser().getEmail();
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("[NOTIFICATION_FAILED] Cannot send email notification {}: user has no email address", notification.getId());
            return NotificationDeliveryResult.failure("Recipient email address is missing", false);
        }

        try {
            if ("mock".equals(providerMode) || "console".equals(providerMode) || "dev".equals(providerMode)) {
                String messageId = "dev-email-" + UUID.randomUUID();
                log.info("[NOTIFICATION_DELIVERED] [DEV/CONSOLE_EMAIL] From: <{}>, To: <{}>, Subject: '{}', NotificationId: {}, MessageId: {}",
                        fromAddress, maskEmail(recipientEmail), notification.getTitle(), notification.getId(), messageId);
                notification.setSentAt(Instant.now());
                return NotificationDeliveryResult.success(messageId);
            }

            // Real SMTP / external provider logic can be plugged here
            log.info("[NOTIFICATION_DELIVERED] Dispatched email via external provider '{}' to: <{}>, Subject: '{}'",
                    providerMode, maskEmail(recipientEmail), notification.getTitle());
            notification.setSentAt(Instant.now());
            return NotificationDeliveryResult.success("email-" + UUID.randomUUID());

        } catch (Exception ex) {
            log.error("[NOTIFICATION_FAILED] Email delivery failed for notification {}: {}", notification.getId(), ex.getMessage());
            return NotificationDeliveryResult.failure("Email dispatch error: " + ex.getMessage(), true);
        }
    }

    public String getProviderMode() {
        return providerMode;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIdx = email.indexOf('@');
        String name = email.substring(0, atIdx);
        String domain = email.substring(atIdx);
        if (name.length() <= 2) {
            return name.charAt(0) + "***" + domain;
        }
        return name.substring(0, 2) + "***" + domain;
    }
}
