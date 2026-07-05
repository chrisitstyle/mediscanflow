package com.chrisitstyle.mediscanflow.medicalplatform.auth.keycloak;

import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.ResourceNotFoundException;
import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.UserManagementException;

import java.net.URI;

/**
 * Shared helper methods for low-level Keycloak Admin API HTTP interactions.
 *
 * <p>This class keeps repeated concerns such as bearer token formatting,
 * created-user id extraction and not-found exception mapping in one place.</p>
 */
final class KeycloakHttpSupport {

    private KeycloakHttpSupport() {
    }

    static String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    static String extractUserId(URI location) {
        String path = location.getPath();
        int lastSlashIndex = path.lastIndexOf('/');

        if (lastSlashIndex < 0 || lastSlashIndex == path.length() - 1) {
            throw new UserManagementException("Could not extract created user id from Keycloak location.");
        }

        return path.substring(lastSlashIndex + 1);
    }

    static ResourceNotFoundException userNotFound(String userId, Throwable cause) {
        return new ResourceNotFoundException("User not found with id: " + userId, cause);
    }
}
