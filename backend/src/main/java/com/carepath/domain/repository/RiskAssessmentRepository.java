package com.carepath.domain.repository;

import com.carepath.domain.models.RiskAssessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, UUID> {

    Optional<RiskAssessment> findFirstByPatientIdOrderByAssessmentTimestampDesc(UUID patientId);

    Page<RiskAssessment> findByPatientIdOrderByAssessmentTimestampDesc(UUID patientId, Pageable pageable);
}
