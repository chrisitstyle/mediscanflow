package com.chrisitstyle.mediscanflow.medicalplatform.security;

import com.chrisitstyle.mediscanflow.medicalplatform.auth.UserRole;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Converts Keycloak JWT claims into Spring Security authorities.
 *
 * <p>The converter keeps the default scope-based authorities and additionally maps
 * supported Keycloak realm roles from the {@code realm_access.roles} claim to
 * application authorities such as {@code ROLE_ADMIN}, {@code ROLE_DOCTOR} and
 * {@code ROLE_STAFF}.</p>
 */
@Component
public class KeycloakJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM = "roles";

    private final JwtGrantedAuthoritiesConverter scopeAuthoritiesConverter =
            new JwtGrantedAuthoritiesConverter();

    @Override
    public Collection<GrantedAuthority> convert(@NonNull Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>(scopeAuthoritiesConverter.convert(jwt));

        extractRealmRoleNames(jwt).stream()
                .map(UserRole::fromName)
                .flatMap(Optional::stream)
                .map(UserRole::authority)
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        return authorities;
    }

    private Collection<String> extractRealmRoleNames(Jwt jwt) {
        Object realmAccessObject = jwt.getClaim(REALM_ACCESS_CLAIM);

        if (!(realmAccessObject instanceof Map<?, ?> realmAccess)) {
            return Set.of();
        }

        Object rolesObject = realmAccess.get(ROLES_CLAIM);

        if (!(rolesObject instanceof Collection<?> roles)) {
            return Set.of();
        }

        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }
}