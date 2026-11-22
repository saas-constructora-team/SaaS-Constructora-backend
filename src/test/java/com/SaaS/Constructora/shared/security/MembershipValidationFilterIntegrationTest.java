package com.SaaS.Constructora.shared.security;

import com.SaaS.Constructora.identity.application.ports.UserRepository;
import com.SaaS.Constructora.identity.domain.Membership;
import com.SaaS.Constructora.identity.domain.Role;
import com.SaaS.Constructora.identity.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que el filtro de membresía rechace tokens válidos cuyo usuario no
 * pertenece (o ya no pertenece) a la empresa indicada en el JWT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class MembershipValidationFilterIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void tokenSinMembresiaActivaDevuelve403YNoLlegaAlControlador() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        User user = new User(userId, "sin-membresia@prueba.com", "Sin Membresía",
                "hash", true, LocalDateTime.now());
        Membership membership = new Membership(UUID.randomUUID(), userId, tenantId,
                Role.ADMIN, true, LocalDateTime.now());

        String token = jwtService.generateToken(user, membership);

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        assertThat(TenantContext.getCurrentTenantId()).isEmpty();
    }

    @Test
    void tokenDeOtraEmpresaNoAprovechaLaMembresiaDelUsuario() throws Exception {
        // El usuario semilla solo pertenece a la empresa demo. Un token firmado
        // para otra empresa no debe resolver el tenant desde su membresía real.
        User user = userRepository.findByEmail("admin@demo.com").orElseThrow();
        Membership membershipAjena = new Membership(UUID.randomUUID(), user.getId(),
                UUID.randomUUID(), Role.ADMIN, true, LocalDateTime.now());

        String token = jwtService.generateToken(user, membershipAjena);

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        assertThat(TenantContext.getCurrentTenantId()).isEmpty();
    }
}
