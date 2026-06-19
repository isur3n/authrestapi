package com.example.authrestapi.dto;

import java.time.Instant;
import lombok.Builder;

@Builder
public record TokenGenerateResponse(
        String applicationId,
        Instant generatedTime,
        String token
) {}

