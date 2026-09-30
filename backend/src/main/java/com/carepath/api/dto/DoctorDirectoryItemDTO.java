package com.carepath.api.dto;

import com.carepath.domain.enums.AccountStatus;
import com.carepath.domain.models.ClinicianProfile;
import com.carepath.domain.models.User;

import java.util.UUID;

public class DoctorDirectoryItemDTO {

    private UUID userId;
    private UUID clinicianProfileId;
    private String name;
    private String email;
    private String specialization;
    private String hospitalOrganization;
    private String licenseNumber;
    private AccountStatus status;

    public DoctorDirectoryItemDTO() {
    }

    public DoctorDirectoryItemDTO(UUID userId, UUID clinicianProfileId, String name, String email,
                                  String specialization, String hospitalOrganization, String licenseNumber,
                                  AccountStatus status) {
        this.userId = userId;
        this.clinicianProfileId = clinicianProfileId;
        this.name = name;
        this.email = email;
        this.specialization = specialization;
        this.hospitalOrganization = hospitalOrganization;
        this.licenseNumber = licenseNumber;
        this.status = status;
    }

    public static DoctorDirectoryItemDTO fromUserAndProfile(User user, ClinicianProfile profile) {
        if (user == null) return null;
        String fullName = ("Dr. " + user.getFirstName() + " " + user.getLastName()).trim();
        return new DoctorDirectoryItemDTO(
                user.getId(),
                profile != null ? profile.getId() : null,
                fullName,
                user.getEmail(),
                profile != null ? profile.getSpecialty() : "General Practice",
                profile != null ? profile.getClinicName() : "CarePath Clinical Network",
                profile != null ? profile.getLicenseNumber() : "N/A",
                user.getStatus()
        );
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getClinicianProfileId() {
        return clinicianProfileId;
    }

    public void setClinicianProfileId(UUID clinicianProfileId) {
        this.clinicianProfileId = clinicianProfileId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getHospitalOrganization() {
        return hospitalOrganization;
    }

    public void setHospitalOrganization(String hospitalOrganization) {
        this.hospitalOrganization = hospitalOrganization;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }
}
