package com.carepath.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class ReportShareRequestDTO {

    @NotNull(message = "reportId must not be null")
    private UUID reportId;

    @NotNull(message = "doctorId must not be null")
    private UUID doctorId;

    public ReportShareRequestDTO() {
    }

    public ReportShareRequestDTO(UUID reportId, UUID doctorId) {
        this.reportId = reportId;
        this.doctorId = doctorId;
    }

    public UUID getReportId() {
        return reportId;
    }

    public void setReportId(UUID reportId) {
        this.reportId = reportId;
    }

    public UUID getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(UUID doctorId) {
        this.doctorId = doctorId;
    }
}
