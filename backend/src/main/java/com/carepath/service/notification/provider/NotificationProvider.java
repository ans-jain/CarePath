package com.carepath.service.notification.provider;

import com.carepath.domain.enums.NotificationChannel;
import com.carepath.domain.models.Notification;

public interface NotificationProvider {

    NotificationChannel getChannel();

    NotificationDeliveryResult send(Notification notification);
}
