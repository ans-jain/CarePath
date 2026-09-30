package com.carepath.api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight, non-sensitive health check endpoints for liveness and readiness probing.
 */
@RestController
public class HealthCheckController {

    private final DataSource dataSource;

    @Autowired
    public HealthCheckController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> livenessCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "carepath-backend");
        response.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health/liveness")
    public ResponseEntity<Map<String, Object>> livenessProbe() {
        return livenessCheck();
    }

    @GetMapping("/health/ready")
    public ResponseEntity<Map<String, Object>> readinessCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "carepath-backend");
        response.put("timestamp", Instant.now().toString());

        boolean dbHealthy = checkDatabaseConnectivity();

        if (dbHealthy) {
            response.put("status", "UP");
            response.put("database", "CONNECTED");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "DOWN");
            response.put("database", "UNAVAILABLE");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    @GetMapping("/health/readiness")
    public ResponseEntity<Map<String, Object>> readinessProbe() {
        return readinessCheck();
    }

    private boolean checkDatabaseConnectivity() {
        if (dataSource == null) {
            return false;
        }
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }
}
