package com.carepath.api.dto;

import com.carepath.domain.enums.MeasurementContext;
import com.carepath.domain.enums.MeasurementSource;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.time.Instant;
import java.util.List;

public class BatchCreateVitalRequestDTO {

    private Instant recordedAt;

    @JsonAlias({"measurementContext", "context", "default_context"})
    private MeasurementContext defaultContext;

    @JsonAlias({"source", "default_source"})
    private MeasurementSource defaultSource;

    @NotEmpty(message = "entries list cannot be empty")
    private List<@Valid BatchVitalEntryDTO> entries;

    public BatchCreateVitalRequestDTO() {
    }

    public BatchCreateVitalRequestDTO(Instant recordedAt, List<BatchVitalEntryDTO> entries) {
        this.recordedAt = recordedAt;
        this.entries = entries;
    }

    public BatchCreateVitalRequestDTO(Instant recordedAt, MeasurementContext defaultContext,
                                     MeasurementSource defaultSource, List<BatchVitalEntryDTO> entries) {
        this.recordedAt = recordedAt;
        this.defaultContext = defaultContext;
        this.defaultSource = defaultSource;
        this.entries = entries;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public MeasurementContext getDefaultContext() {
        return defaultContext;
    }

    public void setDefaultContext(MeasurementContext defaultContext) {
        this.defaultContext = defaultContext;
    }

    public MeasurementSource getDefaultSource() {
        return defaultSource;
    }

    public void setDefaultSource(MeasurementSource defaultSource) {
        this.defaultSource = defaultSource;
    }

    public List<BatchVitalEntryDTO> getEntries() {
        return entries;
    }

    public void setEntries(List<BatchVitalEntryDTO> entries) {
        this.entries = entries;
    }
}
