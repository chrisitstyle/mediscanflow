package com.chrisitstyle.mediscanflow.medicalplatform.auth;

import com.chrisitstyle.mediscanflow.medicalplatform.users.UserAccount;

import java.util.List;

/**
 * Provides application-level identity management operations.
 */
public interface IdentityProvider {

    List<UserAccount> getUsers();

    UserAccount getUser(String userId);

    long countEnabledAdmins();

    String createUser(
            String firstName,
            String lastName,
            String email,
            String temporaryPassword,
            UserRole role
    );

    void updateUserEnabled(String userId, boolean enabled);
}
