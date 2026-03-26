package com.example.authrestapi.security;

import com.example.authrestapi.config.AuthProperties;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtSigningKeyProvider {
    private final SecretKey signingKey;

    public JwtSigningKeyProvider(AuthProperties authProperties) {
        this.signingKey = Keys.hmacShaKeyFor(
                authProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8)
        );
        log.info("JwtSigningKeyProvider initialized secretLengthChars={}", authProperties.getJwt().getSecret().length());
    }

    public SecretKey getSigningKey() {
        log.debug("JwtSigningKeyProvider.getSigningKey called");
        return signingKey;
    }
}

