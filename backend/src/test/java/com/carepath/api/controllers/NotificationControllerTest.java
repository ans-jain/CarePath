package com.carepath.api.controllers;

import com.carepath.api.dto.NotificationPreferenceDTO;
import com.carepath.api.dto.NotificationPreferenceUpdateRequest;
import com.carepath.api.dto.NotificationResponseDTO;
import com.carepath.domain.enums.NotificationChannel;
import com.carepath.domain.enums.NotificationStatus;
import com.carepath.domain.enums.NotificationType;
import com.carepath.domain.enums.Role;
import com.carepath.domain.models.User;
import com.carepath.security.UserPrincipal;
import com.carepath.service.NotificationPreferenceService;
import com.carepath.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationPreferenceService preferenceService;

    private MockMvc notificationMockMvc;
    private MockMvc preferenceMockMvc;
    private ObjectMapper objectMapper;

    private UserPrincipal samplePrincipal;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        User user = new User("sarah@carepath.io", "pass", Role.ROLE_PATIENT, "Sarah", "Jenkins");
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, userId);
        } catch (Exception ignored) {}
        samplePrincipal = UserPrincipal.create(user);

        objectMapper = new ObjectMapper();

        // Custom argument resolver to inject mock authenticated UserPrincipal
        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return samplePrincipal;
            }
        };

        NotificationController notificationController = new NotificationController(notificationService);
        notificationMockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();

        NotificationPreferenceController preferenceController = new NotificationPreferenceController(preferenceService);
        preferenceMockMvc = MockMvcBuilders.standaloneSetup(preferenceController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/notifications returns paginated notifications and unread count")
    void getNotifications_ReturnsPage() throws Exception {
        NotificationResponseDTO item = new NotificationResponseDTO();
        item.setId(UUID.randomUUID());
        item.setTitle("Risk Assessment Completed");
        item.setMessage("Your assessment is ready.");
        item.setType(NotificationType.RISK_ASSESSMENT_COMPLETED);
        item.setChannel(NotificationChannel.IN_APP);
        item.setStatus(NotificationStatus.SENT);
        item.setCreatedAt(Instant.now());

        when(notificationService.getUserNotifications(eq(userId), any(Pageable.class), eq(false)))
                .thenReturn(new PageImpl<>(List.of(item)));
        when(notificationService.getUnreadCount(userId)).thenReturn(1L);

        notificationMockMvc.perform(get("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title", is("Risk Assessment Completed")))
                .andExpect(jsonPath("$.unreadCount", is(1)));
    }

    @Test
    @DisplayName("GET /api/v1/notifications/unread-count returns unread count map")
    void getUnreadCount_ReturnsCountMap() throws Exception {
        when(notificationService.getUnreadCount(userId)).thenReturn(4L);

        notificationMockMvc.perform(get("/api/v1/notifications/unread-count")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount", is(4)));
    }

    @Test
    @DisplayName("PATCH /api/v1/notifications/{id}/read marks notification as read")
    void markAsRead_Success() throws Exception {
        UUID notifId = UUID.randomUUID();
        NotificationResponseDTO readNotif = new NotificationResponseDTO();
        readNotif.setId(notifId);
        readNotif.setRead(true);
        readNotif.setStatus(NotificationStatus.READ);

        when(notificationService.markAsRead(notifId, userId)).thenReturn(readNotif);

        notificationMockMvc.perform(patch("/api/v1/notifications/" + notifId + "/read")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(notifId.toString())))
                .andExpect(jsonPath("$.read", is(true)))
                .andExpect(jsonPath("$.status", is("READ")));
    }

    @Test
    @DisplayName("PATCH /api/v1/notifications/read-all marks all unread notifications as read")
    void markAllAsRead_Success() throws Exception {
        when(notificationService.markAllAsRead(userId)).thenReturn(3);

        notificationMockMvc.perform(patch("/api/v1/notifications/read-all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount", is(3)));
    }

    @Test
    @DisplayName("GET /api/v1/notification-preferences returns current user preferences")
    void getPreferences_Success() throws Exception {
        NotificationPreferenceDTO dto = new NotificationPreferenceDTO();
        dto.setUserId(userId);
        dto.setInAppEnabled(true);
        dto.setEmailEnabled(false);
        dto.setRiskAssessmentCompletedEnabled(true);
        dto.setRemindersEnabled(true);
        dto.setSystemUpdatesEnabled(true);

        when(preferenceService.getPreferences(userId)).thenReturn(dto);

        preferenceMockMvc.perform(get("/api/v1/notification-preferences")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(userId.toString())))
                .andExpect(jsonPath("$.inAppEnabled", is(true)))
                .andExpect(jsonPath("$.emailEnabled", is(false)));
    }

    @Test
    @DisplayName("PUT /api/v1/notification-preferences updates user preferences")
    void updatePreferences_Success() throws Exception {
        NotificationPreferenceUpdateRequest updateReq = new NotificationPreferenceUpdateRequest();
        updateReq.setEmailEnabled(true);

        NotificationPreferenceDTO updatedDto = new NotificationPreferenceDTO();
        updatedDto.setUserId(userId);
        updatedDto.setInAppEnabled(true);
        updatedDto.setEmailEnabled(true);

        when(preferenceService.updatePreferences(eq(userId), any(NotificationPreferenceUpdateRequest.class)))
                .thenReturn(updatedDto);

        preferenceMockMvc.perform(put("/api/v1/notification-preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailEnabled", is(true)));
    }
}
