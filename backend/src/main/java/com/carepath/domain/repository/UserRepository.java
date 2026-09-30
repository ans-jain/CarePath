package com.carepath.domain.repository;

import com.carepath.domain.enums.AccountStatus;
import com.carepath.domain.enums.Role;
import com.carepath.domain.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByRoleAndStatus(Role role, AccountStatus status);
}
