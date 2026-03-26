package com.example.authrestapi.service;

import com.example.authrestapi.config.AuthProperties;
import com.example.authrestapi.dto.TokenGenerateResponse;
import com.example.authrestapi.dto.TokenValidateRequest;
import com.example.authrestapi.store.TokenStore;
import lombok.extern.slf4j.Slf4j;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class AuthTokenService {
    private static final Duration TTL = Duration.ofHours(1);

    private final TokenStore tokenStore;
    private final Key signingKey;
    private final AuthProperties authProperties;

    public AuthTokenService(TokenStore tokenStore, AuthProperties authProperties) {
        this.tokenStore = tokenStore;
        this.authProperties = authProperties;
        this.signingKey = Keys.hmacShaKeyFor(authProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
        log.info("AuthTokenService initialized revalidationPercentage={}", authProperties.getRevalidation().getPercentage());
    }

    public TokenGenerateResponse generateToken(String applicationId) {
        log.info("Generating token applicationId={}", applicationId);
        // Align with JWT claim + validation (epoch millis) so TokenStore keys match lookups.
        Instant generatedTime = Instant.ofEpochMilli(Instant.now().toEpochMilli());
        Instant expiryTime = generatedTime.plus(TTL);

        boolean randomRevalidation = shouldRevalidate();
        log.debug(
                "Token generation params applicationId={} generatedTime={} expiryTime={} randomRevalidation={}",
                applicationId,
                generatedTime,
                expiryTime,
                randomRevalidation
        );

        String jwt = Jwts.builder()
                .subject(applicationId)
                .issuedAt(Date.from(generatedTime))
                .expiration(Date.from(expiryTime))
                .claim("applicationId", applicationId)
                .claim("generatedTime", generatedTime.toEpochMilli())
                .claim("expiryTime", expiryTime.toEpochMilli())
                .claim("randomRevalidation", randomRevalidation)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();

        tokenStore.saveToken(applicationId, generatedTime, jwt);
        log.debug("JWT generated applicationId={} tokenLength={}", applicationId, jwt.length());

        return TokenGenerateResponse.builder()
                .generatedTime(generatedTime)
                .token(jwt)
                .build();
    }

    public String validateAndGetApplicationId(TokenValidateRequest request) {
        log.info("Validating token generatedTime={}", request.generatedTime());
        log.debug("Validating JWT tokenLength={}", request.token().length());
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(authProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(request.token())
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT token");
        }

        String applicationId = claims.get("applicationId", String.class);
        Long generatedTimeMillis = claims.get("generatedTime", Long.class);
        if (applicationId == null || generatedTimeMillis == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing required claims");
        }

        Instant generatedTime = Instant.ofEpochMilli(generatedTimeMillis);
        Instant requestTime = Instant.ofEpochMilli(request.generatedTime().toEpochMilli());
        log.debug(
                "JWT claims applicationId={} generatedTimeMillis={} requestGeneratedTime={}",
                applicationId,
                generatedTimeMillis,
                request.generatedTime()
        );
        if (!generatedTime.equals(requestTime)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Generated time mismatch");
        }

        String stored = tokenStore.retrieveToken(applicationId, generatedTime)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token not recognized"));

        if (!stored.equals(request.token())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token not recognized");
        }

        log.info("Token validated applicationId={}", applicationId);
        return applicationId;
    }

    private boolean shouldRevalidate() {
        int percentage = authProperties.getRevalidation().getPercentage();
        if (percentage <= 0) return false;
        if (percentage >= 100) return true;
        return ThreadLocalRandom.current().nextInt(100) < percentage;
    }
}

