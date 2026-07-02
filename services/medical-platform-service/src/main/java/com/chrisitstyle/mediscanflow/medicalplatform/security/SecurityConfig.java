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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import static com.chrisitstyle.mediscanflow.medicalplatform.auth.UserRole.*;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) {
        try {
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

                            .requestMatchers(HttpMethod.GET, "/patients/*/audit-events")
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

                            .requestMatchers(HttpMethod.GET, "/analyses/*/audit-events")
                            .hasAnyRole(role(ADMIN), role(DOCTOR), role(STAFF))

                            .requestMatchers(HttpMethod.POST, "/patients/*/analyses")
                            .hasAnyRole(role(ADMIN), role(DOCTOR))

                            .requestMatchers(HttpMethod.POST, "/analyses/*/retry")
                            .hasAnyRole(role(ADMIN), role(DOCTOR))

                            .requestMatchers(HttpMethod.GET, "/audit-events/recent")
                            .hasAnyRole(role(ADMIN), role(DOCTOR), role(STAFF))

                            .anyRequest()
                            .authenticated()
                    )
                    .oauth2ResourceServer(oauth2 -> oauth2
                            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                    )
                    .build();
        } catch (Exception exception) {
            throw new SecurityConfigurationException(
                    "Could not configure security filter chain.",
                    exception
            );
        }
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(
            KeycloakJwtGrantedAuthoritiesConverter keycloakJwtGrantedAuthoritiesConverter
    ) {
        JwtAuthenticationConverter authenticationConverter =
                new JwtAuthenticationConverter();

        authenticationConverter.setJwtGrantedAuthoritiesConverter(
                keycloakJwtGrantedAuthoritiesConverter
        );

        return authenticationConverter;
    }

    private static String role(UserRole role) {
        return role.name();
    }
}