package com.carepath.api.dto;

import com.carepath.domain.enums.Role;

import java.util.UUID;

public class RegisterResponse {

    private UUID userId;
    private String email;
    private Role role;
    private String message;

    public RegisterResponse() {
    }

    public RegisterResponse(UUID userId, String email, Role role, String message) {
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.message = message;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
