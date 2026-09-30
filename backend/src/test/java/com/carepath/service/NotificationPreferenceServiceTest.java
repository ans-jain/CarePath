package com.carepath.service;

import com.carepath.api.dto.NotificationPreferenceDTO;
import com.carepath.api.dto.NotificationPreferenceUpdateRequest;
import com.carepath.api.exception.ResourceNotFoundException;
import com.carepath.domain.enums.NotificationChannel;
import com.carepath.domain.enums.NotificationType;
import com.carepath.domain.enums.Role;
import com.carepath.domain.models.NotificationPreference;
import com.carepath.domain.models.User;
import com.carepath.domain.repository.NotificationPreferenceRepository;
import com.carepath.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private UserRepository userRepository;

    private NotificationPreferenceService preferenceService;

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

        preferenceService = new NotificationPreferenceService(preferenceRepository, userRepository);
    }

    @Test
    @DisplayName("Returns existing user preferences if already present in database")
    void getPreferences_Existing() {
        NotificationPreference existing = new NotificationPreference(sampleUser);
        existing.setEmailEnabled(false);

        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

        NotificationPreferenceDTO dto = preferenceService.getPreferences(userId);
        assertThat(dto.getUserId()).isEqualTo(userId);
        assertThat(dto.isEmailEnabled()).isFalse();
        assertThat(dto.isInAppEnabled()).isTrue();
    }

    @Test
    @DisplayName("Creates default preferences with all channels enabled when user has none")
    void getPreferences_CreatesDefault() {
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(preferenceRepository.save(any(NotificationPreference.class))).thenAnswer(i -> i.getArgument(0));

        NotificationPreferenceDTO dto = preferenceService.getPreferences(userId);
        assertThat(dto.getUserId()).isEqualTo(userId);
        assertThat(dto.isInAppEnabled()).isTrue();
        assertThat(dto.isEmailEnabled()).isTrue();
        assertThat(dto.isRiskAssessmentCompletedEnabled()).isTrue();
        verify(preferenceRepository).save(any(NotificationPreference.class));
    }

    @Test
    @DisplayName("Updates specific user notification preference toggles")
    void updatePreferences_Success() {
        NotificationPreference existing = new NotificationPreference(sampleUser);
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(preferenceRepository.save(any(NotificationPreference.class))).thenAnswer(i -> i.getArgument(0));

        NotificationPreferenceUpdateRequest update = new NotificationPreferenceUpdateRequest();
        update.setEmailEnabled(false);
        update.setRemindersEnabled(false);

        NotificationPreferenceDTO result = preferenceService.updatePreferences(userId, update);
        assertThat(result.isEmailEnabled()).isFalse();
        assertThat(result.isRemindersEnabled()).isFalse();
        assertThat(result.isInAppEnabled()).isTrue(); // Unchanged
    }

    @Test
    @DisplayName("isChannelAndTypeEnabled checks channel toggles accurately")
    void isChannelAndTypeEnabled_RespectsToggles() {
        NotificationPreference pref = new NotificationPreference(sampleUser);
        pref.setEmailEnabled(false);
        pref.setRemindersEnabled(false);

        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));

        // Email disabled
        assertThat(preferenceService.isChannelAndTypeEnabled(userId, NotificationChannel.EMAIL, NotificationType.RISK_ASSESSMENT_COMPLETED))
                .isFalse();

        // In-app enabled for risk assessment
        assertThat(preferenceService.isChannelAndTypeEnabled(userId, NotificationChannel.IN_APP, NotificationType.RISK_ASSESSMENT_COMPLETED))
                .isTrue();

        // Reminders disabled
        assertThat(preferenceService.isChannelAndTypeEnabled(userId, NotificationChannel.IN_APP, NotificationType.REMINDER))
                .isFalse();
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException if creating preferences for non-existent user")
    void getPreferences_UserNotFound() {
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> preferenceService.getPreferences(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
