package com.carepath.domain.repository;

import com.carepath.domain.enums.ReportShareStatus;
import com.carepath.domain.models.ReportShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportShareRepository extends JpaRepository<ReportShare, UUID> {

    Optional<ReportShare> findByReportIdAndDoctorId(UUID reportId, UUID doctorId);

    List<ReportShare> findByReportId(UUID reportId);

    List<ReportShare> findByReportIdAndStatusNot(UUID reportId, ReportShareStatus status);

    List<ReportShare> findByDoctorIdAndStatusNotOrderBySharedAtDesc(UUID doctorId, ReportShareStatus status);

    @Query("SELECT rs FROM ReportShare rs WHERE rs.doctor.id = :doctorId AND rs.report.id = :reportId AND rs.status != :excludedStatus")
    Optional<ReportShare> findActiveShareForDoctorAndReport(
            @Param("doctorId") UUID doctorId,
            @Param("reportId") UUID reportId,
            @Param("excludedStatus") ReportShareStatus excludedStatus);

    boolean existsByReportIdAndDoctorIdAndStatusNot(UUID reportId, UUID doctorId, ReportShareStatus status);
}
