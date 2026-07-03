package com.chrisitstyle.mediscanflow.medicalplatform.auth;

import com.chrisitstyle.mediscanflow.medicalplatform.auth.dto.CurrentUserDTO;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthenticatedUserProvider authenticatedUserProvider;

    public AuthController(AuthenticatedUserProvider authenticatedUserProvider) {
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping("/auth/me")
    public CurrentUserDTO getCurrentUser(JwtAuthenticationToken authentication) {
        return authenticatedUserProvider.getCurrentUser(authentication);
    }
}