package com.carepath.domain.models;

import com.carepath.domain.enums.MetricType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "patient_baselines")
public class PatientBaseline {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientProfile patient;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false, length = 32)
    private MetricType metricType;

    @NotNull
    @Column(name = "window_start", nullable = false)
    private Instant windowStart;

    @NotNull
    @Column(name = "window_end", nullable = false)
    private Instant windowEnd;

    @NotNull
    @Column(name = "mean_value", nullable = false, precision = 8, scale = 2)
    private BigDecimal meanValue;

    @NotNull
    @Column(name = "median_value", nullable = false, precision = 8, scale = 2)
    private BigDecimal medianValue;

    @NotNull
    @Column(name = "std_deviation", nullable = false, precision = 8, scale = 2)
    private BigDecimal stdDeviation;

    @NotNull
    @Column(name = "p25_value", nullable = false, precision = 8, scale = 2)
    private BigDecimal p25Value;

    @NotNull
    @Column(name = "p75_value", nullable = false, precision = 8, scale = 2)
    private BigDecimal p75Value;

    @NotNull
    @Column(name = "ewma_value", nullable = false, precision = 8, scale = 2)
    private BigDecimal ewmaValue;

    @NotNull
    @Column(name = "sample_count", nullable = false)
    private Integer sampleCount;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    public PatientBaseline() {
    }

    public PatientBaseline(PatientProfile patient, MetricType metricType, Instant windowStart, Instant windowEnd,
                           BigDecimal meanValue, BigDecimal medianValue, BigDecimal stdDeviation,
                           BigDecimal p25Value, BigDecimal p75Value, BigDecimal ewmaValue, Integer sampleCount) {
        this.patient = patient;
        this.metricType = metricType;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.meanValue = meanValue;
        this.medianValue = medianValue;
        this.stdDeviation = stdDeviation;
        this.p25Value = p25Value;
        this.p75Value = p75Value;
        this.ewmaValue = ewmaValue;
        this.sampleCount = sampleCount;
    }

    @PrePersist
    protected void onCreate() {
        if (this.calculatedAt == null) {
            this.calculatedAt = Instant.now();
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

    public MetricType getMetricType() {
        return metricType;
    }

    public void setMetricType(MetricType metricType) {
        this.metricType = metricType;
    }

    public Instant getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(Instant windowStart) {
        this.windowStart = windowStart;
    }

    public Instant getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(Instant windowEnd) {
        this.windowEnd = windowEnd;
    }

    public BigDecimal getMeanValue() {
        return meanValue;
    }

    public void setMeanValue(BigDecimal meanValue) {
        this.meanValue = meanValue;
    }

    public BigDecimal getMedianValue() {
        return medianValue;
    }

    public void setMedianValue(BigDecimal medianValue) {
        this.medianValue = medianValue;
    }

    public BigDecimal getStdDeviation() {
        return stdDeviation;
    }

    public void setStdDeviation(BigDecimal stdDeviation) {
        this.stdDeviation = stdDeviation;
    }

    public BigDecimal getP25Value() {
        return p25Value;
    }

    public void setP25Value(BigDecimal p25Value) {
        this.p25Value = p25Value;
    }

    public BigDecimal getP75Value() {
        return p75Value;
    }

    public void setP75Value(BigDecimal p75Value) {
        this.p75Value = p75Value;
    }

    public BigDecimal getEwmaValue() {
        return ewmaValue;
    }

    public void setEwmaValue(BigDecimal ewmaValue) {
        this.ewmaValue = ewmaValue;
    }

    public Integer getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(Integer sampleCount) {
        this.sampleCount = sampleCount;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(Instant calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PatientBaseline that = (PatientBaseline) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
