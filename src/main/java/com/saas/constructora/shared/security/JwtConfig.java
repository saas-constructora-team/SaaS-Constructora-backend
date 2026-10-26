package com.saas.constructora.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Configura el consumidor de JWTs con la misma clave RSA con la que
 * {@link JwtService} firma los tokens. Valida firma, expiración e issuer.
 */
@Configuration
public class JwtConfig {

    @Bean
    JwtDecoder jwtDecoder(JwtService jwtService) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey(jwtService.getPublicKey())
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(JwtService.ISSUER));
        return decoder;
    }
}
