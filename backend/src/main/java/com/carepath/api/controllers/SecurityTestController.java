package com.carepath.api.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping({"/api/v1/test", "/api/test"})
public class SecurityTestController {

    @GetMapping("/public")
    public ResponseEntity<Map<String, String>> publicEndpoint() {
        return ResponseEntity.ok(Map.of("message", "Public endpoint accessible without authentication"));
    }

    @GetMapping("/patient")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<Map<String, String>> patientEndpoint() {
        return ResponseEntity.ok(Map.of("message", "Patient endpoint accessible only by PATIENT"));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> adminEndpoint() {
        return ResponseEntity.ok(Map.of("message", "Admin endpoint accessible only by ADMIN"));
    }

    @GetMapping("/clinician")
    @PreAuthorize("hasRole('CLINICIAN')")
    public ResponseEntity<Map<String, String>> clinicianEndpoint() {
        return ResponseEntity.ok(Map.of("message", "Clinician endpoint accessible only by CLINICIAN"));
    }
}
