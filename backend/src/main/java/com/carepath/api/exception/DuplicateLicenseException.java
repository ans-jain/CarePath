package com.carepath.api.exception;

public class DuplicateLicenseException extends RuntimeException {
    public DuplicateLicenseException(String message) {
        super(message);
    }
}
