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

@Component
public class KeycloakJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final JwtGrantedAuthoritiesConverter scopeAuthoritiesConverter =
            new JwtGrantedAuthoritiesConverter();

    @Override
    public Collection<GrantedAuthority> convert(@NonNull Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        authorities.addAll(scopeAuthoritiesConverter.convert(jwt));
        authorities.addAll(extractRealmRoles(jwt));

        return authorities;
    }

    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        Object realmAccessObject = jwt.getClaim("realm_access");

        if (!(realmAccessObject instanceof Map<?, ?> realmAccess)) {
            return authorities;
        }

        Object rolesObject = realmAccess.get("roles");

        if (!(rolesObject instanceof Collection<?> roles)) {
            return authorities;
        }

        roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(UserRole::fromName)
                .flatMap(Optional::stream)
                .map(UserRole::authority)
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        return authorities;
    }
}