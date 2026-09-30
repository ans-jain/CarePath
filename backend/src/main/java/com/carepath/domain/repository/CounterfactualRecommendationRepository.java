package com.carepath.domain.repository;

import com.carepath.domain.models.CounterfactualRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CounterfactualRecommendationRepository extends JpaRepository<CounterfactualRecommendation, UUID> {

    List<CounterfactualRecommendation> findByRiskAssessmentId(UUID riskAssessmentId);
}
