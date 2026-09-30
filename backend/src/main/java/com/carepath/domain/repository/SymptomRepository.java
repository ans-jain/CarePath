package com.carepath.domain.repository;

import com.carepath.domain.models.SymptomLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SymptomRepository extends JpaRepository<SymptomLog, UUID> {

    List<SymptomLog> findByPatientIdOrderByRecordedAtDesc(UUID patientId);

    Page<SymptomLog> findByPatientIdOrderByRecordedAtDesc(UUID patientId, Pageable pageable);

    List<SymptomLog> findByPatientIdAndRecordedAtBetweenOrderByRecordedAtDesc(
            UUID patientId, Instant start, Instant end);
}
