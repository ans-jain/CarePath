package com.carepath.api.dto;

import com.carepath.domain.enums.AlcoholUse;
import com.carepath.domain.enums.BiologicalSex;
import com.carepath.domain.enums.SmokingStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class PatientProfileRequestDTO {

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotNull(message = "Biological sex is required")
    private BiologicalSex biologicalSex;

    @NotNull(message = "Height in cm is required")
    @DecimalMin(value = "50.0", message = "Height must be between 50.0 and 260.0 cm")
    @DecimalMax(value = "260.0", message = "Height must be between 50.0 and 260.0 cm")
    private BigDecimal heightCm;

    @NotNull(message = "Baseline weight in kg is required")
    @DecimalMin(value = "20.0", message = "Weight must be between 20.0 and 400.0 kg")
    @DecimalMax(value = "400.0", message = "Weight must be between 20.0 and 400.0 kg")
    private BigDecimal baselineWeightKg;

    private SmokingStatus smokingStatus = SmokingStatus.NEVER;

    private AlcoholUse alcoholUse = AlcoholUse.NONE;

    private Map<String, Object> medicalHistory;

    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;

    @Size(max = 30, message = "Phone cannot exceed 30 characters")
    private String phone;

    public PatientProfileRequestDTO() {
    }

    public PatientProfileRequestDTO(LocalDate dateOfBirth, BiologicalSex biologicalSex,
                                   BigDecimal heightCm, BigDecimal baselineWeightKg,
                                   SmokingStatus smokingStatus, AlcoholUse alcoholUse,
                                   Map<String, Object> medicalHistory) {
        this.dateOfBirth = dateOfBirth;
        this.biologicalSex = biologicalSex;
        this.heightCm = heightCm;
        this.baselineWeightKg = baselineWeightKg;
        this.smokingStatus = smokingStatus != null ? smokingStatus : SmokingStatus.NEVER;
        this.alcoholUse = alcoholUse != null ? alcoholUse : AlcoholUse.NONE;
        this.medicalHistory = medicalHistory;
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

    public Map<String, Object> getMedicalHistory() {
        return medicalHistory;
    }

    public void setMedicalHistory(Map<String, Object> medicalHistory) {
        this.medicalHistory = medicalHistory;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
