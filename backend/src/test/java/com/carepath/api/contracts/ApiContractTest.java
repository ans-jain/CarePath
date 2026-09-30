package com.carepath.api.contracts;

import com.carepath.api.dto.AuditLogResponseDTO;
import com.carepath.api.dto.RiskAssessmentRecordDTO;
import com.carepath.api.dto.RiskAssessmentResponseDTO;
import com.carepath.api.dto.VitalResponseDTO;
import com.carepath.domain.enums.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApiContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("RiskAssessmentRecordDTO contract serialization")
    void testRiskAssessmentRecordDTOContract() throws Exception {
        RiskAssessmentRecordDTO dto = new RiskAssessmentRecordDTO();
        dto.setModelVersion("calibrated_v1.0.0");
        dto.setOverallRiskScore(new BigDecimal("0.450"));
        dto.setRiskCategory(RiskCategory.MODERATE);
        dto.setConfidenceLevel(ConfidenceLevel.LONGITUDINAL_ROBUST);
        dto.setFeatureSnapshot("{\"systolic_bp\": 125.0}");

        String json = objectMapper.writeValueAsString(dto);
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.has("modelVersion")).isTrue();
        assertThat(node.has("overallRiskScore")).isTrue();
        assertThat(node.has("riskCategory")).isTrue();
        assertThat(node.has("confidenceLevel")).isTrue();
        assertThat(node.has("featureSnapshot")).isTrue();
        assertThat(node.get("riskCategory").asText()).isEqualTo("MODERATE");
        assertThat(node.get("confidenceLevel").asText()).isEqualTo("LONGITUDINAL_ROBUST");
    }

    @Test
    @DisplayName("VitalResponseDTO contract serialization")
    void testVitalResponseDTOContract() throws Exception {
        VitalResponseDTO dto = new VitalResponseDTO();
        dto.setId(UUID.randomUUID());
        dto.setPatientId(UUID.randomUUID());
        dto.setRecordedAt(Instant.now());
        dto.setMetricType(MetricType.SYSTOLIC_BP);
        dto.setValue(new BigDecimal("120.00"));
        dto.setUnit("mmHg");
        dto.setMeasurementContext(MeasurementContext.RESTING);
        dto.setSource(MeasurementSource.MANUAL);

        String json = objectMapper.writeValueAsString(dto);
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.has("id")).isTrue();
        assertThat(node.has("patientId")).isTrue();
        assertThat(node.has("metricType")).isTrue();
        assertThat(node.has("value")).isTrue();
        assertThat(node.has("unit")).isTrue();
        assertThat(node.has("measurementContext")).isTrue();
        assertThat(node.has("source")).isTrue();
        assertThat(node.has("recordedAt")).isTrue();
    }

    @Test
    @DisplayName("AuditLogResponseDTO contract serialization")
    void testAuditLogResponseDTOContract() throws Exception {
        UUID logId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();

        AuditLogResponseDTO dto = new AuditLogResponseDTO(
                logId,
                actorId,
                "admin@carepath.io",
                null,
                "LOGIN_SUCCESS",
                "User",
                entityId,
                "127.0.0.1",
                "Mozilla/5.0",
                "SUCCESS",
                "Authentication succeeded",
                Instant.now()
        );

        String json = objectMapper.writeValueAsString(dto);
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.has("id")).isTrue();
        assertThat(node.has("actionType")).isTrue();
        assertThat(node.has("actorEmail")).isTrue();
        assertThat(node.has("status")).isTrue();
        assertThat(node.has("ipAddress")).isTrue();
        assertThat(node.has("userAgent")).isTrue();
        assertThat(node.get("status").asText()).isEqualTo("SUCCESS");
        assertThat(node.get("actionType").asText()).isEqualTo("LOGIN_SUCCESS");
    }
}
