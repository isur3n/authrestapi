package com.example.authrestapi.store;

import java.time.Instant;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class TokenKey {
    private final String applicationId;
    private final Instant generatedTime;
}

