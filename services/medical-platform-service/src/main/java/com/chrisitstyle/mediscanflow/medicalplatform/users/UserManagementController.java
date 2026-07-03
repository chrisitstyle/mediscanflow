package com.chrisitstyle.mediscanflow.medicalplatform.users;

import com.chrisitstyle.mediscanflow.medicalplatform.users.dto.CreateUserRequestDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.users.dto.UserCreatedResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserManagementController {

    private final UserManagementService userManagementService;

    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @PostMapping("/admin/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserCreatedResponseDTO createUser(
            @Valid @RequestBody CreateUserRequestDTO request
    ) {
        return userManagementService.createUser(request);
    }
}