package com.carepath.api.controllers;

import com.carepath.api.dto.NotificationPageResponseDTO;
import com.carepath.api.dto.NotificationResponseDTO;
import com.carepath.security.UserPrincipal;
import com.carepath.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<NotificationPageResponseDTO> getNotifications(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly,
            @AuthenticationPrincipal UserPrincipal principal) {

        int validPage = Math.max(0, page);
        int validSize = Math.min(100, Math.max(1, size));
        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<NotificationResponseDTO> notificationPage = notificationService.getUserNotifications(
                principal.getId(), pageable, unreadOnly);
        long unreadCount = notificationService.getUnreadCount(principal.getId());

        NotificationPageResponseDTO response = new NotificationPageResponseDTO(
                notificationPage.getContent(),
                notificationPage.getNumber(),
                notificationPage.getSize(),
                notificationPage.getTotalElements(),
                notificationPage.getTotalPages(),
                notificationPage.isFirst(),
                notificationPage.isLast(),
                unreadCount
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        long unreadCount = notificationService.getUnreadCount(principal.getId());
        return ResponseEntity.ok(Map.of("unreadCount", unreadCount));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponseDTO> markAsRead(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        NotificationResponseDTO response = notificationService.markAsRead(id, principal.getId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal) {
        int updatedCount = notificationService.markAllAsRead(principal.getId());
        return ResponseEntity.ok(Map.of("updatedCount", updatedCount));
    }
}
