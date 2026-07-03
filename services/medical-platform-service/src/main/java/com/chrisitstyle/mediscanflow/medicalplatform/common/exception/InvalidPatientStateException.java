package com.chrisitstyle.mediscanflow.medicalplatform.common.exception;

public class InvalidPatientStateException extends RuntimeException {
    public InvalidPatientStateException(String message) {
        super(message);
    }
}
