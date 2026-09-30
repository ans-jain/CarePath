package com.carepath.api.dto;

import com.carepath.domain.enums.AlcoholUse;
import com.carepath.domain.enums.BiologicalSex;
import com.carepath.domain.enums.SmokingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public class PatientIntakeResponseDTO {

    private UUID patientId;
    private UUID userId;
    private LocalDate dateOfBirth;
    private BiologicalSex biologicalSex;
    private BigDecimal heightCm;
    private BigDecimal baselineWeightKg;
    private BigDecimal currentBmi;
    private SmokingStatus smokingStatus;
    private AlcoholUse alcoholUse;
    private Map<String, Object> medicalHistory;
    private Instant completedAt;
    private String message;

    public PatientIntakeResponseDTO() {
    }

    public PatientIntakeResponseDTO(UUID patientId, UUID userId, LocalDate dateOfBirth,
                                    BiologicalSex biologicalSex, BigDecimal heightCm,
                                    BigDecimal baselineWeightKg, BigDecimal currentBmi,
                                    SmokingStatus smokingStatus, AlcoholUse alcoholUse,
                                    Map<String, Object> medicalHistory, Instant completedAt,
                                    String message) {
        this.patientId = patientId;
        this.userId = userId;
        this.dateOfBirth = dateOfBirth;
        this.biologicalSex = biologicalSex;
        this.heightCm = heightCm;
        this.baselineWeightKg = baselineWeightKg;
        this.currentBmi = currentBmi;
        this.smokingStatus = smokingStatus;
        this.alcoholUse = alcoholUse;
        this.medicalHistory = medicalHistory;
        this.completedAt = completedAt;
        this.message = message;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
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

    public BigDecimal getCurrentBmi() {
        return currentBmi;
    }

    public void setCurrentBmi(BigDecimal currentBmi) {
        this.currentBmi = currentBmi;
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

    public Map<String, Object> getMedicalHistory() {
        return medicalHistory;
    }

    public void setMedicalHistory(Map<String, Object> medicalHistory) {
        this.medicalHistory = medicalHistory;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
