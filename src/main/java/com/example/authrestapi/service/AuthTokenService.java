package com.example.authrestapi.service;

import com.example.authrestapi.config.AuthProperties;
import com.example.authrestapi.dto.TokenGenerateResponse;
import com.example.authrestapi.enums.TokenStatus;
import com.example.authrestapi.dto.TokenValidateRequest;
import com.example.authrestapi.dto.TokenValidationResponse;
import com.example.authrestapi.store.TokenStore;
import lombok.extern.slf4j.Slf4j;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthTokenService {

    private final TokenStore tokenStore;
    private final Key signingKey;
    private final AuthProperties authProperties;

    public AuthTokenService(TokenStore tokenStore, AuthProperties authProperties) {
        this.tokenStore = tokenStore;
        this.authProperties = authProperties;
        this.signingKey = Keys.hmacShaKeyFor(authProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
        log.info("AuthTokenService initialized revalidationPercentage={}",
                authProperties.getRevalidation().getPercentage());
    }

    public TokenGenerateResponse generateToken(String applicationId) {
        log.info("Generating token applicationId={}", applicationId);
        // Align with JWT claim + validation (epoch millis) so TokenStore keys match
        // lookups.
        Instant generatedTime = Instant.ofEpochMilli(Instant.now().toEpochMilli());
        Instant expiryTime = generatedTime.plus(authProperties.getTtl());

        boolean randomRevalidation = shouldRevalidate();
        log.debug(
                "Token generation params applicationId={} generatedTime={} expiryTime={} randomRevalidation={}",
                applicationId,
                generatedTime,
                expiryTime,
                randomRevalidation);

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
                .applicationId(applicationId)
                .build();
    }

    public TokenValidationResponse validateToken(TokenValidateRequest request) {
        if (request.token() == null || request.generatedTime() == null || request.applicationId() == null) {
            log.warn("Request is missing required fields");
            return new TokenValidationResponse(TokenStatus.FAILED);
        }

        log.info("Validating token generatedTime={} applicationId={}", request.generatedTime(),
                request.applicationId());
        log.debug("Validating JWT tokenLength={}", request.token().length());
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(
                            Keys.hmacShaKeyFor(authProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(request.token())
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT is expired: {}", e.getMessage());
            return new TokenValidationResponse(TokenStatus.EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return new TokenValidationResponse(TokenStatus.FAILED);
        }

        String claimApplicationId = claims.get("applicationId", String.class);
        Long generatedTimeMillis = claims.get("generatedTime", Long.class);
        if (claimApplicationId == null || generatedTimeMillis == null) {
            log.warn("JWT is missing required claims");
            return new TokenValidationResponse(TokenStatus.FAILED);
        }

        if (!claimApplicationId.equals(request.applicationId())) {
            log.warn("Application ID mismatch: claim={}, request={}", claimApplicationId, request.applicationId());
            return new TokenValidationResponse(TokenStatus.FAILED);
        }

        Instant generatedTime = Instant.ofEpochMilli(generatedTimeMillis);
        Instant requestTime = Instant.ofEpochMilli(request.generatedTime().toEpochMilli());
        log.debug(
                "JWT claims applicationId={} generatedTimeMillis={} requestGeneratedTime={}",
                claimApplicationId,
                generatedTimeMillis,
                request.generatedTime());
        if (!generatedTime.equals(requestTime)) {
            log.warn("Generated time mismatch: claim={}, request={}", generatedTime, requestTime);
            return new TokenValidationResponse(TokenStatus.FAILED);
        }

        String stored = tokenStore.retrieveToken(claimApplicationId, generatedTime).orElse(null);
        if (stored == null || !stored.equals(request.token())) {
            log.warn("Token not recognized in TokenStore");
            return new TokenValidationResponse(TokenStatus.FAILED);
        }

        log.info("Token validated successfully applicationId={}", claimApplicationId);
        return new TokenValidationResponse(TokenStatus.SUCCESS);
    }

    private boolean shouldRevalidate() {
        int percentage = authProperties.getRevalidation().getPercentage();
        if (percentage <= 0)
            return false;
        if (percentage >= 100)
            return true;
        return ThreadLocalRandom.current().nextInt(100) < percentage;
    }
}
