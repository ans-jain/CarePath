package com.carepath.security;

import com.carepath.domain.enums.AccessStatus;
import com.carepath.domain.enums.Role;
import com.carepath.domain.models.ClinicianProfile;
import com.carepath.domain.models.PatientClinicianAccess;
import com.carepath.domain.models.PatientProfile;
import com.carepath.domain.models.User;
import com.carepath.domain.repository.ClinicianProfileRepository;
import com.carepath.domain.repository.PatientClinicianAccessRepository;
import com.carepath.domain.repository.PatientRepository;
import com.carepath.domain.repository.UserRepository;
import com.carepath.service.AccessControlService;
import com.carepath.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessControlServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private ClinicianProfileRepository clinicianProfileRepository;

    @Mock
    private PatientClinicianAccessRepository patientClinicianAccessRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    private AccessControlService accessControlService;

    private User patientUser1;
    private User patientUser2;
    private User clinicianUser;
    private User adminUser;

    private PatientProfile patientProfile1;
    private PatientProfile patientProfile2;
    private ClinicianProfile clinicianProfile;

    private UserPrincipal patientPrincipal1;
    private UserPrincipal patientPrincipal2;
    private UserPrincipal clinicianPrincipal;
    private UserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        accessControlService = new AccessControlService(
                patientRepository,
                clinicianProfileRepository,
                patientClinicianAccessRepository,
                userRepository,
                auditLogService
        );

        UUID p1Id = UUID.randomUUID();
        patientUser1 = new User("patient1@carepath.io", "hash", Role.ROLE_PATIENT, "Sarah", "Jenkins");
        patientUser1.setId(p1Id);
        patientPrincipal1 = UserPrincipal.create(patientUser1);

        UUID p2Id = UUID.randomUUID();
        patientUser2 = new User("patient2@carepath.io", "hash", Role.ROLE_PATIENT, "Bob", "Smith");
        patientUser2.setId(p2Id);
        patientPrincipal2 = UserPrincipal.create(patientUser2);

        UUID cId = UUID.randomUUID();
        clinicianUser = new User("dr.marcus@carepath.io", "hash", Role.ROLE_CLINICIAN, "Marcus", "Vance");
        clinicianUser.setId(cId);
        clinicianPrincipal = UserPrincipal.create(clinicianUser);

        UUID aId = UUID.randomUUID();
        adminUser = new User("admin@carepath.io", "hash", Role.ROLE_ADMIN, "Alex", "Admin");
        adminUser.setId(aId);
        adminPrincipal = UserPrincipal.create(adminUser);

        patientProfile1 = new PatientProfile();
        patientProfile1.setId(UUID.randomUUID());
        patientProfile1.setUser(patientUser1);

        patientProfile2 = new PatientProfile();
        patientProfile2.setId(UUID.randomUUID());
        patientProfile2.setUser(patientUser2);

        clinicianProfile = new ClinicianProfile(clinicianUser, "MED-12345", "Cardiology", "Heart Center");
        clinicianProfile.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Patient can access their own patient profile")
    void testPatientCanAccessOwnRecord() {
        when(patientRepository.findById(patientProfile1.getId())).thenReturn(Optional.of(patientProfile1));

        boolean canAccess = accessControlService.canAccessPatient(patientPrincipal1, patientProfile1.getId());
        assertThat(canAccess).isTrue();

        PatientProfile result = accessControlService.checkAndGetPatient(patientProfile1.getId(), patientPrincipal1, "READ", "127.0.0.1");
        assertThat(result).isEqualTo(patientProfile1);
    }

    @Test
    @DisplayName("Patient CANNOT access another patient's profile and audit log is recorded")
    void testPatientCannotAccessOtherPatientRecord() {
        when(patientRepository.findById(patientProfile2.getId())).thenReturn(Optional.of(patientProfile2));
        when(userRepository.findById(patientPrincipal1.getId())).thenReturn(Optional.of(patientUser1));

        boolean canAccess = accessControlService.canAccessPatient(patientPrincipal1, patientProfile2.getId());
        assertThat(canAccess).isFalse();

        assertThatThrownBy(() -> accessControlService.checkAndGetPatient(patientProfile2.getId(), patientPrincipal1, "READ", "127.0.0.1"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not authorized");

        verify(auditLogService, times(1)).recordEvent(
                eq(patientUser1),
                eq(patientPrincipal1.getUsername()),
                eq(patientProfile2.getId()),
                eq("FORBIDDEN_ACCESS_ATTEMPT"),
                eq("PatientProfile"),
                eq(patientProfile2.getId()),
                eq("FORBIDDEN"),
                eq("127.0.0.1"),
                isNull(),
                anyMap()
        );
    }

    @Test
    @DisplayName("Clinician WITHOUT active assignment cannot access patient")
    void testClinicianWithoutAssignmentDenied() {
        when(patientRepository.findById(patientProfile1.getId())).thenReturn(Optional.of(patientProfile1));
        when(clinicianProfileRepository.findByUserId(clinicianPrincipal.getId())).thenReturn(Optional.of(clinicianProfile));
        when(patientClinicianAccessRepository.findByClinicianIdAndPatientIdAndAccessStatus(
                clinicianProfile.getId(), patientProfile1.getId(), AccessStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(userRepository.findById(clinicianPrincipal.getId())).thenReturn(Optional.of(clinicianUser));

        boolean canAccess = accessControlService.canAccessPatient(clinicianPrincipal, patientProfile1.getId());
        assertThat(canAccess).isFalse();

        assertThatThrownBy(() -> accessControlService.checkAndGetPatient(patientProfile1.getId(), clinicianPrincipal, "READ", "127.0.0.1"))
                .isInstanceOf(AccessDeniedException.class);

        verify(auditLogService, times(1)).recordEvent(
                eq(clinicianUser), anyString(), eq(patientProfile1.getId()),
                eq("FORBIDDEN_ACCESS_ATTEMPT"), eq("PatientProfile"), eq(patientProfile1.getId()),
                eq("FORBIDDEN"), anyString(), isNull(), anyMap()
        );
    }

    @Test
    @DisplayName("Clinician WITH active assignment CAN access patient")
    void testClinicianWithActiveAssignmentAllowed() {
        when(patientRepository.findById(patientProfile1.getId())).thenReturn(Optional.of(patientProfile1));
        when(clinicianProfileRepository.findByUserId(clinicianPrincipal.getId())).thenReturn(Optional.of(clinicianProfile));

        PatientClinicianAccess access = new PatientClinicianAccess(patientProfile1, clinicianProfile, AccessStatus.ACTIVE);
        when(patientClinicianAccessRepository.findByClinicianIdAndPatientIdAndAccessStatus(
                clinicianProfile.getId(), patientProfile1.getId(), AccessStatus.ACTIVE))
                .thenReturn(Optional.of(access));

        boolean canAccess = accessControlService.canAccessPatient(clinicianPrincipal, patientProfile1.getId());
        assertThat(canAccess).isTrue();

        PatientProfile result = accessControlService.checkAndGetPatient(patientProfile1.getId(), clinicianPrincipal, "CLINICAL_REVIEW", "127.0.0.1");
        assertThat(result).isEqualTo(patientProfile1);
    }

    @Test
    @DisplayName("Admin can access any patient profile for administrative operations")
    void testAdminCanAccessAnyPatient() {
        when(patientRepository.findById(patientProfile1.getId())).thenReturn(Optional.of(patientProfile1));

        boolean canAccess = accessControlService.canAccessPatient(adminPrincipal, patientProfile1.getId());
        assertThat(canAccess).isTrue();

        PatientProfile result = accessControlService.checkAndGetPatient(patientProfile1.getId(), adminPrincipal, "ADMIN_INSPECT", "127.0.0.1");
        assertThat(result).isEqualTo(patientProfile1);
    }

    @Test
    @DisplayName("User ownership check rejects unauthorized actor")
    void testCheckUserOwnershipRejection() {
        UUID targetUserId = UUID.randomUUID();
        when(userRepository.findById(patientPrincipal1.getId())).thenReturn(Optional.of(patientUser1));

        assertThatThrownBy(() -> accessControlService.checkUserOwnership(targetUserId, patientPrincipal1, "Notification", "127.0.0.1"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("do not own");

        verify(auditLogService, times(1)).recordEvent(
                eq(patientUser1), anyString(), isNull(),
                eq("FORBIDDEN_ACCESS_ATTEMPT"), eq("Notification"), eq(targetUserId),
                eq("FORBIDDEN"), anyString(), isNull(), anyMap()
        );
    }
}
