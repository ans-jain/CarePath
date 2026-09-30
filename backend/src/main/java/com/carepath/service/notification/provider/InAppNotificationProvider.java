package com.carepath.service.notification.provider;

import com.carepath.domain.enums.NotificationChannel;
import com.carepath.domain.models.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class InAppNotificationProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(InAppNotificationProvider.class);

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public NotificationDeliveryResult send(Notification notification) {
        log.info("[NOTIFICATION_DELIVERED] In-app notification available for user ID: {}, type: {}, title: {}",
                notification.getUser().getId(), notification.getType(), notification.getTitle());
        notification.setSentAt(Instant.now());
        return NotificationDeliveryResult.success("inapp-" + notification.getId());
    }
}
