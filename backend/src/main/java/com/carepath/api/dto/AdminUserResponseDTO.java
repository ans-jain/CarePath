package com.carepath.api.dto;

import com.carepath.domain.enums.Role;
import com.carepath.domain.models.User;

import java.time.Instant;
import java.util.UUID;

public class AdminUserResponseDTO {

    private UUID id;
    private String email;
    private Role role;
    private String firstName;
    private String lastName;
    private String phone;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public AdminUserResponseDTO() {
    }

    public AdminUserResponseDTO(UUID id, String email, Role role, String firstName,
                                String lastName, String phone, boolean active,
                                Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static AdminUserResponseDTO fromEntity(User user) {
        if (user == null) return null;
        return new AdminUserResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
