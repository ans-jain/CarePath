package com.carepath.domain.repository;

import com.carepath.domain.models.ClinicianProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClinicianProfileRepository extends JpaRepository<ClinicianProfile, UUID> {

    Optional<ClinicianProfile> findByUserId(UUID userId);

    Optional<ClinicianProfile> findByLicenseNumber(String licenseNumber);

    boolean existsByLicenseNumber(String licenseNumber);
}
