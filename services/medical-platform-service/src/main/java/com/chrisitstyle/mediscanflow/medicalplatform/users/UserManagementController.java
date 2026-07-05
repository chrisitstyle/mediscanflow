package com.chrisitstyle.mediscanflow.medicalplatform.users;

import com.chrisitstyle.mediscanflow.medicalplatform.users.dto.CreateUserRequestDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.users.dto.UpdateUserStatusRequestDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.users.dto.UserCreatedResponseDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.users.dto.UserDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
public class UserManagementController {
    private final UserManagementService userManagementService;

    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    public List<UserDTO> getUsers() {
        return userManagementService.getUsers();
    }

    @GetMapping("/{userId}")
    public UserDTO getUser(@PathVariable String userId) {
        return userManagementService.getUser(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserCreatedResponseDTO createUser(
            @Valid @RequestBody CreateUserRequestDTO request
    ) {
        return userManagementService.createUser(request);
    }

    @PatchMapping("/{userId}/status")
    public UserDTO updateUserStatus(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserStatusRequestDTO request
    ) {
        return userManagementService.updateUserStatus(userId, request);
    }
}
