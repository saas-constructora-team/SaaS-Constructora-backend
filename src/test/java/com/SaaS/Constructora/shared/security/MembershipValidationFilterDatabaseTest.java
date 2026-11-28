package com.SaaS.Constructora.shared.security;

import com.SaaS.Constructora.identity.application.ports.MembershipRepository;
import com.SaaS.Constructora.shared.api.ApiError;
import jakarta.persistence.EntityManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import tools.jackson.databind.ObjectMapper;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Un fallo de base de datos antes del DispatcherServlet también debe sanitizarse. */
class MembershipValidationFilterDatabaseTest {

    private final MembershipRepository memberships = mock(MembershipRepository.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final MembershipValidationFilter filter =
            new MembershipValidationFilter(memberships, entityManager, objectMapper);

    @AfterEach
    void limpiaElContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void falloDeConexionAlValidarMembresiaDevuelve503SinElMensajeInterno() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        autentica(userId, tenantId);
        when(memberships.findActiveByUserIdAndTenantId(userId, tenantId))
                .thenThrow(new CannotGetJdbcConnectionException(
                        "HikariPool-7 - Connection is not available", new SQLException("timeout")));

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        ServletOutputStream output = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(output);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        verify(response).setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        ArgumentCaptor<ApiError> error = ArgumentCaptor.forClass(ApiError.class);
        verify(objectMapper).writeValue(any(ServletOutputStream.class), error.capture());
        assertEquals("SERVICE_UNAVAILABLE", error.getValue().code());
        assertFalse(error.getValue().message().contains("HikariPool"));
    }

    private static void autentica(UUID userId, UUID tenantId) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(userId.toString())
                .claim(JwtService.TENANT_CLAIM, tenantId.toString())
                .claim(JwtService.ROLE_CLAIM, "ADMIN")
                .build();
        // El constructor de un solo argumento no marca la autenticación como válida.
        SecurityContextHolder.getContext()
                .setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }
}
