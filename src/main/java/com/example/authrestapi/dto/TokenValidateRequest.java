package com.example.authrestapi.dto;

import java.time.Instant;
import lombok.Builder;

@Builder
public record TokenValidateRequest(
        Instant generatedTime,
        String token
) {}

