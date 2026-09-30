package com.carepath.api.dto;

import com.carepath.domain.enums.AccountStatus;
import com.carepath.domain.models.ClinicianProfile;
import com.carepath.domain.models.User;

import java.time.Instant;
import java.util.UUID;

public class DoctorRegistrationResponseDTO {

    private UUID id;
    private UUID userId;
    private String name;
    private String firstName;
    private String lastName;
    private String email;
    private String licenseNumber;
    private String specialization;
    private String hospitalOrganization;
    private String phone;
    private Instant registrationDate;
    private AccountStatus status;

    public DoctorRegistrationResponseDTO() {
    }

    public DoctorRegistrationResponseDTO(UUID id, UUID userId, String name, String firstName, String lastName,
                                         String email, String licenseNumber, String specialization,
                                         String hospitalOrganization, String phone, Instant registrationDate,
                                         AccountStatus status) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.licenseNumber = licenseNumber;
        this.specialization = specialization;
        this.hospitalOrganization = hospitalOrganization;
        this.phone = phone;
        this.registrationDate = registrationDate;
        this.status = status;
    }

    public static DoctorRegistrationResponseDTO fromUserAndProfile(User user, ClinicianProfile profile) {
        if (user == null) return null;
        String fullName = (user.getFirstName() + " " + user.getLastName()).trim();
        return new DoctorRegistrationResponseDTO(
                profile != null ? profile.getId() : user.getId(),
                user.getId(),
                fullName,
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                profile != null ? profile.getLicenseNumber() : "N/A",
                profile != null ? profile.getSpecialty() : "General Practice",
                profile != null ? profile.getClinicName() : "CarePath Clinical Network",
                user.getPhone(),
                user.getCreatedAt(),
                user.getStatus()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Instant getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(Instant registrationDate) {
        this.registrationDate = registrationDate;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }
}
