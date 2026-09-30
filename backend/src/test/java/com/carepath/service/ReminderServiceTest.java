package com.carepath.service;

import com.carepath.domain.enums.Role;
import com.carepath.domain.events.ReminderEvent;
import com.carepath.domain.models.User;
import com.carepath.domain.repository.UserRepository;
import com.carepath.service.notification.ReminderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UserRepository userRepository;

    private ReminderService reminderService;

    @BeforeEach
    void setUp() {
        reminderService = new ReminderService(eventPublisher, userRepository, true);
    }

    @Test
    @DisplayName("sendVitalsReminder publishes timezone-aware idempotent ReminderEvent")
    void sendVitalsReminder_PublishesEvent() {
        UUID userId = UUID.randomUUID();

        reminderService.sendVitalsReminder(userId);

        ArgumentCaptor<ReminderEvent> captor = ArgumentCaptor.forClass(ReminderEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());

        ReminderEvent event = captor.getValue();
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getReminderType()).isEqualTo("DAILY_VITALS_LOG");
        assertThat(event.getEventId()).startsWith("vitals-reminder-" + userId);
    }

    @Test
    @DisplayName("executeScheduledVitalsReminders dispatches reminders to all active patients")
    void executeScheduledVitalsReminders_IteratesActivePatients() {
        User activePatient = new User("active@carepath.io", "hash", Role.ROLE_PATIENT, "Active", "Patient");
        User inactivePatient = new User("inactive@carepath.io", "hash", Role.ROLE_PATIENT, "Inactive", "Patient");
        inactivePatient.setActive(false);
        User clinician = new User("doc@carepath.io", "hash", Role.ROLE_CLINICIAN, "Marcus", "Vance");

        when(userRepository.findAll()).thenReturn(List.of(activePatient, inactivePatient, clinician));

        reminderService.executeScheduledVitalsReminders();

        // Only active patient should receive reminder
        verify(eventPublisher, times(1)).publishEvent(any(ReminderEvent.class));
    }

    @Test
    @DisplayName("executeScheduledVitalsReminders skips execution when globally disabled")
    void executeScheduledVitalsReminders_SkippedWhenDisabled() {
        ReminderService disabledService = new ReminderService(eventPublisher, userRepository, false);

        disabledService.executeScheduledVitalsReminders();

        verify(userRepository, never()).findAll();
        verify(eventPublisher, never()).publishEvent(any());
    }
}
