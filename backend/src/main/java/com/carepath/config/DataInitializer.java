package com.carepath.config;

import com.carepath.domain.enums.AccountStatus;
import com.carepath.domain.enums.Role;
import com.carepath.domain.models.ClinicianProfile;
import com.carepath.domain.models.User;
import com.carepath.domain.repository.ClinicianProfileRepository;
import com.carepath.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final ClinicianProfileRepository clinicianProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           ClinicianProfileRepository clinicianProfileRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.clinicianProfileRepository = clinicianProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        initializeAdminUser();
        initializeDoctorUser();
        initializePatientUser();
    }

    private void initializeAdminUser() {
        String adminEmail = "admin@carepath.io";
        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = new User(
                    adminEmail,
                    passwordEncoder.encode("AdminPassword123!"),
                    Role.ROLE_ADMIN,
                    "System",
                    "Admin"
            );
            admin.setStatus(AccountStatus.ACTIVE);
            admin.setActive(true);
            admin.setPhone("+15551000001");
            userRepository.save(admin);
            log.info("[DATA_INITIALIZER] Seeded system admin user: {}", adminEmail);
        }
    }

    private void initializeDoctorUser() {
        String doctorEmail = "dr.marcus@carepath.io";
        if (!userRepository.existsByEmail(doctorEmail)) {
            User doctor = new User(
                    doctorEmail,
                    passwordEncoder.encode("SecurePass123!"),
                    Role.ROLE_CLINICIAN,
                    "Marcus",
                    "Vance"
            );
            doctor.setStatus(AccountStatus.ACTIVE);
            doctor.setActive(true);
            doctor.setPhone("+15552000002");
            User savedDoctor = userRepository.save(doctor);

            ClinicianProfile profile = new ClinicianProfile(
                    savedDoctor,
                    "MD-8921-CARDIO",
                    "Cardiology",
                    "Metropolitan Heart & Vascular Institute"
            );
            clinicianProfileRepository.save(profile);
            log.info("[DATA_INITIALIZER] Seeded approved doctor user: {}", doctorEmail);
        }
    }

    private void initializePatientUser() {
        String patientEmail = "patient.sarah@carepath.io";
        if (!userRepository.existsByEmail(patientEmail)) {
            User patient = new User(
                    patientEmail,
                    passwordEncoder.encode("SecurePass123!"),
                    Role.ROLE_PATIENT,
                    "Sarah",
                    "Jenkins"
            );
            patient.setStatus(AccountStatus.ACTIVE);
            patient.setActive(true);
            patient.setPhone("+15553000003");
            userRepository.save(patient);
            log.info("[DATA_INITIALIZER] Seeded active patient user: {}", patientEmail);
        }
    }
}
