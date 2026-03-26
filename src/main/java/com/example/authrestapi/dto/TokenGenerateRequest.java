package com.example.authrestapi.dto;

import lombok.Builder;

/**
 * Request DTO for generating a JWT token.
 *
 * Note: business validations are intentionally handled by {@code AuthValidationService}
 * so they can be reused from non-HTTP transports (e.g., gRPC).
 */
@Builder
public record TokenGenerateRequest(String applicationId) {
}

