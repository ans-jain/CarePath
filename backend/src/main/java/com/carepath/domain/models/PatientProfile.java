package com.carepath.domain.models;

import com.carepath.domain.enums.AlcoholUse;
import com.carepath.domain.enums.BiologicalSex;
import com.carepath.domain.enums.SmokingStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "patient_profiles", uniqueConstraints = {
    @UniqueConstraint(name = "uk_patient_profiles_user_id", columnNames = "user_id")
})
public class PatientProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @NotNull
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "biological_sex", nullable = false, length = 16)
    private BiologicalSex biologicalSex;

    @NotNull
    @DecimalMin("50.0")
    @DecimalMax("260.0")
    @Column(name = "height_cm", nullable = false, precision = 5, scale = 2)
    private BigDecimal heightCm;

    @NotNull
    @DecimalMin("20.0")
    @DecimalMax("400.0")
    @Column(name = "baseline_weight_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal baselineWeightKg;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "smoking_status", nullable = false, length = 32)
    private SmokingStatus smokingStatus = SmokingStatus.NEVER;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "alcohol_use", nullable = false, length = 32)
    private AlcoholUse alcoholUse = AlcoholUse.NONE;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "medical_history", nullable = false, columnDefinition = "jsonb")
    private String medicalHistory = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PatientProfile() {
    }

    public PatientProfile(User user, LocalDate dateOfBirth, BiologicalSex biologicalSex,
                          BigDecimal heightCm, BigDecimal baselineWeightKg) {
        this.user = user;
        this.dateOfBirth = dateOfBirth;
        this.biologicalSex = biologicalSex;
        this.heightCm = heightCm;
        this.baselineWeightKg = baselineWeightKg;
        this.smokingStatus = SmokingStatus.NEVER;
        this.alcoholUse = AlcoholUse.NONE;
        this.medicalHistory = "{}";
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.medicalHistory == null) {
            this.medicalHistory = "{}";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
        if (this.medicalHistory == null) {
            this.medicalHistory = "{}";
        }
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public BiologicalSex getBiologicalSex() {
        return biologicalSex;
    }

    public void setBiologicalSex(BiologicalSex biologicalSex) {
        this.biologicalSex = biologicalSex;
    }

    public BigDecimal getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(BigDecimal heightCm) {
        this.heightCm = heightCm;
    }

    public BigDecimal getBaselineWeightKg() {
        return baselineWeightKg;
    }

    public void setBaselineWeightKg(BigDecimal baselineWeightKg) {
        this.baselineWeightKg = baselineWeightKg;
    }

    public SmokingStatus getSmokingStatus() {
        return smokingStatus;
    }

    public void setSmokingStatus(SmokingStatus smokingStatus) {
        this.smokingStatus = smokingStatus;
    }

    public AlcoholUse getAlcoholUse() {
        return alcoholUse;
    }

    public void setAlcoholUse(AlcoholUse alcoholUse) {
        this.alcoholUse = alcoholUse;
    }

    public String getMedicalHistory() {
        return medicalHistory;
    }

    public void setMedicalHistory(String medicalHistory) {
        this.medicalHistory = medicalHistory;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PatientProfile that = (PatientProfile) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
