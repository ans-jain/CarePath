package com.carepath.domain.repository;

import com.carepath.domain.enums.MetricType;
import com.carepath.domain.models.PatientBaseline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientBaselineRepository extends JpaRepository<PatientBaseline, UUID> {

    List<PatientBaseline> findByPatientId(UUID patientId);

    Optional<PatientBaseline> findFirstByPatientIdAndMetricTypeOrderByWindowEndDesc(
            UUID patientId, MetricType metricType);
}
