package com.example.authrestapi.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Data
@Validated
@ConfigurationProperties(prefix = "auth")
public class AuthProperties implements InitializingBean {

    private final Jwt jwt = new Jwt();
    private final Revalidation revalidation = new Revalidation();

    @Setter(AccessLevel.NONE)
    private Duration ttl;

    public void setTtl(Long ttlSeconds) {
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

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

    @Override
    public void afterPropertiesSet() throws Exception {
        if (this.ttl == null) {
            this.ttl = Duration.ofSeconds(300);
        }
    }
}
