package com.chrisitstyle.mediscanflow.medicalplatform.users;

import com.chrisitstyle.mediscanflow.medicalplatform.users.dto.UserDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserMapper {
    UserDTO toDTO(UserAccount user) {
        return new UserDTO(
                user.id(),
                user.email(),
                user.firstName(),
                user.lastName(),
                user.roles(),
                user.status()
        );
    }

    List<UserDTO> toDTOs(List<UserAccount> users) {
        return users.stream()
                .map(this::toDTO)
                .toList();
    }
}
