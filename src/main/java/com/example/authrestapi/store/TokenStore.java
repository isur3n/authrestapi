package com.example.authrestapi.store;

import java.time.Instant;
import java.util.Optional;

public interface TokenStore {
    void saveToken(String applicationId, Instant generatedTime, String token);

    Optional<String> retrieveToken(String applicationId, Instant generatedTime);
}

