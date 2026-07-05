package com.chrisitstyle.mediscanflow.medicalplatform.users.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserStatusDTOTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void fromEnabledReturnsEnabledStatus() {
        UserStatusDTO status = UserStatusDTO.fromEnabled(true);

        assertEquals(UserStatusDTO.ENABLED, status);
        assertEquals("Enabled", status.getValue());
        assertTrue(status.isEnabled());
    }

    @Test
    void fromEnabledReturnsDisabledStatus() {
        UserStatusDTO status = UserStatusDTO.fromEnabled(false);

        assertEquals(UserStatusDTO.DISABLED, status);
        assertEquals("Disabled", status.getValue());
        assertFalse(status.isEnabled());
    }

    @Test
    void fromValueAcceptsEnabledStatus() {
        UserStatusDTO status = UserStatusDTO.fromValue("Enabled");

        assertEquals(UserStatusDTO.ENABLED, status);
    }

    @Test
    void fromValueAcceptsDisabledStatus() {
        UserStatusDTO status = UserStatusDTO.fromValue("Disabled");

        assertEquals(UserStatusDTO.DISABLED, status);
    }

    @Test
    void fromValueIsCaseInsensitive() {
        assertEquals(UserStatusDTO.ENABLED, UserStatusDTO.fromValue("enabled"));
        assertEquals(UserStatusDTO.DISABLED, UserStatusDTO.fromValue("disabled"));
    }

    @Test
    void fromValueThrowsWhenStatusIsUnsupported() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> UserStatusDTO.fromValue("Blocked")
        );

        assertEquals("User status must be Enabled or Disabled", exception.getMessage());
    }

    @Test
    void serializesStatusAsDisplayValue() throws Exception {
        assertEquals("\"Enabled\"", objectMapper.writeValueAsString(UserStatusDTO.ENABLED));
        assertEquals("\"Disabled\"", objectMapper.writeValueAsString(UserStatusDTO.DISABLED));
    }

    @Test
    void deserializesStatusFromDisplayValue() throws Exception {
        assertEquals(
                UserStatusDTO.ENABLED,
                objectMapper.readValue("\"Enabled\"", UserStatusDTO.class)
        );

        assertEquals(
                UserStatusDTO.DISABLED,
                objectMapper.readValue("\"Disabled\"", UserStatusDTO.class)
        );
    }
}