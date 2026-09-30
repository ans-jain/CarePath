package com.carepath.api.dto;

import com.carepath.domain.enums.MeasurementContext;
import com.carepath.domain.enums.MeasurementSource;
import com.carepath.domain.enums.MetricType;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public class CreateVitalRequestDTO {

    private Instant recordedAt;

    @NotNull(message = "metricType is required")
    private MetricType metricType;

    @NotNull(message = "value is required")
    private BigDecimal value;

    private String unit;

    @JsonAlias({"context", "measurement_context"})
    private MeasurementContext measurementContext;

    private MeasurementSource source;

    public CreateVitalRequestDTO() {
    }

    public CreateVitalRequestDTO(Instant recordedAt, MetricType metricType, BigDecimal value,
                                 String unit, MeasurementContext measurementContext, MeasurementSource source) {
        this.recordedAt = recordedAt;
        this.metricType = metricType;
        this.value = value;
        this.unit = unit;
        this.measurementContext = measurementContext;
        this.source = source;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
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

    public MeasurementContext getMeasurementContext() {
        return measurementContext;
    }

    public void setMeasurementContext(MeasurementContext measurementContext) {
        this.measurementContext = measurementContext;
    }

    public void setContext(MeasurementContext context) {
        this.measurementContext = context;
    }

    public MeasurementSource getSource() {
        return source;
    }

    public void setSource(MeasurementSource source) {
        this.source = source;
    }
}
