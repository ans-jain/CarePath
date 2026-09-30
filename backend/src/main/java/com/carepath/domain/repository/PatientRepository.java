package com.carepath.domain.repository;

import com.carepath.domain.models.PatientProfile;
import com.carepath.domain.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<PatientProfile, UUID> {

    Optional<PatientProfile> findByUserId(UUID userId);

    Optional<PatientProfile> findByUser(User user);

    boolean existsByUserId(UUID userId);
}
