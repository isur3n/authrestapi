package com.example.authrestapi.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TokenStatus {
    SUCCESS("success"),
    FAILED("failed"),
    EXPIRED("expired");

    private final String value;

    TokenStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
