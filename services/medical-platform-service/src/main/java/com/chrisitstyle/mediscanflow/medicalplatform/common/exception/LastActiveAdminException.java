package com.chrisitstyle.mediscanflow.medicalplatform.common.exception;

public class LastActiveAdminException extends RuntimeException {
    public LastActiveAdminException() {
        super("Cannot disable the last active admin account.");
    }
}
