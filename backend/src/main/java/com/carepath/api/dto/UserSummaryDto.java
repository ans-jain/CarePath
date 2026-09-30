package com.carepath.api.dto;

import com.carepath.domain.enums.AccountStatus;
import com.carepath.domain.enums.Role;

import java.util.UUID;

public class UserSummaryDto {

    private UUID id;
    private String email;
    private Role role;
    private AccountStatus status;
    private String firstName;
    private String lastName;

    public UserSummaryDto() {
    }

    public UserSummaryDto(UUID id, String email, Role role, String firstName, String lastName) {
        this(id, email, role, AccountStatus.ACTIVE, firstName, lastName);
    }

    public UserSummaryDto(UUID id, String email, Role role, AccountStatus status, String firstName, String lastName) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.status = status;
        this.firstName = firstName;
        this.lastName = lastName;
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

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
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
}
