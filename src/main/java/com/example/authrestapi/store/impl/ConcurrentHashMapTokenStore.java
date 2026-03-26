package com.example.authrestapi.store.impl;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import com.example.authrestapi.store.TokenKey;
import com.example.authrestapi.store.TokenStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * In-memory token storage backed by {@link ConcurrentHashMap}.
 */
@Slf4j
@Component
public class ConcurrentHashMapTokenStore implements TokenStore {
    private final ConcurrentHashMap<TokenKey, String> tokens = new ConcurrentHashMap<>();

    @Override
    public void saveToken(String applicationId, Instant generatedTime, String token) {
        log.debug("Saving token applicationId={} generatedTime={}", applicationId, generatedTime);
        tokens.put(new TokenKey(applicationId, generatedTime), token);
    }

    @Override
    public Optional<String> retrieveToken(String applicationId, Instant generatedTime) {
        Optional<String> token = Optional.ofNullable(tokens.get(new TokenKey(applicationId, generatedTime)));
        log.debug(
                "Retrieving token applicationId={} generatedTime={} hit={}",
                applicationId,
                generatedTime,
                token.isPresent()
        );
        return token;
    }
}

