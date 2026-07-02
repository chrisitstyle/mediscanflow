package com.chrisitstyle.mediscanflow.medicalplatform.auth;

import com.chrisitstyle.mediscanflow.medicalplatform.auth.dto.CurrentUserDTO;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@RestController
public class AuthController {

    @GetMapping("/auth/me")
    public CurrentUserDTO getCurrentUser(JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();

        Set<String> roles = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .map(UserRole::fromAuthority)
                .flatMap(Optional::stream)
                .map(UserRole::name)
                .collect(Collectors.toCollection(TreeSet::new));

        return new CurrentUserDTO(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("given_name"),
                jwt.getClaimAsString("family_name"),
                roles
        );
    }
}