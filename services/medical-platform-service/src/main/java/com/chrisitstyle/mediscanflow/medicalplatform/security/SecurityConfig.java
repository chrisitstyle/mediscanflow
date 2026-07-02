package com.chrisitstyle.mediscanflow.medicalplatform.security;

import com.chrisitstyle.mediscanflow.medicalplatform.auth.UserRole;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.chrisitstyle.mediscanflow.medicalplatform.auth.UserRole.ADMIN;
import static com.chrisitstyle.mediscanflow.medicalplatform.auth.UserRole.DOCTOR;
import static com.chrisitstyle.mediscanflow.medicalplatform.auth.UserRole.STAFF;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**"
                        ).permitAll()

                        .requestMatchers(HttpMethod.GET, "/auth/me")
                        .authenticated()

                        .requestMatchers(HttpMethod.GET, "/system/status")
                        .hasRole(role(ADMIN))

                        .requestMatchers(HttpMethod.GET, "/dashboard/**")
                        .hasAnyRole(role(ADMIN), role(DOCTOR), role(STAFF))

                        .requestMatchers(HttpMethod.GET, "/patients")
                        .hasAnyRole(role(ADMIN), role(DOCTOR), role(STAFF))

                        .requestMatchers(HttpMethod.GET, "/patients/*")
                        .hasAnyRole(role(ADMIN), role(DOCTOR), role(STAFF))

                        .requestMatchers(HttpMethod.GET, "/patients/*/analyses")
                        .hasAnyRole(role(ADMIN), role(DOCTOR), role(STAFF))

                        .requestMatchers(HttpMethod.POST, "/patients")
                        .hasAnyRole(role(ADMIN), role(DOCTOR))

                        .requestMatchers(HttpMethod.PUT, "/patients/*/profile")
                        .hasAnyRole(role(ADMIN), role(DOCTOR))

                        .requestMatchers(HttpMethod.PATCH, "/patients/*/archive")
                        .hasAnyRole(role(ADMIN), role(DOCTOR))

                        .requestMatchers(HttpMethod.PATCH, "/patients/*/restore")
                        .hasAnyRole(role(ADMIN), role(DOCTOR))

                        .requestMatchers(HttpMethod.GET, "/analyses")
                        .hasAnyRole(role(ADMIN), role(DOCTOR), role(STAFF))

                        .requestMatchers(HttpMethod.GET, "/analyses/recent")
                        .hasAnyRole(role(ADMIN), role(DOCTOR), role(STAFF))

                        .requestMatchers(HttpMethod.GET, "/analyses/*")
                        .hasAnyRole(role(ADMIN), role(DOCTOR), role(STAFF))

                        .requestMatchers(HttpMethod.GET, "/analyses/*/report")
                        .hasAnyRole(role(ADMIN), role(DOCTOR), role(STAFF))

                        .requestMatchers(HttpMethod.POST, "/patients/*/analyses")
                        .hasAnyRole(role(ADMIN), role(DOCTOR))

                        .requestMatchers(HttpMethod.POST, "/analyses/*/retry")
                        .hasAnyRole(role(ADMIN), role(DOCTOR))

                        .anyRequest()
                        .authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                )
                .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopeAuthoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        JwtAuthenticationConverter authenticationConverter =
                new JwtAuthenticationConverter();

        authenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Set<GrantedAuthority> authorities = new HashSet<>();

            Collection<GrantedAuthority> scopeAuthorities =
                    scopeAuthoritiesConverter.convert(jwt);

            if (scopeAuthorities != null) {
                authorities.addAll(scopeAuthorities);
            }

            authorities.addAll(extractRealmRoles(jwt));

            return authorities;
        });

        return authenticationConverter;
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

    private static String role(UserRole role) {
        return role.name();
    }
}