package com.SaaS.Constructora.identity;

import com.SaaS.Constructora.identity.application.ports.EmailSendResult;
import com.SaaS.Constructora.identity.application.ports.EmailSender;
import com.SaaS.Constructora.identity.application.ports.GoogleTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración de las rebanadas HU-01 y HU-13: arranca PostgreSQL real
 * (Testcontainers), ejecuta todas las migraciones de Flyway y ejercita
 * login, registro y recuperación de contraseña.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class LoginIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecordingEmailSender emailSender;

    @BeforeEach
    void clearEmails() {
        emailSender.sent.clear();
        FakeGoogleTokenVerifier.reset();
    }

    @TestConfiguration
    static class TestEmailConfig {
        @Bean
        @Primary
        RecordingEmailSender recordingEmailSender() {
            return new RecordingEmailSender();
        }

        @Bean
        @Primary
        GoogleTokenVerifier fakeGoogleTokenVerifier() {
            return new FakeGoogleTokenVerifier();
        }
    }

    /** Sustituye la llamada real a Google: "id-token:<sub>|<email>|<nombre>" */
    static class FakeGoogleTokenVerifier implements GoogleTokenVerifier {
        static String lastToken;

        static void reset() { lastToken = null; }

        @Override
        public GoogleAccount verify(String idToken) {
            lastToken = idToken;
            String[] parts = idToken.split("\\|");
            String subject = parts[0].replace("id-token:", "");
            String email = parts.length > 1 ? parts[1] : subject + "@gmail.com";
            String name = parts.length > 2 ? parts[2] : "Usuario Google";
            if (idToken.contains("invalido")) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "Token de Google inválido o expirado");
            }
            return new GoogleAccount(subject, email, name, true);
        }
    }

    static class RecordingEmailSender implements EmailSender {
        final List<String> sent = new ArrayList<>();

        @Override
        public EmailSendResult sendPasswordResetEmail(String toEmail, String recipientName, String resetLink) {
            sent.add(resetLink);
            return EmailSendResult.delivered("test-id");
        }
    }

    @Test
    void loginConUsuarioSeedDevuelve200YToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@demo.com\",\"password\":\"Admin123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600))
                .andExpect(jsonPath("$.user.email").value("admin@demo.com"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.tenantId").value("5f2b9c1d-3e47-4a86-9c21-8d4f0a7b5e13"));
    }

    @Test
    void loginConPasswordIncorrectaDevuelve401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@demo.com\",\"password\":\"otra\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginConEmailInexistenteDevuelve401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@demo.com\",\"password\":\"Admin123!\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginSinBodyDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registroDeEmpresaYPosteriorLoginFuncionan() throws Exception {
        // Registro abierto: cualquiera crea su empresa y queda con sesión iniciada
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Constructora Andina\",\"taxId\":\"NIT-111\","
                                + "\"address\":\"Calle Andina 123\",\"adminFullName\":\"Ana Pérez\","
                                + "\"adminEmail\":\"ana@andina.com\",\"adminPassword\":\"Secreta123!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.tenant.name").value("Constructora Andina"))
                .andExpect(jsonPath("$.tenant.slug").value("constructora-andina"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.email").value("ana@andina.com"));

        // El admin recién creado puede autenticarse con sus credenciales
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@andina.com\",\"password\":\"Secreta123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    void registroConEmailYaUsadoDevuelve409() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Otra Empresa\",\"taxId\":\"NIT-222\","
                                + "\"address\":\"Calle Otra 456\",\"adminFullName\":\"Otro\","
                                + "\"adminEmail\":\"admin@demo.com\",\"adminPassword\":\"Secreta123!\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void registroConGoogleCreaEmpresaYPosteriorLoginEntraALaMisma() throws Exception {
        String token = "id-token:google-sub-1|gerente@google-nueva.com|Gerente Google";

        // Alta: cuenta nueva -> crea la empresa y responde 201
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"" + token + "\",\"companyName\":\"Constructora Google\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.newUser").value(true))
                .andExpect(jsonPath("$.tenant.slug").value("constructora-google"))
                .andExpect(jsonPath("$.user.email").value("gerente@google-nueva.com"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));

        // Segundo ingreso con el mismo "sub": ya existe -> 200 y no vuelve a crear empresa
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"" + token + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newUser").value(false))
                .andExpect(jsonPath("$.tenant.slug").value("constructora-google"));
    }

    @Test
    void googleSinNombreDeEmpresaGeneraNombresUnicosParaUsuariosHomonimos() throws Exception {
        // Primer usuario: no envía companyName -> nombre por defecto
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"id-token:google-sub-homo-1|homo1@correo.com|Juan Pérez\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenant.name").value("Empresa de Juan Pérez"))
                .andExpect(jsonPath("$.tenant.slug").value("empresa-de-juan-perez"));

        // Segundo usuario con el mismo nombre: no debe chocar (409), se genera uno único
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"id-token:google-sub-homo-2|homo2@correo.com|Juan Pérez\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenant.name", startsWith("Empresa de Juan Pérez ")))
                .andExpect(jsonPath("$.tenant.slug", startsWith("empresa-de-juan-perez-")));
    }

    @Test
    void loginConGoogleDeUsuarioExistenteVinculaLaCuenta() throws Exception {
        // El usuario semilla entra con Google: debe vincularse, no duplicarse
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"id-token:google-sub-admin|admin@demo.com|Administrador Demo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newUser").value(false))
                .andExpect(jsonPath("$.user.email").value("admin@demo.com"))
                .andExpect(jsonPath("$.tenant.slug").value("demo"));

        // Y ahora puede seguir entrando con contraseña (la cuenta no se alteró)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@demo.com\",\"password\":\"Admin123!\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void googleConTokenInvalidoDevuelve401() throws Exception {
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"id-token:invalido|nadie@gmail.com\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registroConGoogleYNombreDeEmpresaDuplicadoDevuelve409() throws Exception {
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"id-token:google-sub-dup|otro@gmail.com\","
                                + "\"companyName\":\"Constructora Ya Existe\"}"))
                .andExpect(status().isCreated());

        // El mismo correo con otra empresa choca con el email ya registrado
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"id-token:google-sub-dup2|otro@gmail.com\","
                                + "\"companyName\":\"Constructora Distinta\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newUser").value(false))
                .andExpect(jsonPath("$.tenant.slug").value("constructora-ya-existe"));

        // Y el nombre de empresa duplicado también choca (409) para un correo nuevo
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"id-token:google-sub-dup3|tercero@gmail.com\","
                                + "\"companyName\":\"Constructora Ya Existe\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void flujoCompletoDeRecuperacionDePassword() throws Exception {
        // 1) Creamos una empresa/usuario propios para no alterar el usuario semilla
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Constructora Recupera\",\"taxId\":\"NIT-333\","
                                + "\"address\":\"Calle Recupera 789\",\"adminFullName\":\"Rita Reset\","
                                + "\"adminEmail\":\"rita@recupera.com\",\"adminPassword\":\"Original123!\"}"))
                .andExpect(status().isCreated());

        // 2) Solicitar recuperación
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"rita@recupera.com\"}"))
                .andExpect(status().isOk());

        // 3) Se envió el correo con el enlace; extraemos el token
        org.junit.jupiter.api.Assertions.assertEquals(1, emailSender.sent.size());
        String link = emailSender.sent.getFirst();
        String token = link.substring(link.indexOf("token=") + "token=".length());

        // 4) Restablecer con el token
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"newPassword\":\"NuevaClave456!\"}"))
                .andExpect(status().isOk());

        // 5) La contraseña vieja ya no sirve y la nueva sí
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"rita@recupera.com\",\"password\":\"Original123!\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"rita@recupera.com\",\"password\":\"NuevaClave456!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()));

        // 6) El token ya no puede reutilizarse
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"newPassword\":\"OtraClave789!\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void forgotPasswordConEmailInexistenteNoRevelaNada() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nadie@demo.com\"}"))
                .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(0, emailSender.sent.size());
    }

    @Test
    void resetPasswordConTokenInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token-invalido\",\"newPassword\":\"NuevaClave456!\"}"))
                .andExpect(status().isBadRequest());
    }
}
