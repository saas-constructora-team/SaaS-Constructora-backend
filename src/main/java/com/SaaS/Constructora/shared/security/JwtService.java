package com.SaaS.Constructora.shared.security;

import com.SaaS.Constructora.identity.domain.User;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Emisión de JWTs (RS256) para el módulo identity.
 * La clave RSA se genera al arrancar la aplicación; los tokens emitidos
 * caducan al reiniciar el proceso. Suficiente para el login inicial.
 */
@Service
public class JwtService {

    private final RSAKey signingKey;
    private final NimbusJwtEncoder encoder;
    private final long ttlSeconds;

    public JwtService(@Value("${app.security.jwt.ttl-minutes:60}") long ttlMinutes) {
        try {
            this.signingKey = new RSAKeyGenerator(2048)
                    .keyID(UUID.randomUUID().toString())
                    .generate();
        } catch (JOSEException e) {
            throw new IllegalStateException("No se pudo generar la clave RSA para firmar JWTs", e);
        }
        this.encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(signingKey)));
        this.ttlSeconds = Duration.ofMinutes(ttlMinutes).toSeconds();
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("constructora-api")
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ttlSeconds))
                .claim("tenant_id", user.getTenantId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(signingKey.getKeyID())
                .build();

        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long getTokenTtlSeconds() {
        return ttlSeconds;
    }
}
