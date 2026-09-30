package com.carepath.api.controllers;

import com.carepath.api.dto.NotificationPreferenceDTO;
import com.carepath.api.dto.NotificationPreferenceUpdateRequest;
import com.carepath.security.UserPrincipal;
import com.carepath.service.NotificationPreferenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notification-preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    public NotificationPreferenceController(NotificationPreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public ResponseEntity<NotificationPreferenceDTO> getPreferences(
            @AuthenticationPrincipal UserPrincipal principal) {
        NotificationPreferenceDTO preferences = preferenceService.getPreferences(principal.getId());
        return ResponseEntity.ok(preferences);
    }

    @PutMapping
    public ResponseEntity<NotificationPreferenceDTO> updatePreferences(
            @RequestBody NotificationPreferenceUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        NotificationPreferenceDTO updated = preferenceService.updatePreferences(principal.getId(), request);
        return ResponseEntity.ok(updated);
    }
}
