package com.carepath.api.exception;

import org.springframework.security.core.AuthenticationException;

public class DoctorPendingApprovalException extends AuthenticationException {
    public DoctorPendingApprovalException(String message) {
        super(message);
    }
}
