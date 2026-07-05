package com.chrisitstyle.mediscanflow.medicalplatform.users;

import com.chrisitstyle.mediscanflow.medicalplatform.auth.UserRole;
import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.LastActiveAdminException;
import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.SelfDisableNotAllowedException;
import com.chrisitstyle.mediscanflow.medicalplatform.users.dto.CreateUserRequestDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.users.dto.UpdateUserStatusRequestDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.users.dto.UserCreatedResponseDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.users.dto.UserDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.users.dto.UserStatusDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserManagementControllerTest {
    private static final String USER_ID = "user-1";
    private static final String ADMIN_ID = "admin-1";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserManagementService userManagementService;

    @Test
    void getUsersReturnsUsers() throws Exception {
        when(userManagementService.getUsers())
                .thenReturn(List.of(
                        userDTO(ADMIN_ID, "admin@test.com", UserRole.ADMIN, UserStatusDTO.ENABLED),
                        userDTO(USER_ID, "doctor@test.com", UserRole.DOCTOR, UserStatusDTO.DISABLED)
                ));

        mockMvc.perform(get("/api/admin/users").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(ADMIN_ID))
                .andExpect(jsonPath("$[0].email").value("admin@test.com"))
                .andExpect(jsonPath("$[0].roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$[0].status").value("Enabled"))
                .andExpect(jsonPath("$[1].id").value(USER_ID))
                .andExpect(jsonPath("$[1].email").value("doctor@test.com"))
                .andExpect(jsonPath("$[1].roles[0]").value("DOCTOR"))
                .andExpect(jsonPath("$[1].status").value("Disabled"));

        verify(userManagementService).getUsers();
    }

    @Test
    void getUserReturnsUser() throws Exception {
        when(userManagementService.getUser(USER_ID))
                .thenReturn(userDTO(USER_ID, "doctor@test.com", UserRole.DOCTOR, UserStatusDTO.ENABLED));

        mockMvc.perform(get("/api/admin/users/{userId}", USER_ID).contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.email").value("doctor@test.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.roles[0]").value("DOCTOR"))
                .andExpect(jsonPath("$.status").value("Enabled"));

        verify(userManagementService).getUser(USER_ID);
    }

    @Test
    void updateUserStatusReturnsUpdatedUser() throws Exception {
        when(userManagementService.updateUserStatus(
                eq(USER_ID),
                any(UpdateUserStatusRequestDTO.class)
        )).thenReturn(userDTO(USER_ID, "doctor@test.com", UserRole.DOCTOR, UserStatusDTO.DISABLED));

        mockMvc.perform(patch("/api/admin/users/{userId}/status", USER_ID)
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "Disabled"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.email").value("doctor@test.com"))
                .andExpect(jsonPath("$.status").value("Disabled"));

        ArgumentCaptor<UpdateUserStatusRequestDTO> requestCaptor =
                ArgumentCaptor.forClass(UpdateUserStatusRequestDTO.class);

        verify(userManagementService).updateUserStatus(eq(USER_ID), requestCaptor.capture());
        assertEquals(UserStatusDTO.DISABLED, requestCaptor.getValue().status());
    }

    @Test
    void updateUserStatusReturnsBadRequestWhenStatusIsMissing() throws Exception {
        mockMvc.perform(patch("/api/admin/users/{userId}/status", USER_ID)
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.status").value("Status is required"));

        verify(userManagementService, never())
                .updateUserStatus(eq(USER_ID), any(UpdateUserStatusRequestDTO.class));
    }

    @Test
    void updateUserStatusReturnsBadRequestWhenStatusIsInvalid() throws Exception {
        mockMvc.perform(patch("/api/admin/users/{userId}/status", USER_ID)
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "Blocked"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));

        verify(userManagementService, never())
                .updateUserStatus(eq(USER_ID), any(UpdateUserStatusRequestDTO.class));
    }

    @Test
    void updateUserStatusReturnsBadRequestWhenAdminDisablesSelf() throws Exception {
        when(userManagementService.updateUserStatus(
                eq(ADMIN_ID),
                any(UpdateUserStatusRequestDTO.class)
        )).thenThrow(new SelfDisableNotAllowedException());

        mockMvc.perform(patch("/api/admin/users/{userId}/status", ADMIN_ID)
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "Disabled"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Admin cannot disable their own account."));

        verify(userManagementService)
                .updateUserStatus(eq(ADMIN_ID), any(UpdateUserStatusRequestDTO.class));
    }

    @Test
    void updateUserStatusReturnsConflictWhenLastActiveAdminWouldBeDisabled() throws Exception {
        when(userManagementService.updateUserStatus(
                eq(ADMIN_ID),
                any(UpdateUserStatusRequestDTO.class)
        )).thenThrow(new LastActiveAdminException());

        mockMvc.perform(patch("/api/admin/users/{userId}/status", ADMIN_ID)
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "Disabled"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot disable the last active admin account."));

        verify(userManagementService)
                .updateUserStatus(eq(ADMIN_ID), any(UpdateUserStatusRequestDTO.class));
    }

    @Test
    void createUserReturnsCreatedUser() throws Exception {
        when(userManagementService.createUser(any(CreateUserRequestDTO.class)))
                .thenReturn(new UserCreatedResponseDTO(
                        USER_ID,
                        "doctor@test.com",
                        "John",
                        "Doe",
                        UserRole.DOCTOR,
                        true
                ));

        mockMvc.perform(post("/api/admin/users")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "John",
                                  "lastName": "Doe",
                                  "email": "doctor@test.com",
                                  "role": "DOCTOR",
                                  "temporaryPassword": "Temporary123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.email").value("doctor@test.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.role").value("DOCTOR"))
                .andExpect(jsonPath("$.enabled").value(true));

        verify(userManagementService).createUser(any(CreateUserRequestDTO.class));
    }

    private static UserDTO userDTO(
            String id,
            String email,
            UserRole role,
            UserStatusDTO status
    ) {
        return new UserDTO(
                id,
                email,
                "John",
                "Doe",
                Set.of(role),
                status
        );
    }
}