package com.carepath.service.notification;

import com.carepath.domain.enums.NotificationChannel;
import com.carepath.domain.enums.NotificationType;
import com.carepath.domain.events.ReminderEvent;
import com.carepath.domain.events.RiskAssessmentCompletedEvent;
import com.carepath.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Async("notificationTaskExecutor")
    @EventListener
    public void handleRiskAssessmentCompleted(RiskAssessmentCompletedEvent event) {
        log.info("[EVENT_RECEIVED] RiskAssessmentCompletedEvent for user: {}, assessment: {}",
                event.getUserId(), event.getAssessmentId());

        String inAppTitle = "Risk Assessment Completed";
        String inAppMessage = "Your latest cardiometabolic risk assessment is ready for review.";
        String metadata = String.format("{\"assessmentId\":\"%s\",\"riskCategory\":\"%s\"}",
                event.getAssessmentId(), event.getRiskCategory());

        // 1. Dispatch In-App Notification
        notificationService.createAndDispatchNotification(
                event.getUserId(),
                NotificationType.RISK_ASSESSMENT_COMPLETED,
                NotificationChannel.IN_APP,
                inAppTitle,
                inAppMessage,
                event.getEventId() + "-inapp",
                metadata
        );

        // 2. Dispatch Email Notification (Strictly non-sensitive preview)
        String emailTitle = "CarePath: New Risk Assessment Ready";
        String emailMessage = "Hello, your latest cardiometabolic risk assessment has completed. " +
                "Please log in to your CarePath secure portal to review decision-support insights and feature attributions.";

        notificationService.createAndDispatchNotification(
                event.getUserId(),
                NotificationType.RISK_ASSESSMENT_COMPLETED,
                NotificationChannel.EMAIL,
                emailTitle,
                emailMessage,
                event.getEventId() + "-email",
                String.format("{\"assessmentId\":\"%s\"}", event.getAssessmentId())
        );
    }

    @Async("notificationTaskExecutor")
    @EventListener
    public void handleReminder(ReminderEvent event) {
        log.info("[EVENT_RECEIVED] ReminderEvent for user: {}, type: {}",
                event.getUserId(), event.getReminderType());

        // 1. In-App Reminder
        notificationService.createAndDispatchNotification(
                event.getUserId(),
                NotificationType.REMINDER,
                NotificationChannel.IN_APP,
                event.getTitle(),
                event.getMessage(),
                event.getEventId() + "-inapp",
                String.format("{\"reminderType\":\"%s\"}", event.getReminderType())
        );

        // 2. Email Reminder
        notificationService.createAndDispatchNotification(
                event.getUserId(),
                NotificationType.REMINDER,
                NotificationChannel.EMAIL,
                "CarePath Reminder: " + event.getTitle(),
                event.getMessage() + "\n\nPlease log in to CarePath to complete your health activity.",
                event.getEventId() + "-email",
                String.format("{\"reminderType\":\"%s\"}", event.getReminderType())
        );
    }
}
