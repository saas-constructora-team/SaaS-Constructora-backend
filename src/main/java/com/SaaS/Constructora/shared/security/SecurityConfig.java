package com.SaaS.Constructora.shared.security;

import com.SaaS.Constructora.shared.api.ApiError;
import com.SaaS.Constructora.identity.application.ports.MembershipRepository;
import tools.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad HTTP del monolito modular.
 * La autenticación se hace con JWT (Bearer) y el tenant se deriva de su claim
 * {@code tenant_id}; no se acepta un tenant enviado por el cliente.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final MembershipRepository membershipRepository;
    private final EntityManager entityManager;

    SecurityConfig(MembershipRepository membershipRepository, EntityManager entityManager) {
        this.membershipRepository = membershipRepository;
        this.entityManager = entityManager;
    }

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/error", "/actuator/**",
                        "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json");
                    objectMapper.writeValue(response.getOutputStream(), ApiError.of(
                            HttpStatus.UNAUTHORIZED.value(), "Unauthorized", "Se requiere autenticación"));
                })
                .accessDeniedHandler((request, response, exception) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType("application/json");
                    objectMapper.writeValue(response.getOutputStream(), ApiError.of(
                            HttpStatus.FORBIDDEN.value(), "Forbidden", "No tiene permiso para esta operación"));
                }))
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            // Debe ir después del resource server: allí ya está poblado el SecurityContext
            // con el JWT validado, del que se obtienen usuario y tenant.
            .addFilterAfter(
                new MembershipValidationFilter(membershipRepository, entityManager, objectMapper),
                BearerTokenAuthenticationFilter.class);
        return http.build();
    }
}
