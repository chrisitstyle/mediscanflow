package com.chrisitstyle.mediscanflow.medicalplatform.users;

import com.chrisitstyle.mediscanflow.medicalplatform.audit.AuditEventService;
import com.chrisitstyle.mediscanflow.medicalplatform.audit.AuditEventType;
import com.chrisitstyle.mediscanflow.medicalplatform.users.dto.CreateUserRequestDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.users.dto.UserCreatedResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementService {

    private final KeycloakAdminClient keycloakAdminClient;
    private final AuditEventService auditEventService;

    public UserManagementService(
            KeycloakAdminClient keycloakAdminClient,
            AuditEventService auditEventService
    ) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.auditEventService = auditEventService;
    }

    @Transactional
    public UserCreatedResponseDTO createUser(CreateUserRequestDTO request) {
        String firstName = request.firstName().trim();
        String lastName = request.lastName().trim();
        String email = request.email().trim().toLowerCase();

        String userId = keycloakAdminClient.createUser(
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
}