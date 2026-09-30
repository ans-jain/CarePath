package com.carepath.service.notification;

import com.carepath.domain.enums.Role;
import com.carepath.domain.events.ReminderEvent;
import com.carepath.domain.models.User;
import com.carepath.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;
    private final boolean remindersEnabled;

    public ReminderService(
            ApplicationEventPublisher eventPublisher,
            UserRepository userRepository,
            @Value("${app.notifications.reminders.enabled:true}") boolean remindersEnabled) {
        this.eventPublisher = eventPublisher;
        this.userRepository = userRepository;
        this.remindersEnabled = remindersEnabled;
    }

    /**
     * Dispatches an idempotent vitals logging reminder for a specific patient.
     * Safe against duplicate triggers on the same calendar date.
     */
    public void sendVitalsReminder(UUID userId) {
        String todayUtc = LocalDate.now(ZoneOffset.UTC).toString();
        String eventId = String.format("vitals-reminder-%s-%s", userId, todayUtc);

        ReminderEvent event = new ReminderEvent(
                userId,
                "DAILY_VITALS_LOG",
                "Daily Vitals Log Reminder",
                "Please remember to record your resting blood pressure and daily biomarkers in CarePath.",
                eventId
        );

        eventPublisher.publishEvent(event);
        log.info("[REMINDER_TRIGGERED] Published vitals reminder for user: {}, eventId: {}", userId, eventId);
    }

    /**
     * Optional background cron scheduler for daily reminders across active patients.
     * Runs daily at 09:00 UTC if enabled.
     */
    @Scheduled(cron = "${app.notifications.reminders.cron:0 0 9 * * *}")
    public void executeScheduledVitalsReminders() {
        if (!remindersEnabled) {
            log.debug("[SCHEDULED_REMINDER_SKIPPED] Reminders are globally disabled via configuration.");
            return;
        }

        log.info("[SCHEDULED_REMINDER_START] Executing daily vitals check-in reminder task...");
        List<User> activePatients = userRepository.findAll().stream()
                .filter(u -> u.isActive() && u.getRole() == Role.ROLE_PATIENT)
                .toList();

        for (User patient : activePatients) {
            try {
                sendVitalsReminder(patient.getId());
            } catch (Exception ex) {
                log.error("[SCHEDULED_REMINDER_ERROR] Failed to dispatch reminder for patient {}: {}",
                        patient.getId(), ex.getMessage());
            }
        }
        log.info("[SCHEDULED_REMINDER_COMPLETE] Processed reminders for {} active patients.", activePatients.size());
    }
}
