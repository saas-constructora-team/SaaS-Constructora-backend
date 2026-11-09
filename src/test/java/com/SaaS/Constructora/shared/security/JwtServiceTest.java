package com.SaaS.Constructora.shared.security;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class JwtServiceTest {

    @Test
    void instancesConfiguredWithSamePrivateKeySharePublicKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        String configuredKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        JwtService firstInstance = new JwtService(60, configuredKey);
        JwtService secondInstance = new JwtService(60, configuredKey);

        assertArrayEquals(firstInstance.getPublicKey().getEncoded(),
                secondInstance.getPublicKey().getEncoded());
    }
}
