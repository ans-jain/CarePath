package com.carepath.domain.repository;

import com.carepath.domain.enums.AccessStatus;
import com.carepath.domain.models.PatientClinicianAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientClinicianAccessRepository extends JpaRepository<PatientClinicianAccess, UUID> {

    List<PatientClinicianAccess> findByClinicianIdAndAccessStatus(UUID clinicianId, AccessStatus status);

    List<PatientClinicianAccess> findByPatientId(UUID patientId);

    Optional<PatientClinicianAccess> findByClinicianIdAndPatientIdAndAccessStatus(
            UUID clinicianId, UUID patientId, AccessStatus status);

    Optional<PatientClinicianAccess> findByGrantCode(String grantCode);
}
