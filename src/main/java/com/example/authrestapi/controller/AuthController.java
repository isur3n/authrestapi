package com.example.authrestapi.controller;

import com.example.authrestapi.dto.TokenGenerateResponse;
import com.example.authrestapi.dto.TokenValidateRequest;
import com.example.authrestapi.dto.TokenValidationResponse;
import com.example.authrestapi.service.AuthTokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@Validated
public class AuthController {
    private final AuthTokenService authTokenService;

    public AuthController(AuthTokenService authTokenService) {
        this.authTokenService = authTokenService;
    }

    @GetMapping("/token")
    public TokenGenerateResponse generateToken(
            @RequestParam
            @Size(min = 10, max = 10)
            String applicationId
    ) {
        log.info("GET /token called applicationId={}", applicationId);
        TokenGenerateResponse response = authTokenService.generateToken(applicationId);
        log.debug(
                "Generated token applicationId={} generatedTime={} tokenLength={}",
                applicationId,
                response.generatedTime(),
                response.token().length()
        );
        return response;
    }

    @PostMapping("/token/validate")
    public TokenValidationResponse validate(@Valid @RequestBody TokenValidateRequest request) {
        log.info(
                "POST /token/validate called generatedTime={}",
                request.generatedTime()
        );
        log.debug("Validating JWT tokenLength={}", request.token().length());
        try {
            TokenValidationResponse response = authTokenService.validateAndGetApplicationId(request);
            log.info("Token validated successfully applicationId={}", response.applicationId());
            return response;
        } catch (ResponseStatusException e) {
            log.debug("Token validation failed status={} reason={}", e.getStatusCode(), e.getReason());
            throw e;
        }
    }
}

