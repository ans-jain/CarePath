package com.carepath.service;

import com.carepath.api.dto.DoctorDirectoryItemDTO;
import com.carepath.api.dto.ReportShareRequestDTO;
import com.carepath.api.dto.ReportShareResponseDTO;
import com.carepath.api.dto.SharedReportItemDTO;
import com.carepath.domain.enums.AccountStatus;
import com.carepath.domain.enums.ConfidenceLevel;
import com.carepath.domain.enums.ReportShareStatus;
import com.carepath.domain.enums.RiskCategory;
import com.carepath.domain.enums.Role;
import com.carepath.domain.models.ClinicianProfile;
import com.carepath.domain.models.PatientProfile;
import com.carepath.domain.models.ReportShare;
import com.carepath.domain.models.RiskAssessment;
import com.carepath.domain.models.User;
import com.carepath.domain.repository.ClinicianProfileRepository;
import com.carepath.domain.repository.ReportShareRepository;
import com.carepath.domain.repository.RiskAssessmentRepository;
import com.carepath.domain.repository.UserRepository;
import com.carepath.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportShareServiceTest {

    @Mock
    private ReportShareRepository reportShareRepository;

    @Mock
    private RiskAssessmentRepository riskAssessmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClinicianProfileRepository clinicianProfileRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ReportShareService reportShareService;

    private User patientUser;
    private User doctorUser;
    private PatientProfile patientProfile;
    private ClinicianProfile clinicianProfile;
    private RiskAssessment assessment;
    private UserPrincipal patientPrincipal;
    private UserPrincipal doctorPrincipal;

    @BeforeEach
    void setUp() {
        UUID patientUserId = UUID.randomUUID();
        patientUser = new User("sarah@carepath.io", "hash", Role.ROLE_PATIENT, "Sarah", "Jenkins");
        patientUser.setId(patientUserId);
        patientUser.setStatus(AccountStatus.ACTIVE);

        UUID doctorUserId = UUID.randomUUID();
        doctorUser = new User("dr.chen@carepath.io", "hash", Role.ROLE_CLINICIAN, "Robert", "Chen");
        doctorUser.setId(doctorUserId);
        doctorUser.setStatus(AccountStatus.ACTIVE);

        patientProfile = new PatientProfile();
        patientProfile.setId(UUID.randomUUID());
        patientProfile.setUser(patientUser);

        clinicianProfile = new ClinicianProfile(doctorUser, "MD-12345", "Cardiology", "Metro Heart Center");
        clinicianProfile.setId(UUID.randomUUID());

        assessment = new RiskAssessment(
                patientProfile,
                Instant.now(),
                "calibrated_v1.0.0",
                new BigDecimal("0.720"),
                RiskCategory.ELEVATED,
                ConfidenceLevel.LONGITUDINAL_ROBUST,
                "{\"features\":{\"systolic_bp_current\":142}}"
        );
        assessment.setId(UUID.randomUUID());

        patientPrincipal = UserPrincipal.create(patientUser);
        doctorPrincipal = UserPrincipal.create(doctorUser);
    }

    @Test
    @DisplayName("getVerifiedDoctors returns active verified doctors with clinician details")
    void getVerifiedDoctors_Success() {
        when(userRepository.findByRoleAndStatus(Role.ROLE_CLINICIAN, AccountStatus.ACTIVE))
                .thenReturn(List.of(doctorUser));
        when(clinicianProfileRepository.findByUserId(doctorUser.getId()))
                .thenReturn(Optional.of(clinicianProfile));

        List<DoctorDirectoryItemDTO> result = reportShareService.getVerifiedDoctors();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Dr. Robert Chen");
        assertThat(result.get(0).getSpecialization()).isEqualTo("Cardiology");
        assertThat(result.get(0).getHospitalOrganization()).isEqualTo("Metro Heart Center");
    }

    @Test
    @DisplayName("shareReport successfully creates report share when caller is the patient owner")
    void shareReport_Success() {
        ReportShareRequestDTO req = new ReportShareRequestDTO(assessment.getId(), doctorUser.getId());

        when(riskAssessmentRepository.findById(assessment.getId())).thenReturn(Optional.of(assessment));
        when(userRepository.findById(doctorUser.getId())).thenReturn(Optional.of(doctorUser));
        when(reportShareRepository.findByReportIdAndDoctorId(assessment.getId(), doctorUser.getId()))
                .thenReturn(Optional.empty());

        ReportShare savedShare = new ReportShare(assessment, patientProfile, doctorUser);
        savedShare.setId(UUID.randomUUID());
        savedShare.setStatus(ReportShareStatus.NEW);

        when(reportShareRepository.save(any(ReportShare.class))).thenReturn(savedShare);
        when(clinicianProfileRepository.findByUserId(doctorUser.getId()))
                .thenReturn(Optional.of(clinicianProfile));

        ReportShareResponseDTO dto = reportShareService.shareReport(req, patientPrincipal, "127.0.0.1");

        assertThat(dto).isNotNull();
        assertThat(dto.getReportId()).isEqualTo(assessment.getId());
        assertThat(dto.getDoctorName()).isEqualTo("Dr. Robert Chen");
        assertThat(dto.getStatus()).isEqualTo(ReportShareStatus.NEW);
        verify(reportShareRepository).save(any(ReportShare.class));
    }

    @Test
    @DisplayName("shareReport throws AccessDeniedException when caller is not the report owner")
    void shareReport_ThrowsAccessDenied_WhenNotOwner() {
        UUID strangerId = UUID.randomUUID();
        User strangerUser = new User("stranger@carepath.io", "hash", Role.ROLE_PATIENT, "Other", "Patient");
        strangerUser.setId(strangerId);
        UserPrincipal strangerPrincipal = UserPrincipal.create(strangerUser);

        ReportShareRequestDTO req = new ReportShareRequestDTO(assessment.getId(), doctorUser.getId());
        when(riskAssessmentRepository.findById(assessment.getId())).thenReturn(Optional.of(assessment));

        assertThatThrownBy(() -> reportShareService.shareReport(req, strangerPrincipal, "127.0.0.1"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You are only permitted to share your own assessment reports.");

        verify(reportShareRepository, never()).save(any());
    }

    @Test
    @DisplayName("shareReport throws IllegalArgumentException when selected doctor is not active")
    void shareReport_ThrowsIllegalArgument_WhenDoctorInactive() {
        doctorUser.setStatus(AccountStatus.PENDING);
        ReportShareRequestDTO req = new ReportShareRequestDTO(assessment.getId(), doctorUser.getId());

        when(riskAssessmentRepository.findById(assessment.getId())).thenReturn(Optional.of(assessment));
        when(userRepository.findById(doctorUser.getId())).thenReturn(Optional.of(doctorUser));

        assertThatThrownBy(() -> reportShareService.shareReport(req, patientPrincipal, "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Selected healthcare provider is not an active, verified doctor.");
    }

    @Test
    @DisplayName("revokeShare updates status to REVOKED when called by patient owner")
    void revokeShare_Success() {
        UUID shareId = UUID.randomUUID();
        ReportShare share = new ReportShare(assessment, patientProfile, doctorUser);
        share.setId(shareId);
        share.setStatus(ReportShareStatus.NEW);

        when(reportShareRepository.findById(shareId)).thenReturn(Optional.of(share));
        when(reportShareRepository.save(any(ReportShare.class))).thenAnswer(inv -> inv.getArgument(0));

        ReportShareResponseDTO dto = reportShareService.revokeShare(shareId, patientPrincipal, "127.0.0.1");

        assertThat(dto.getStatus()).isEqualTo(ReportShareStatus.REVOKED);
        assertThat(share.getRevokedAt()).isNotNull();
    }

    @Test
    @DisplayName("getSharedReportsForDoctor returns only shared reports for the requesting doctor")
    void getSharedReportsForDoctor_Success() {
        ReportShare share = new ReportShare(assessment, patientProfile, doctorUser);
        share.setId(UUID.randomUUID());
        share.setStatus(ReportShareStatus.NEW);

        when(reportShareRepository.findByDoctorIdAndStatusNotOrderBySharedAtDesc(
                doctorPrincipal.getId(), ReportShareStatus.REVOKED))
                .thenReturn(List.of(share));

        List<SharedReportItemDTO> list = reportShareService.getSharedReportsForDoctor(doctorPrincipal);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getPatientName()).isEqualTo("Sarah Jenkins");
        assertThat(list.get(0).getStatus()).isEqualTo("NEW");
        assertThat(list.get(0).getRiskCategory()).isEqualTo(RiskCategory.ELEVATED);
    }

    @Test
    @DisplayName("getSharedReportDetailForDoctor transitions status from NEW to VIEWED on first access")
    void getSharedReportDetailForDoctor_TransitionsToViewed() {
        ReportShare share = new ReportShare(assessment, patientProfile, doctorUser);
        share.setId(UUID.randomUUID());
        share.setStatus(ReportShareStatus.NEW);

        when(reportShareRepository.findActiveShareForDoctorAndReport(
                doctorPrincipal.getId(), assessment.getId(), ReportShareStatus.REVOKED))
                .thenReturn(Optional.of(share));
        when(reportShareRepository.save(any(ReportShare.class))).thenAnswer(inv -> inv.getArgument(0));

        SharedReportItemDTO detail = reportShareService.getSharedReportDetailForDoctor(
                assessment.getId(), doctorPrincipal, "127.0.0.1");

        assertThat(detail).isNotNull();
        assertThat(share.getStatus()).isEqualTo(ReportShareStatus.VIEWED);
        assertThat(share.getViewedAt()).isNotNull();
    }

    @Test
    @DisplayName("getSharedReportDetailForDoctor throws AccessDeniedException when access was revoked or never granted")
    void getSharedReportDetailForDoctor_ThrowsAccessDenied_WhenNotShared() {
        when(reportShareRepository.findActiveShareForDoctorAndReport(
                doctorPrincipal.getId(), assessment.getId(), ReportShareStatus.REVOKED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportShareService.getSharedReportDetailForDoctor(
                assessment.getId(), doctorPrincipal, "127.0.0.1"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You do not have permission to view this report.");
    }
}
