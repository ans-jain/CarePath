package com.carepath.api.dto;

import com.carepath.domain.enums.AccountStatus;
import com.carepath.domain.enums.Role;

import java.util.UUID;

public class DoctorRegisterResponse {

    private UUID userId;
    private String email;
    private Role role;
    private AccountStatus status;
    private String message;

    public DoctorRegisterResponse() {
    }

    public DoctorRegisterResponse(UUID userId, String email, Role role, AccountStatus status, String message) {
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.status = status;
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

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
