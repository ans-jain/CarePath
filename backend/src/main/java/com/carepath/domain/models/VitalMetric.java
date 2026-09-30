package com.carepath.domain.models;

import com.carepath.domain.enums.MeasurementContext;
import com.carepath.domain.enums.MeasurementSource;
import com.carepath.domain.enums.MetricType;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "vital_metrics")
public class VitalMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientProfile patient;

    @NotNull
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false, length = 32)
    private MetricType metricType;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "value", nullable = false, precision = 8, scale = 2)
    private BigDecimal value;

    @NotBlank
    @Size(max = 20)
    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "measurement_context", nullable = false, length = 32)
    private MeasurementContext measurementContext = MeasurementContext.RESTING;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 32)
    private MeasurementSource source = MeasurementSource.MANUAL;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public VitalMetric() {
    }

    public VitalMetric(PatientProfile patient, Instant recordedAt, MetricType metricType,
                       BigDecimal value, String unit, MeasurementContext context, MeasurementSource source) {
        this.patient = patient;
        this.recordedAt = recordedAt;
        this.metricType = metricType;
        this.value = value;
        this.unit = unit;
        this.measurementContext = context != null ? context : MeasurementContext.RESTING;
        this.source = source != null ? source : MeasurementSource.MANUAL;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.measurementContext == null) {
            this.measurementContext = MeasurementContext.RESTING;
        }
        if (this.source == null) {
            this.source = MeasurementSource.MANUAL;
        }
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public PatientProfile getPatient() {
        return patient;
    }

    public void setPatient(PatientProfile patient) {
        this.patient = patient;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VitalMetric that = (VitalMetric) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
