package com.carepath.api.dto;

import com.carepath.domain.enums.MeasurementContext;
import com.carepath.domain.enums.MeasurementSource;
import com.carepath.domain.enums.MetricType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class VitalResponseDTO {

    private UUID id;
    private UUID patientId;
    private MetricType metricType;
    private BigDecimal value;
    private String unit;
    private Instant recordedAt;
    private MeasurementContext measurementContext;
    private MeasurementSource source;
    private Instant createdAt;
    private Boolean isBaselineAnomaly;
    private Double zScore;

    public VitalResponseDTO() {
    }

    public VitalResponseDTO(UUID id, UUID patientId, MetricType metricType, BigDecimal value,
                            String unit, Instant recordedAt, MeasurementContext measurementContext,
                            MeasurementSource source, Instant createdAt) {
        this.id = id;
        this.patientId = patientId;
        this.metricType = metricType;
        this.value = value;
        this.unit = unit;
        this.recordedAt = recordedAt;
        this.measurementContext = measurementContext;
        this.source = source;
        this.createdAt = createdAt;
    }

    public VitalResponseDTO(UUID id, UUID patientId, MetricType metricType, BigDecimal value,
                            String unit, Instant recordedAt, MeasurementContext measurementContext,
                            MeasurementSource source, Instant createdAt,
                            Boolean isBaselineAnomaly, Double zScore) {
        this.id = id;
        this.patientId = patientId;
        this.metricType = metricType;
        this.value = value;
        this.unit = unit;
        this.recordedAt = recordedAt;
        this.measurementContext = measurementContext;
        this.source = source;
        this.createdAt = createdAt;
        this.isBaselineAnomaly = isBaselineAnomaly;
        this.zScore = zScore;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public MetricType getMetricType() {
        return metricType;
    }

    public void setMetricType(MetricType metricType) {
        this.metricType = metricType;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public MeasurementContext getMeasurementContext() {
        return measurementContext;
    }

    public void setMeasurementContext(MeasurementContext measurementContext) {
        this.measurementContext = measurementContext;
    }

    public MeasurementContext getContext() {
        return measurementContext;
    }

    public MeasurementSource getSource() {
        return source;
    }

    public void setSource(MeasurementSource source) {
        this.source = source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getIsBaselineAnomaly() {
        return isBaselineAnomaly;
    }

    public void setIsBaselineAnomaly(Boolean baselineAnomaly) {
        isBaselineAnomaly = baselineAnomaly;
    }

    public Double getzScore() {
        return zScore;
    }

    public void setzScore(Double zScore) {
        this.zScore = zScore;
    }
}
