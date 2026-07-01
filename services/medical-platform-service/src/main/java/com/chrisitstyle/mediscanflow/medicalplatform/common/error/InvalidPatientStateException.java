package com.chrisitstyle.mediscanflow.medicalplatform.common.error;

public class InvalidPatientStateException extends RuntimeException {
    public InvalidPatientStateException(String message) {
        super(message);
    }
}
