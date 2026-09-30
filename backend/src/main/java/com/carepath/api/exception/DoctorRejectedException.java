package com.carepath.api.exception;

import org.springframework.security.core.AuthenticationException;

public class DoctorRejectedException extends AuthenticationException {
    public DoctorRejectedException(String message) {
        super(message);
    }
}
