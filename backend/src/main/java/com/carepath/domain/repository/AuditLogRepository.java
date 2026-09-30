package com.carepath.domain.repository;

import com.carepath.domain.models.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findByTargetPatientIdOrderByCreatedAtDesc(UUID targetPatientId, Pageable pageable);

    Page<AuditLog> findByActorUserIdOrderByCreatedAtDesc(UUID actorUserId, Pageable pageable);

    Page<AuditLog> findByActionTypeOrderByCreatedAtDesc(String actionType, Pageable pageable);
}
