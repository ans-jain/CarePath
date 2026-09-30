package com.carepath.api.controllers;

import com.carepath.api.dto.*;
import com.carepath.domain.models.AuditLog;
import com.carepath.security.UserPrincipal;
import com.carepath.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AuditLogResponseDTO>> getAuditLogs(
            @RequestParam(name = "actorUserId", required = false) UUID actorUserId,
            @RequestParam(name = "actionType", required = false) String actionType,
            @RequestParam(name = "entityName", required = false) String entityName,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLogResponseDTO> response = adminService.getAuditLogs(actorUserId, actionType, entityName, status, startDate, endDate, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserResponseDTO>> getUsers(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdminUserResponseDTO> response = adminService.getUsers(pageable);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<AdminUserResponseDTO> updateUserRole(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRoleRequest request,
            @AuthenticationPrincipal UserPrincipal adminPrincipal,
            HttpServletRequest servletRequest) {

        String clientIp = extractClientIp(servletRequest);
        AdminUserResponseDTO response = adminService.updateUserRole(userId, request.getRole(), adminPrincipal, clientIp);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<AdminUserResponseDTO> updateUserStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserStatusRequest request,
            @AuthenticationPrincipal UserPrincipal adminPrincipal,
            HttpServletRequest servletRequest) {

        String clientIp = extractClientIp(servletRequest);
        AdminUserResponseDTO response = adminService.updateUserStatus(userId, request.getActive(), adminPrincipal, clientIp);
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/system/security", "/security/overview"})
    public ResponseEntity<SecurityOverviewDTO> getSecurityOverview() {
        SecurityOverviewDTO response = adminService.getSecurityOverview();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/doctor-requests")
    public ResponseEntity<List<DoctorRegistrationResponseDTO>> getDoctorRequests(
            @RequestParam(name = "status", required = false) String status) {
        List<DoctorRegistrationResponseDTO> response = adminService.getDoctorRequests(status);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/doctor-requests/{userId}/approve", "/doctors/{userId}/approve"})
    public ResponseEntity<DoctorRegistrationResponseDTO> approveDoctorRequest(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal adminPrincipal,
            HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        DoctorRegistrationResponseDTO response = adminService.approveDoctorRequest(userId, adminPrincipal, clientIp);
        return ResponseEntity.ok(response);
    }

    @PatchMapping({"/doctor-requests/{userId}/approve", "/doctors/{userId}/approve"})
    public ResponseEntity<DoctorRegistrationResponseDTO> patchApproveDoctorRequest(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal adminPrincipal,
            HttpServletRequest servletRequest) {
        return approveDoctorRequest(userId, adminPrincipal, servletRequest);
    }

    @PostMapping({"/doctor-requests/{userId}/reject", "/doctors/{userId}/reject"})
    public ResponseEntity<DoctorRegistrationResponseDTO> rejectDoctorRequest(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal adminPrincipal,
            HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        DoctorRegistrationResponseDTO response = adminService.rejectDoctorRequest(userId, adminPrincipal, clientIp);
        return ResponseEntity.ok(response);
    }

    @PatchMapping({"/doctor-requests/{userId}/reject", "/doctors/{userId}/reject"})
    public ResponseEntity<DoctorRegistrationResponseDTO> patchRejectDoctorRequest(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal adminPrincipal,
            HttpServletRequest servletRequest) {
        return rejectDoctorRequest(userId, adminPrincipal, servletRequest);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
