package com.example.authrestapi.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    private final Jwt jwt = new Jwt();
    private final Revalidation revalidation = new Revalidation();

    @Data
    public static class Jwt {
        @NotBlank
        private String secret;
    }

    @Data
    public static class Revalidation {
        @Min(0)
        @Max(100)
        private int percentage = 50;
    }
}

