package com.carepath.api.controllers;

import com.carepath.api.dto.PatientIntakeRequestDTO;
import com.carepath.api.dto.PatientIntakeResponseDTO;
import com.carepath.api.dto.PatientProfileRequestDTO;
import com.carepath.api.dto.PatientProfileResponseDTO;
import com.carepath.security.UserPrincipal;
import com.carepath.service.PatientProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/patients", "/api/v1/patient"})
public class PatientController {

    private final PatientProfileService patientProfileService;

    public PatientController(PatientProfileService patientProfileService) {
        this.patientProfileService = patientProfileService;
    }

    @GetMapping({"/me", "/profile"})
    public ResponseEntity<PatientProfileResponseDTO> getCurrentPatientProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        PatientProfileResponseDTO response = patientProfileService.getProfileForCurrentUser(principal);
        return ResponseEntity.ok(response);
    }

    @PutMapping({"/me", "/profile"})
    public ResponseEntity<PatientProfileResponseDTO> updateCurrentPatientProfile(
            @Valid @RequestBody PatientProfileRequestDTO requestDTO,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        PatientProfileResponseDTO response = patientProfileService.upsertProfileForCurrentUser(requestDTO, principal, clientIp);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{patientId}")
    public ResponseEntity<PatientProfileResponseDTO> getPatientProfileById(
            @PathVariable UUID patientId,
            @AuthenticationPrincipal UserPrincipal principal) {
        PatientProfileResponseDTO response = patientProfileService.getProfileById(patientId, principal);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{patientId}")
    public ResponseEntity<PatientProfileResponseDTO> updatePatientProfileById(
            @PathVariable UUID patientId,
            @Valid @RequestBody PatientProfileRequestDTO requestDTO,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        PatientProfileResponseDTO response = patientProfileService.updateProfileById(patientId, requestDTO, principal, clientIp);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/intake", "/me/intake"})
    public ResponseEntity<PatientIntakeResponseDTO> submitIntake(
            @Valid @RequestBody PatientIntakeRequestDTO requestDTO,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        PatientIntakeResponseDTO response = patientProfileService.submitIntake(requestDTO, principal, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping({"/intake", "/me/intake"})
    public ResponseEntity<PatientIntakeResponseDTO> getIntake(
            @AuthenticationPrincipal UserPrincipal principal) {
        PatientIntakeResponseDTO response = patientProfileService.getIntakeForCurrentUser(principal);
        return ResponseEntity.ok(response);
    }

    @PutMapping({"/intake", "/me/intake"})
    public ResponseEntity<PatientIntakeResponseDTO> updateIntake(
            @Valid @RequestBody PatientIntakeRequestDTO requestDTO,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        PatientIntakeResponseDTO response = patientProfileService.submitIntake(requestDTO, principal, clientIp);
        return ResponseEntity.ok(response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
