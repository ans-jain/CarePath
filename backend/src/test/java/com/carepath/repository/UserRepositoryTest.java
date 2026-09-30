package com.carepath.repository;

import com.carepath.domain.enums.Role;
import com.carepath.domain.models.User;
import com.carepath.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User(
                "user.test." + UUID.randomUUID() + "@carepath.io",
                "$2a$12$e8Y...dummyHashedPasswordValue",
                Role.ROLE_PATIENT,
                "Sarah",
                "Jenkins"
        );
        sampleUser.setPhone("+15551234567");
    }

    @Test
    @DisplayName("Should successfully persist and retrieve user by ID and email")
    void testSaveAndFindUser() {
        User saved = userRepository.save(sampleUser);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        Optional<User> foundByEmail = userRepository.findByEmail(sampleUser.getEmail());
        assertThat(foundByEmail).isPresent();
        assertThat(foundByEmail.get().getFirstName()).isEqualTo("Sarah");
        assertThat(foundByEmail.get().getLastName()).isEqualTo("Jenkins");
        assertThat(foundByEmail.get().getRole()).isEqualTo(Role.ROLE_PATIENT);
        assertThat(foundByEmail.get().isActive()).isTrue();
    }

    @Test
    @DisplayName("Should enforce email uniqueness constraint")
    void testEmailUniquenessConstraint() {
        userRepository.saveAndFlush(sampleUser);

        User duplicateUser = new User(
                sampleUser.getEmail(),
                "$2a$12$anotherHashedValue",
                Role.ROLE_PATIENT,
                "Duplicate",
                "User"
        );

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicateUser))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should check email existence accurately")
    void testExistsByEmail() {
        userRepository.save(sampleUser);

        assertThat(userRepository.existsByEmail(sampleUser.getEmail())).isTrue();
        assertThat(userRepository.existsByEmail("nonexistent@carepath.io")).isFalse();
    }
}
