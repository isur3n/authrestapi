package com.example.authrestapi.dto;

import lombok.Builder;

@Builder
public record TokenValidationResponse(
                TokenStatus validation) {
}
