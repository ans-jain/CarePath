package com.carepath.domain.repository;

import com.carepath.domain.enums.AlertSeverity;
import com.carepath.domain.enums.AlertStatus;
import com.carepath.domain.models.ClinicalAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClinicalAlertRepository extends JpaRepository<ClinicalAlert, UUID> {

    List<ClinicalAlert> findByPatientIdAndStatusOrderByCreatedAtDesc(UUID patientId, AlertStatus status);

    Page<ClinicalAlert> findByPatientIdOrderByCreatedAtDesc(UUID patientId, Pageable pageable);

    List<ClinicalAlert> findByPatientIdAndSeverityAndStatus(
            UUID patientId, AlertSeverity severity, AlertStatus status);

    long countByPatientIdAndStatus(UUID patientId, AlertStatus status);
}
