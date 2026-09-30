package com.carepath.service;

import com.carepath.domain.enums.NotificationChannel;
import com.carepath.domain.enums.NotificationType;
import com.carepath.domain.events.ReminderEvent;
import com.carepath.domain.events.RiskAssessmentCompletedEvent;
import com.carepath.service.notification.NotificationEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    private NotificationEventListener eventListener;

    @BeforeEach
    void setUp() {
        eventListener = new NotificationEventListener(notificationService);
    }

    @Test
    @DisplayName("RiskAssessmentCompletedEvent triggers both In-App and Email notifications with non-sensitive preview")
    void handleRiskAssessmentCompleted_DispatchesBothChannels() {
        UUID userId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID assessmentId = UUID.randomUUID();

        RiskAssessmentCompletedEvent event = new RiskAssessmentCompletedEvent(
                userId,
                patientId,
                assessmentId,
                "ELEVATED",
                new BigDecimal("0.650"),
                "assessment-" + assessmentId
        );

        eventListener.handleRiskAssessmentCompleted(event);

        // 1. Verify In-App dispatch
        verify(notificationService, times(1)).createAndDispatchNotification(
                eq(userId),
                eq(NotificationType.RISK_ASSESSMENT_COMPLETED),
                eq(NotificationChannel.IN_APP),
                eq("Risk Assessment Completed"),
                eq("Your latest cardiometabolic risk assessment is ready for review."),
                eq("assessment-" + assessmentId + "-inapp"),
                contains(assessmentId.toString())
        );

        // 2. Verify Email dispatch with non-sensitive message
        verify(notificationService, times(1)).createAndDispatchNotification(
                eq(userId),
                eq(NotificationType.RISK_ASSESSMENT_COMPLETED),
                eq(NotificationChannel.EMAIL),
                eq("CarePath: New Risk Assessment Ready"),
                contains("Please log in to your CarePath secure portal"),
                eq("assessment-" + assessmentId + "-email"),
                contains(assessmentId.toString())
        );
    }

    @Test
    @DisplayName("ReminderEvent triggers In-App and Email reminders")
    void handleReminder_DispatchesReminders() {
        UUID userId = UUID.randomUUID();
        ReminderEvent event = new ReminderEvent(
                userId,
                "DAILY_VITALS_LOG",
                "Daily Vitals Log Reminder",
                "Please record your morning vitals.",
                "reminder-123"
        );

        eventListener.handleReminder(event);

        verify(notificationService, times(1)).createAndDispatchNotification(
                eq(userId),
                eq(NotificationType.REMINDER),
                eq(NotificationChannel.IN_APP),
                eq("Daily Vitals Log Reminder"),
                eq("Please record your morning vitals."),
                eq("reminder-123-inapp"),
                contains("DAILY_VITALS_LOG")
        );

        verify(notificationService, times(1)).createAndDispatchNotification(
                eq(userId),
                eq(NotificationType.REMINDER),
                eq(NotificationChannel.EMAIL),
                contains("Daily Vitals Log Reminder"),
                contains("Please record your morning vitals."),
                eq("reminder-123-email"),
                contains("DAILY_VITALS_LOG")
        );
    }
}
