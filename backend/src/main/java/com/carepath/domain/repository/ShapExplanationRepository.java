package com.carepath.domain.repository;

import com.carepath.domain.models.ShapExplanation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShapExplanationRepository extends JpaRepository<ShapExplanation, UUID> {

    Optional<ShapExplanation> findByRiskAssessmentId(UUID riskAssessmentId);
}
