package com.chrisitstyle.mediscanflow.medicalplatform.users.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum UserStatusDTO {
    ENABLED("Enabled", true),
    DISABLED("Disabled", false);

    private final String value;
    private final boolean enabled;

    UserStatusDTO(String value, boolean enabled) {
        this.value = value;
        this.enabled = enabled;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public static UserStatusDTO fromEnabled(boolean enabled) {
        return enabled ? ENABLED : DISABLED;
    }

    @JsonCreator
    public static UserStatusDTO fromValue(String value) {
        return Arrays.stream(values())
                .filter(status -> status.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User status must be Enabled or Disabled"));
    }
}
