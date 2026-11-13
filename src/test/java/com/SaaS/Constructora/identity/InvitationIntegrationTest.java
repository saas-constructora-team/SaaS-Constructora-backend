package com.SaaS.Constructora.identity;

import com.SaaS.Constructora.identity.application.ports.EmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo completo de invitación y vinculación de trabajadores (invite-and-claim)
 * sobre PostgreSQL real con todas las migraciones de Flyway aplicadas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class InvitationIntegrationTest {

    private static final String DEMO_TENANT_ID = "5f2b9c1d-3e47-4a86-9c21-8d4f0a7b5e13";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecordingInvitationEmailSender emailSender;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearEmails() {
        emailSender.sent.clear();
    }

    @TestConfiguration
    static class TestEmailConfig {
        @Bean
        @Primary
        RecordingInvitationEmailSender recordingInvitationEmailSender() {
            return new RecordingInvitationEmailSender();
        }
    }

    static class RecordingInvitationEmailSender implements EmailSender {
        final List<String> sent = new ArrayList<>();

        @Override
        public void sendPasswordResetEmail(String toEmail, String recipientName, String resetLink) {
            sent.add(resetLink);
        }

        @Override
        public void sendInvitationEmail(String toEmail, String recipientName, String companyName,
                                        String inviteLink, long expiresInHours) {
            sent.add(inviteLink);
        }
    }

    private String loginAsAdmin() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@demo.com\",\"password\":\"Admin123!\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(body, "$.accessToken");
    }

    private String invite(String adminToken, String email, String documentNumber, String role)
            throws Exception {
        String roleField = role == null ? "" : ",\"role\":\"" + role + "\"";
        mockMvc.perform(post("/api/personnel/invite")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Obrero Invitado\",\"documentType\":\"NATIONAL_ID\","
                                + "\"documentNumber\":\"" + documentNumber + "\","
                                + "\"phone\":\"3001234567\",\"email\":\"" + email + "\","
                                + "\"emergencyContactName\":\"Contacto Emergencia\","
                                + "\"emergencyContactPhone\":\"3007654321\","
                                + "\"position\":\"Albañil\"" + roleField + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("INVITED"))
                .andExpect(jsonPath("$.membershipId").isNotEmpty())
                .andExpect(jsonPath("$.userId").isNotEmpty());

        assertEquals(1, emailSender.sent.size());
        String link = emailSender.sent.getFirst();
        return link.substring(link.indexOf("token=") + "token=".length());
    }

    @Test
    void invitarEmailNuevoCreaUsuarioInactivoYSeActivaAlAceptar() throws Exception {
        String adminToken = loginAsAdmin();
        String email = "nuevo-" + UUID.randomUUID() + "@obra.com";
        String document = "DOC-" + UUID.randomUUID();

        String token = invite(adminToken, email, document, null);

        // Usuario creado inactivo y sin membresía accesible todavía
        assertEquals(Boolean.FALSE, jdbcTemplate.queryForObject(
                "SELECT active FROM users WHERE email = ?", Boolean.class, email));
        assertEquals("INVITED", jdbcTemplate.queryForObject(
                "SELECT status FROM memberships m JOIN users u ON u.id = m.user_id "
                        + "WHERE u.email = ?", String.class, email));
        assertNull(jdbcTemplate.queryForObject(
                "SELECT password_hash FROM workers WHERE document_number = ?", String.class, document));

        // La cuenta inactiva no puede iniciar sesión
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Cualquiera123\"}"))
                .andExpect(status().isUnauthorized());

        // Previsualización pública con el token
        mockMvc.perform(get("/api/auth/invitations/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Constructora Demo"))
                .andExpect(jsonPath("$.email").value(email));

        // Aceptación: define contraseña y recibe sesión del tenant que invita
        mockMvc.perform(post("/api/auth/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"password\":\"NuevaClave123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.role").value("WORKER"))
                .andExpect(jsonPath("$.user.tenantId").value(DEMO_TENANT_ID));

        // Usuario y membresía quedan activos
        assertEquals(Boolean.TRUE, jdbcTemplate.queryForObject(
                "SELECT active FROM users WHERE email = ?", Boolean.class, email));
        assertEquals("ACTIVE", jdbcTemplate.queryForObject(
                "SELECT status FROM memberships m JOIN users u ON u.id = m.user_id "
                        + "WHERE u.email = ?", String.class, email));

        // Ya puede iniciar sesión con la contraseña que definió
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"NuevaClave123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        // El enlace es de un solo uso
        mockMvc.perform(post("/api/auth/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"password\":\"OtraClave123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invitarEmailExistenteReutilizaUsuarioYCreaNuevaMembresia() throws Exception {
        String email = "multi-" + UUID.randomUUID() + "@correo.com";

        // Empresa B con su propio admin
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Constructora B " + UUID.randomUUID() + "\","
                                + "\"adminFullName\":\"Admin B\",\"adminEmail\":\"" + email + "\","
                                + "\"adminPassword\":\"ClaveB1234\"}"))
                .andExpect(status().isCreated());

        String adminToken = loginAsAdmin();
        String token = invite(adminToken, email, "DOC-" + UUID.randomUUID(), null);

        // No se duplica la cuenta: mismo email, un solo usuario
        assertEquals(Integer.valueOf(1), jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email));
        assertEquals(Integer.valueOf(2), jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM memberships m JOIN users u ON u.id = m.user_id "
                        + "WHERE u.email = ?", Integer.class, email));

        // Al aceptar sin contraseña (la cuenta ya existía) entra directo a la empresa que invita
        mockMvc.perform(post("/api/auth/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.tenantId").value(DEMO_TENANT_ID));
    }

    @Test
    void invitarConRolAdminEsRechazado() throws Exception {
        String adminToken = loginAsAdmin();

        mockMvc.perform(post("/api/personnel/invite")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Intruso Admin\",\"documentType\":\"NATIONAL_ID\","
                                + "\"documentNumber\":\"DOC-" + UUID.randomUUID() + "\","
                                + "\"phone\":\"3001234567\","
                                + "\"email\":\"admin-intruso-" + UUID.randomUUID() + "@obra.com\","
                                + "\"emergencyContactName\":\"Contacto\","
                                + "\"emergencyContactPhone\":\"3007654321\","
                                + "\"position\":\"Albañil\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reenviarInvalidaElTokenAnterior() throws Exception {
        String adminToken = loginAsAdmin();
        String email = "reenvio-" + UUID.randomUUID() + "@obra.com";

        String firstToken = invite(adminToken, email, "DOC-" + UUID.randomUUID(), null);
        String membershipId = jdbcTemplate.queryForObject(
                "SELECT m.id FROM memberships m JOIN users u ON u.id = m.user_id WHERE u.email = ?",
                String.class, email);

        emailSender.sent.clear();
        mockMvc.perform(post("/api/personnel/invitations/" + membershipId + "/resend")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVITED"));

        assertEquals(1, emailSender.sent.size());
        String secondLink = emailSender.sent.getFirst();
        String secondToken = secondLink.substring(secondLink.indexOf("token=") + "token=".length());

        mockMvc.perform(get("/api/auth/invitations/" + firstToken))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/auth/invitations/" + secondToken))
                .andExpect(status().isOk());
    }
}
