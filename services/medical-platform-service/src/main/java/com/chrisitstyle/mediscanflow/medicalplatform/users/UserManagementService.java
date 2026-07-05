package com.chrisitstyle.mediscanflow.medicalplatform.users;

import com.chrisitstyle.mediscanflow.medicalplatform.audit.AuditEventService;
import com.chrisitstyle.mediscanflow.medicalplatform.audit.AuditEventType;
import com.chrisitstyle.mediscanflow.medicalplatform.auth.AuthenticatedUserProvider;
import com.chrisitstyle.mediscanflow.medicalplatform.auth.UserRole;
import com.chrisitstyle.mediscanflow.medicalplatform.auth.dto.CurrentUserDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.auth.keycloak.KeycloakIdentityProvider;
import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.LastActiveAdminException;
import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.SelfDisableNotAllowedException;
import com.chrisitstyle.mediscanflow.medicalplatform.users.dto.CreateUserRequestDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.users.dto.UpdateUserStatusRequestDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.users.dto.UserCreatedResponseDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.users.dto.UserDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.users.dto.UserStatusDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class UserManagementService {

    private static final long MIN_ACTIVE_ADMIN_COUNT_AFTER_DISABLE = 1L;

    private final KeycloakIdentityProvider identityProvider;
    private final AuditEventService auditEventService;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final UserMapper userMapper;

    public UserManagementService(
            KeycloakIdentityProvider identityProvider,
            AuditEventService auditEventService,
            AuthenticatedUserProvider authenticatedUserProvider,
            UserMapper userMapper
    ) {
        this.identityProvider = identityProvider;
        this.auditEventService = auditEventService;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getUsers() {
        return userMapper.toDTOs(identityProvider.getUsers());
    }

    @Transactional(readOnly = true)
    public UserDTO getUser(String userId) {
        return userMapper.toDTO(identityProvider.getUser(userId));
    }

    @Transactional
    public UserCreatedResponseDTO createUser(CreateUserRequestDTO request) {
        String firstName = request.firstName().trim();
        String lastName = request.lastName().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        String userId = identityProvider.createUser(
                firstName,
                lastName,
                email,
                request.temporaryPassword(),
                request.role()
        );

        auditEventService.recordEvent(
                AuditEventType.USER_CREATED,
                null,
                null,
                "User " + email + " was created with role " + request.role().name() + "."
        );

        return new UserCreatedResponseDTO(
                userId,
                email,
                firstName,
                lastName,
                request.role(),
                true
        );
    }

    @Transactional
    public UserDTO updateUserStatus(String userId, UpdateUserStatusRequestDTO request) {
        UserAccount targetUser = identityProvider.getUser(userId);
        UserStatusDTO requestedStatus = request.status();

        if (targetUser.status() == requestedStatus) {
            return userMapper.toDTO(targetUser);
        }

        if (requestedStatus == UserStatusDTO.DISABLED) {
            validateDisableRequest(targetUser);
        }

        identityProvider.updateUserEnabled(targetUser.id(), requestedStatus.isEnabled());

        UserAccount updatedUser = targetUser.withEnabled(requestedStatus.isEnabled());
        recordStatusChangeEvent(updatedUser);

        return userMapper.toDTO(updatedUser);
    }

    private void validateDisableRequest(UserAccount targetUser) {
        CurrentUserDTO currentUser = authenticatedUserProvider.getCurrentUser();

        if (targetUser.id().equals(currentUser.id())) {
            throw new SelfDisableNotAllowedException();
        }

        if (targetUser.hasRole(UserRole.ADMIN) && disablingWouldRemoveLastActiveAdmin()) {
            throw new LastActiveAdminException();
        }
    }

    private boolean disablingWouldRemoveLastActiveAdmin() {
        return identityProvider.countEnabledAdmins() <= MIN_ACTIVE_ADMIN_COUNT_AFTER_DISABLE;
    }

    private void recordStatusChangeEvent(UserAccount user) {
        AuditEventType eventType = user.enabled()
                ? AuditEventType.USER_ENABLED
                : AuditEventType.USER_DISABLED;

        auditEventService.recordEventWithMetadata(
                eventType,
                null,
                null,
                "User " + user.email() + " was " + user.status().getValue().toLowerCase(Locale.ROOT) + ".",
                statusChangeMetadata(user)
        );
    }

    private String statusChangeMetadata(UserAccount user) {
        return "{\"targetUserId\":\"" + user.id() + "\","
                + "\"targetUserEmail\":\"" + user.email() + "\","
                + "\"status\":\"" + user.status().getValue() + "\"}";
    }
}