package com.carepath.service;

import com.carepath.api.dto.NotificationPreferenceDTO;
import com.carepath.api.dto.NotificationPreferenceUpdateRequest;
import com.carepath.api.exception.ResourceNotFoundException;
import com.carepath.domain.enums.NotificationChannel;
import com.carepath.domain.enums.NotificationType;
import com.carepath.domain.models.NotificationPreference;
import com.carepath.domain.models.User;
import com.carepath.domain.repository.NotificationPreferenceRepository;
import com.carepath.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class NotificationPreferenceService {

    private static final Logger log = LoggerFactory.getLogger(NotificationPreferenceService.class);

    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Autowired
    public NotificationPreferenceService(NotificationPreferenceRepository preferenceRepository,
                                         UserRepository userRepository,
                                         AuditLogService auditLogService) {
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public NotificationPreferenceService(NotificationPreferenceRepository preferenceRepository,
                                         UserRepository userRepository) {
        this(preferenceRepository, userRepository, null);
    }

    @Transactional
    public NotificationPreferenceDTO getPreferences(UUID userId) {
        NotificationPreference preference = getOrCreatePreferenceEntity(userId);
        return NotificationPreferenceDTO.fromEntity(preference);
    }

    @Transactional
    public NotificationPreferenceDTO updatePreferences(UUID userId, NotificationPreferenceUpdateRequest request) {
        NotificationPreference preference = getOrCreatePreferenceEntity(userId);

        if (request.getInAppEnabled() != null) {
            preference.setInAppEnabled(request.getInAppEnabled());
        }
        if (request.getEmailEnabled() != null) {
            preference.setEmailEnabled(request.getEmailEnabled());
        }
        if (request.getRiskAssessmentCompletedEnabled() != null) {
            preference.setRiskAssessmentCompletedEnabled(request.getRiskAssessmentCompletedEnabled());
        }
        if (request.getRemindersEnabled() != null) {
            preference.setRemindersEnabled(request.getRemindersEnabled());
        }
        if (request.getSystemUpdatesEnabled() != null) {
            preference.setSystemUpdatesEnabled(request.getSystemUpdatesEnabled());
        }

        NotificationPreference updated = preferenceRepository.save(preference);
        log.info("[NOTIFICATION_PREFERENCES_UPDATED] User ID: {}, InApp: {}, Email: {}",
                userId, updated.isInAppEnabled(), updated.isEmailEnabled());

        if (auditLogService != null) {
            auditLogService.recordEvent(
                    preference.getUser(),
                    preference.getUser().getEmail(),
                    null,
                    "NOTIFICATION_PREFERENCES_UPDATE",
                    "NotificationPreference",
                    updated.getId(),
                    "SUCCESS",
                    "127.0.0.1",
                    null,
                    Map.of(
                            "inApp", updated.isInAppEnabled(),
                            "email", updated.isEmailEnabled(),
                            "reminders", updated.isRemindersEnabled()
                    )
            );
        }

        return NotificationPreferenceDTO.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public boolean isChannelAndTypeEnabled(UUID userId, NotificationChannel channel, NotificationType type) {
        return preferenceRepository.findByUserId(userId)
                .map(pref -> {
                    boolean channelOk = switch (channel) {
                        case IN_APP -> pref.isInAppEnabled();
                        case EMAIL -> pref.isEmailEnabled();
                        default -> false;
                    };
                    if (!channelOk) return false;

                    return switch (type) {
                        case RISK_ASSESSMENT_COMPLETED, RISK_ASSESSMENT_FAILED -> pref.isRiskAssessmentCompletedEnabled();
                        case REMINDER -> pref.isRemindersEnabled();
                        case SYSTEM -> pref.isSystemUpdatesEnabled();
                    };
                })
                .orElse(true); // Default to true if preference record does not exist
    }

    private NotificationPreference getOrCreatePreferenceEntity(UUID userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
                    NotificationPreference newPref = new NotificationPreference(user);
                    return preferenceRepository.save(newPref);
                });
    }
}
