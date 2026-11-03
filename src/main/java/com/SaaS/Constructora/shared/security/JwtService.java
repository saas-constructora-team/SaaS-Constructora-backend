package com.SaaS.Constructora.shared.security;

import com.SaaS.Constructora.identity.domain.Membership;
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
import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Emisión de JWTs (RS256) para el módulo identity.
 */
@Service
public class JwtService {

    /** Emisor de los tokens; también se valida al consumirlos. */
    public static final String ISSUER = "constructora-api";

    /** Claim donde viaja el tenant (empresa) autenticado. */
    public static final String TENANT_CLAIM = "tenant_id";

    /** Claim donde viaja el rol del usuario autenticado. */
    public static final String ROLE_CLAIM = "role";

    private final RSAKey signingKey;
    private final NimbusJwtEncoder encoder;
    private final long ttlSeconds;

    public JwtService(@Value("${app.security.jwt.ttl-minutes:60}") long ttlMinutes,
                      @Value("${app.security.jwt.private-key:}") String configuredPrivateKey) {
        try {
            this.signingKey = configuredPrivateKey.isBlank()
                    ? generateSigningKey()
                    : loadSigningKey(configuredPrivateKey);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo inicializar la clave RSA para firmar JWTs", e);
        }
        this.encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(signingKey)));
        this.ttlSeconds = Duration.ofMinutes(ttlMinutes).toSeconds();
    }

    public String generateToken(User user, Membership membership) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ttlSeconds))
                .claim(TENANT_CLAIM, membership.getTenantId().toString())
                .claim("email", user.getEmail())
                .claim(ROLE_CLAIM, membership.getRole().name())
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(signingKey.getKeyID())
                .build();

        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long getTokenTtlSeconds() {
        return ttlSeconds;
    }

    /** Clave pública usada por el resource server para validar las firmas. */
    public RSAPublicKey getPublicKey() {
        try {
            return signingKey.toRSAPublicKey();
        } catch (JOSEException e) {
            throw new IllegalStateException("No se pudo obtener la clave pública para validar JWTs", e);
        }
    }

    private static RSAKey generateSigningKey() throws JOSEException {
        return new RSAKeyGenerator(2048)
                .keyID(UUID.randomUUID().toString())
                .generate();
    }

    private static RSAKey loadSigningKey(String pem) throws Exception {
        String encodedKey = pem.replace("\\n", "\n")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
        RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new java.security.spec.RSAPublicKeySpec(
                        privateKey.getModulus(), privateKey.getPublicExponent()));
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
    }
}
