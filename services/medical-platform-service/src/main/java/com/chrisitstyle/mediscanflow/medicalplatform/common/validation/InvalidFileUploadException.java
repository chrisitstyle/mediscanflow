package com.chrisitstyle.mediscanflow.medicalplatform.common.validation;

public class InvalidFileUploadException extends RuntimeException {

    public InvalidFileUploadException(String message) {
        super(message);
    }
}
