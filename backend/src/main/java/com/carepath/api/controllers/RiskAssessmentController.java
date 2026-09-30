package com.carepath.api.controllers;

import com.carepath.api.dto.RiskAssessmentRecordDTO;
import com.carepath.api.dto.RiskAssessmentResponseDTO;
import com.carepath.security.UserPrincipal;
import com.carepath.service.RiskAssessmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/risk-assessments")
public class RiskAssessmentController {

    private final RiskAssessmentService riskAssessmentService;

    public RiskAssessmentController(RiskAssessmentService riskAssessmentService) {
        this.riskAssessmentService = riskAssessmentService;
    }

    @PostMapping
    public ResponseEntity<RiskAssessmentResponseDTO> recordAssessment(
            @RequestParam(name = "patientId", required = false) UUID patientId,
            @Valid @RequestBody RiskAssessmentRecordDTO dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        RiskAssessmentResponseDTO response = riskAssessmentService.recordCompletedAssessment(patientId, dto, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/latest")
    public ResponseEntity<RiskAssessmentResponseDTO> getLatestAssessment(
            @RequestParam(name = "patientId", required = false) UUID patientId,
            @AuthenticationPrincipal UserPrincipal principal) {
        RiskAssessmentResponseDTO response = riskAssessmentService.getLatestAssessment(patientId, principal);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<RiskAssessmentResponseDTO>> getAssessmentHistory(
            @RequestParam(name = "patientId", required = false) UUID patientId,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {
        Page<RiskAssessmentResponseDTO> response = riskAssessmentService.getAssessmentHistory(patientId, pageable, principal);
        return ResponseEntity.ok(response);
    }
}
