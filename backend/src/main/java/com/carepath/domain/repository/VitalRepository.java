package com.carepath.domain.repository;

import com.carepath.domain.enums.MetricType;
import com.carepath.domain.models.VitalMetric;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VitalRepository extends JpaRepository<VitalMetric, UUID>, JpaSpecificationExecutor<VitalMetric> {

    List<VitalMetric> findByPatientIdOrderByRecordedAtDesc(UUID patientId);

    Page<VitalMetric> findByPatientIdOrderByRecordedAtDesc(UUID patientId, Pageable pageable);

    List<VitalMetric> findByPatientIdAndMetricTypeOrderByRecordedAtDesc(UUID patientId, MetricType metricType);

    Optional<VitalMetric> findFirstByPatientIdAndMetricTypeOrderByRecordedAtDesc(UUID patientId, MetricType metricType);

    List<VitalMetric> findByPatientIdAndMetricTypeAndRecordedAtBetweenOrderByRecordedAtAsc(
            UUID patientId, MetricType metricType, Instant start, Instant end);

    @Query("SELECT v FROM VitalMetric v WHERE v.patient.id = :patientId " +
           "AND v.recordedAt = (" +
           "  SELECT MAX(v2.recordedAt) FROM VitalMetric v2 " +
           "  WHERE v2.patient.id = :patientId AND v2.metricType = v.metricType" +
           ")")
    List<VitalMetric> findLatestPerMetricType(@Param("patientId") UUID patientId);

    long countByPatientId(UUID patientId);
}
