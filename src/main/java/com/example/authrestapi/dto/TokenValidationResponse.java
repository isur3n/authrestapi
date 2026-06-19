package com.example.authrestapi.dto;

import com.example.authrestapi.enums.TokenStatus;
import lombok.Builder;

@Builder
public record TokenValidationResponse(
                TokenStatus validation) {
}
