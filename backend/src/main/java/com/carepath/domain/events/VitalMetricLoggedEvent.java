package com.carepath.domain.events;

import com.carepath.domain.enums.MetricType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class VitalMetricLoggedEvent {

    private final UUID vitalId;
    private final UUID patientId;
    private final MetricType metricType;
    private final BigDecimal value;
    private final String unit;
    private final Instant recordedAt;

    public VitalMetricLoggedEvent(UUID vitalId, UUID patientId, MetricType metricType,
                                  BigDecimal value, String unit, Instant recordedAt) {
        this.vitalId = vitalId;
        this.patientId = patientId;
        this.metricType = metricType;
        this.value = value;
        this.unit = unit;
        this.recordedAt = recordedAt;
    }

    public UUID getVitalId() {
        return vitalId;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public MetricType getMetricType() {
        return metricType;
    }

    public BigDecimal getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
