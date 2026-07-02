package com.chrisitstyle.mediscanflow.medicalplatform.auth;

import com.chrisitstyle.mediscanflow.medicalplatform.auth.dto.CurrentUserDTO;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Component
public class AuthenticatedUserProvider {

    public CurrentUserDTO getCurrentUser() {
        Object authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            throw new IllegalStateException("No authenticated JWT user available");
        }

        return getCurrentUser(jwtAuthenticationToken);
    }

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