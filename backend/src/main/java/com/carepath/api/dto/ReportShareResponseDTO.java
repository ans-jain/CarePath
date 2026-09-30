package com.carepath.api.dto;

import com.carepath.domain.enums.ReportShareStatus;
import com.carepath.domain.models.ClinicianProfile;
import com.carepath.domain.models.ReportShare;

import java.time.Instant;
import java.util.UUID;

public class ReportShareResponseDTO {

    private UUID id;
    private UUID reportId;
    private UUID patientId;
    private String patientName;
    private UUID doctorId;
    private String doctorName;
    private String doctorEmail;
    private String doctorSpecialty;
    private String doctorClinic;
    private Instant sharedAt;
    private Instant viewedAt;
    private Instant revokedAt;
    private ReportShareStatus status;

    public ReportShareResponseDTO() {
    }

    public static ReportShareResponseDTO fromEntity(ReportShare share, ClinicianProfile clinicianProfile) {
        if (share == null) return null;
        ReportShareResponseDTO dto = new ReportShareResponseDTO();
        dto.setId(share.getId());
        dto.setReportId(share.getReport().getId());
        dto.setPatientId(share.getPatient().getId());

        if (share.getPatient().getUser() != null) {
            dto.setPatientName((share.getPatient().getUser().getFirstName() + " " +
                    share.getPatient().getUser().getLastName()).trim());
        }

        if (share.getDoctor() != null) {
            dto.setDoctorId(share.getDoctor().getId());
            dto.setDoctorName(("Dr. " + share.getDoctor().getFirstName() + " " +
                    share.getDoctor().getLastName()).trim());
            dto.setDoctorEmail(share.getDoctor().getEmail());
        }

        if (clinicianProfile != null) {
            dto.setDoctorSpecialty(clinicianProfile.getSpecialty());
            dto.setDoctorClinic(clinicianProfile.getClinicName());
        }

        dto.setSharedAt(share.getSharedAt());
        dto.setViewedAt(share.getViewedAt());
        dto.setRevokedAt(share.getRevokedAt());
        dto.setStatus(share.getStatus());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getReportId() {
        return reportId;
    }

    public void setReportId(UUID reportId) {
        this.reportId = reportId;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public UUID getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(UUID doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getDoctorEmail() {
        return doctorEmail;
    }

    public void setDoctorEmail(String doctorEmail) {
        this.doctorEmail = doctorEmail;
    }

    public String getDoctorSpecialty() {
        return doctorSpecialty;
    }

    public void setDoctorSpecialty(String doctorSpecialty) {
        this.doctorSpecialty = doctorSpecialty;
    }

    public String getDoctorClinic() {
        return doctorClinic;
    }

    public void setDoctorClinic(String doctorClinic) {
        this.doctorClinic = doctorClinic;
    }

    public Instant getSharedAt() {
        return sharedAt;
    }

    public void setSharedAt(Instant sharedAt) {
        this.sharedAt = sharedAt;
    }

    public Instant getViewedAt() {
        return viewedAt;
    }

    public void setViewedAt(Instant viewedAt) {
        this.viewedAt = viewedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public ReportShareStatus getStatus() {
        return status;
    }

    public void setStatus(ReportShareStatus status) {
        this.status = status;
    }
}
