package com.carepath.api.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthCheckControllerTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    private HealthCheckController controller;

    @BeforeEach
    void setUp() {
        controller = new HealthCheckController(dataSource);
    }

    @Test
    @DisplayName("Should return 200 UP for liveness check")
    void testLivenessCheck() {
        ResponseEntity<Map<String, Object>> response = controller.livenessCheck();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
        assertThat(response.getBody().get("service")).isEqualTo("carepath-backend");
        assertThat(response.getBody().get("timestamp")).isNotNull();
    }

    @Test
    @DisplayName("Should return 200 UP and CONNECTED for readiness check when database is healthy")
    void testReadinessCheckHealthy() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = controller.readinessCheck();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
        assertThat(response.getBody().get("database")).isEqualTo("CONNECTED");
    }

    @Test
    @DisplayName("Should return 503 DOWN for readiness check when database connection fails")
    void testReadinessCheckUnhealthy() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection refused"));

        ResponseEntity<Map<String, Object>> response = controller.readinessCheck();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("DOWN");
        assertThat(response.getBody().get("database")).isEqualTo("UNAVAILABLE");
    }
}
