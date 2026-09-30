package com.carepath.api.controllers;

import com.carepath.api.dto.*;
import com.carepath.domain.enums.MetricType;
import com.carepath.security.UserPrincipal;
import com.carepath.service.VitalMetricService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
public class VitalController {

    private final VitalMetricService vitalMetricService;

    public VitalController(VitalMetricService vitalMetricService) {
        this.vitalMetricService = vitalMetricService;
    }

    @PostMapping({"/api/v1/vitals", "/api/v1/patients/{patientId}/vitals"})
    public ResponseEntity<VitalResponseDTO> recordVital(
            @PathVariable(name = "patientId", required = false) UUID patientId,
            @Valid @RequestBody CreateVitalRequestDTO requestDTO,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        VitalResponseDTO response = vitalMetricService.recordVital(requestDTO, patientId, principal, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping({"/api/v1/vitals/batch", "/api/v1/patients/{patientId}/vitals/batch"})
    public ResponseEntity<List<VitalResponseDTO>> recordBatchVitals(
            @PathVariable(name = "patientId", required = false) UUID patientId,
            @Valid @RequestBody BatchCreateVitalRequestDTO requestDTO,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        List<VitalResponseDTO> response = vitalMetricService.recordBatchVitals(requestDTO, patientId, principal, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping({"/api/v1/vitals", "/api/v1/patients/{patientId}/vitals"})
    public ResponseEntity<VitalPageResponseDTO> getVitals(
            @PathVariable(name = "patientId", required = false) UUID patientId,
            @RequestParam(name = "metricType", required = false) MetricType metricType,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        Instant effectiveStart = startDate != null ? startDate : from;
        Instant effectiveEnd = endDate != null ? endDate : to;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "recordedAt"));
        VitalPageResponseDTO response = vitalMetricService.getVitals(patientId, metricType, effectiveStart, effectiveEnd, pageable, principal);
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/api/v1/vitals/latest", "/api/v1/patients/{patientId}/vitals/latest"})
    public ResponseEntity<List<VitalResponseDTO>> getLatestVitals(
            @PathVariable(name = "patientId", required = false) UUID patientId,
            @RequestParam(name = "metricType", required = false) MetricType metricType,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<VitalResponseDTO> response = vitalMetricService.getLatestVitals(patientId, metricType, principal);
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/api/v1/vitals/{id}", "/api/v1/patients/{patientId}/vitals/{id}"})
    public ResponseEntity<VitalResponseDTO> getVitalById(
            @PathVariable(name = "id") UUID id,
            @PathVariable(name = "patientId", required = false) UUID patientId,
            @AuthenticationPrincipal UserPrincipal principal) {
        VitalResponseDTO response = vitalMetricService.getVitalById(id, patientId, principal);
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
