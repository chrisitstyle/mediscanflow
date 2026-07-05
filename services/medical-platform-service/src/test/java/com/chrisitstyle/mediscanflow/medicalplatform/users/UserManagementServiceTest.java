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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    private static final String CURRENT_ADMIN_ID = "admin-1";
    private static final String TARGET_USER_ID = "user-1";
    private static final String TARGET_ADMIN_ID = "admin-2";

    @Mock
    private KeycloakIdentityProvider identityProvider;

    @Mock
    private AuditEventService auditEventService;

    @Mock
    private AuthenticatedUserProvider authenticatedUserProvider;

    private UserManagementService userManagementService;

    @BeforeEach
    void setUp() {
        userManagementService = new UserManagementService(
                identityProvider,
                auditEventService,
                authenticatedUserProvider,
                new UserMapper()
        );
    }

    @Test
    void getUsersReturnsMappedUsers() {
        when(identityProvider.getUsers())
                .thenReturn(List.of(
                        user(TARGET_ADMIN_ID, "admin@test.com", UserRole.ADMIN, true),
                        user(TARGET_USER_ID, "doctor@test.com", UserRole.DOCTOR, false)
                ));

        List<UserDTO> users = userManagementService.getUsers();

        assertAll(
                () -> assertEquals(2, users.size()),
                () -> assertEquals(TARGET_ADMIN_ID, users.getFirst().id()),
                () -> assertEquals("admin@test.com", users.getFirst().email()),
                () -> assertEquals(Set.of(UserRole.ADMIN), users.getFirst().roles()),
                () -> assertEquals(UserStatusDTO.ENABLED, users.getFirst().status()),
                () -> assertEquals(TARGET_USER_ID, users.get(1).id()),
                () -> assertEquals("doctor@test.com", users.get(1).email()),
                () -> assertEquals(Set.of(UserRole.DOCTOR), users.get(1).roles()),
                () -> assertEquals(UserStatusDTO.DISABLED, users.get(1).status())
        );

        verify(identityProvider).getUsers();
    }

    @Test
    void getUserReturnsMappedUser() {
        when(identityProvider.getUser(TARGET_USER_ID))
                .thenReturn(user(TARGET_USER_ID, "doctor@test.com", UserRole.DOCTOR, true));

        UserDTO user = userManagementService.getUser(TARGET_USER_ID);

        assertAll(
                () -> assertEquals(TARGET_USER_ID, user.id()),
                () -> assertEquals("doctor@test.com", user.email()),
                () -> assertEquals("John", user.firstName()),
                () -> assertEquals("Doe", user.lastName()),
                () -> assertEquals(Set.of(UserRole.DOCTOR), user.roles()),
                () -> assertEquals(UserStatusDTO.ENABLED, user.status())
        );

        verify(identityProvider).getUser(TARGET_USER_ID);
    }

    @Test
    void updateUserStatusDisablesUserAndRecordsAuditEvent() {
        UserAccount targetUser = user(TARGET_USER_ID, "doctor@test.com", UserRole.DOCTOR, true);

        when(identityProvider.getUser(TARGET_USER_ID))
                .thenReturn(targetUser);
        when(authenticatedUserProvider.getCurrentUser())
                .thenReturn(currentAdmin());

        UserDTO response = userManagementService.updateUserStatus(
                TARGET_USER_ID,
                new UpdateUserStatusRequestDTO(UserStatusDTO.DISABLED)
        );

        assertAll(
                () -> assertEquals(TARGET_USER_ID, response.id()),
                () -> assertEquals("doctor@test.com", response.email()),
                () -> assertEquals(UserStatusDTO.DISABLED, response.status())
        );

        verify(identityProvider).updateUserEnabled(TARGET_USER_ID, false);
        verifyStatusAuditEvent(
                AuditEventType.USER_DISABLED,
                targetUser.withEnabled(false),
                "Disabled"
        );
    }

    @Test
    void updateUserStatusEnablesUserAndRecordsAuditEvent() {
        UserAccount targetUser = user(TARGET_USER_ID, "doctor@test.com", UserRole.DOCTOR, false);

        when(identityProvider.getUser(TARGET_USER_ID))
                .thenReturn(targetUser);

        UserDTO response = userManagementService.updateUserStatus(
                TARGET_USER_ID,
                new UpdateUserStatusRequestDTO(UserStatusDTO.ENABLED)
        );

        assertAll(
                () -> assertEquals(TARGET_USER_ID, response.id()),
                () -> assertEquals("doctor@test.com", response.email()),
                () -> assertEquals(UserStatusDTO.ENABLED, response.status())
        );

        verify(identityProvider).updateUserEnabled(TARGET_USER_ID, true);
        verifyNoInteractions(authenticatedUserProvider);
        verifyStatusAuditEvent(
                AuditEventType.USER_ENABLED,
                targetUser.withEnabled(true),
                "Enabled"
        );
    }

    @Test
    void updateUserStatusDoesNotUpdateOrRecordAuditEventWhenStatusDoesNotChange() {
        UserAccount targetUser = user(TARGET_USER_ID, "doctor@test.com", UserRole.DOCTOR, false);

        when(identityProvider.getUser(TARGET_USER_ID))
                .thenReturn(targetUser);

        UserDTO response = userManagementService.updateUserStatus(
                TARGET_USER_ID,
                new UpdateUserStatusRequestDTO(UserStatusDTO.DISABLED)
        );

        assertAll(
                () -> assertEquals(TARGET_USER_ID, response.id()),
                () -> assertEquals("doctor@test.com", response.email()),
                () -> assertEquals(UserStatusDTO.DISABLED, response.status())
        );

        verify(identityProvider, never()).updateUserEnabled(anyString(), anyBoolean());
        verifyNoInteractions(authenticatedUserProvider);
        verifyNoInteractions(auditEventService);
    }

    @Test
    void updateUserStatusRejectsSelfDisable() {
        UserAccount currentAdminAccount = user(CURRENT_ADMIN_ID, "admin@test.com", UserRole.ADMIN, true);
        UpdateUserStatusRequestDTO request = new UpdateUserStatusRequestDTO(UserStatusDTO.DISABLED);

        when(identityProvider.getUser(CURRENT_ADMIN_ID))
                .thenReturn(currentAdminAccount);
        when(authenticatedUserProvider.getCurrentUser())
                .thenReturn(currentAdmin());

        SelfDisableNotAllowedException exception = assertThrows(
                SelfDisableNotAllowedException.class,
                () -> userManagementService.updateUserStatus(CURRENT_ADMIN_ID, request)
        );

        assertEquals("Admin cannot disable their own account.", exception.getMessage());

        verify(identityProvider, never()).countEnabledAdmins();
        verify(identityProvider, never()).updateUserEnabled(anyString(), anyBoolean());
        verifyNoInteractions(auditEventService);
    }

    @Test
    void updateUserStatusRejectsLastActiveAdminDisable() {
        UserAccount targetAdmin = user(TARGET_ADMIN_ID, "target-admin@test.com", UserRole.ADMIN, true);
        UpdateUserStatusRequestDTO request = new UpdateUserStatusRequestDTO(UserStatusDTO.DISABLED);

        when(identityProvider.getUser(TARGET_ADMIN_ID))
                .thenReturn(targetAdmin);
        when(authenticatedUserProvider.getCurrentUser())
                .thenReturn(currentAdmin());
        when(identityProvider.countEnabledAdmins())
                .thenReturn(1L);

        LastActiveAdminException exception = assertThrows(
                LastActiveAdminException.class,
                () -> userManagementService.updateUserStatus(TARGET_ADMIN_ID, request)
        );

        assertEquals("Cannot disable the last active admin account.", exception.getMessage());

        verify(identityProvider).countEnabledAdmins();
        verify(identityProvider, never()).updateUserEnabled(anyString(), anyBoolean());
        verifyNoInteractions(auditEventService);
    }

    @Test
    void updateUserStatusAllowsAdminDisableWhenMoreThanOneActiveAdminExists() {
        UserAccount targetAdmin = user(TARGET_ADMIN_ID, "target-admin@test.com", UserRole.ADMIN, true);

        when(identityProvider.getUser(TARGET_ADMIN_ID))
                .thenReturn(targetAdmin);
        when(authenticatedUserProvider.getCurrentUser())
                .thenReturn(currentAdmin());
        when(identityProvider.countEnabledAdmins())
                .thenReturn(2L);

        UserDTO response = userManagementService.updateUserStatus(
                TARGET_ADMIN_ID,
                new UpdateUserStatusRequestDTO(UserStatusDTO.DISABLED)
        );

        assertEquals(UserStatusDTO.DISABLED, response.status());

        verify(identityProvider).countEnabledAdmins();
        verify(identityProvider).updateUserEnabled(TARGET_ADMIN_ID, false);
        verifyStatusAuditEvent(
                AuditEventType.USER_DISABLED,
                targetAdmin.withEnabled(false),
                "Disabled"
        );
    }

    @Test
    void createUserTrimsInputLowercasesEmailAndRecordsAuditEvent() {
        CreateUserRequestDTO request = new CreateUserRequestDTO(
                " John ",
                " Smith ",
                " JOHN.SMITH@TEST.COM ",
                UserRole.DOCTOR,
                "Temporary123"
        );

        when(identityProvider.createUser(
                "John",
                "Smith",
                "john.smith@test.com",
                "Temporary123",
                UserRole.DOCTOR
        )).thenReturn(TARGET_USER_ID);

        UserCreatedResponseDTO response = userManagementService.createUser(request);

        assertAll(
                () -> assertEquals(TARGET_USER_ID, response.id()),
                () -> assertEquals("john.smith@test.com", response.email()),
                () -> assertEquals("John", response.firstName()),
                () -> assertEquals("Smith", response.lastName()),
                () -> assertEquals(UserRole.DOCTOR, response.role()),
                () -> assertTrue(response.enabled())
        );

        verify(identityProvider).createUser(
                "John",
                "Smith",
                "john.smith@test.com",
                "Temporary123",
                UserRole.DOCTOR
        );

        verify(auditEventService).recordEvent(
                eq(AuditEventType.USER_CREATED),
                isNull(),
                isNull(),
                eq("User john.smith@test.com was created with role DOCTOR.")
        );
    }

    private void verifyStatusAuditEvent(
            AuditEventType eventType,
            UserAccount user,
            String status
    ) {
        ArgumentCaptor<String> metadataCaptor = ArgumentCaptor.forClass(String.class);

        verify(auditEventService).recordEventWithMetadata(
                eq(eventType),
                isNull(),
                isNull(),
                eq("User " + user.email() + " was " + status.toLowerCase() + "."),
                metadataCaptor.capture()
        );

        assertEquals(
                "{\"targetUserId\":\"" + user.id() + "\","
                        + "\"targetUserEmail\":\"" + user.email() + "\","
                        + "\"status\":\"" + status + "\"}",
                metadataCaptor.getValue()
        );
    }

    private static CurrentUserDTO currentAdmin() {
        return new CurrentUserDTO(
                CURRENT_ADMIN_ID,
                "admin@test.com",
                "Alice",
                "Admin",
                Set.of("ADMIN")
        );
    }

    private static UserAccount user(
            String id,
            String email,
            UserRole role,
            boolean enabled
    ) {
        return new UserAccount(
                id,
                email,
                "John",
                "Doe",
                Set.of(role),
                enabled
        );
    }
}