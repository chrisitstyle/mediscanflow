package com.chrisitstyle.mediscanflow.medicalplatform.common.exception;

public class SelfDisableNotAllowedException extends RuntimeException {
    public SelfDisableNotAllowedException() {
        super("Admin cannot disable their own account.");
    }
}
