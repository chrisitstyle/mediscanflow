package com.chrisitstyle.mediscanflow.medicalplatform.common.error;

public class UserManagementException extends RuntimeException {

    public UserManagementException(String message) {
        super(message);
    }

    public UserManagementException(String message, Throwable cause) {
        super(message, cause);
    }

}