package com.example.authrestapi.dto;

import java.time.Instant;
import lombok.Builder;

@Builder
public record TokenGenerateResponse(
        Instant generatedTime,
        String token
) {}

