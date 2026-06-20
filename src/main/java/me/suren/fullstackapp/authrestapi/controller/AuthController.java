package me.suren.fullstackapp.authrestapi.controller;

import me.suren.fullstackapp.authprovider.dto.TokenGenerateResponse;
import me.suren.fullstackapp.authprovider.dto.TokenValidateRequest;
import me.suren.fullstackapp.authprovider.dto.TokenValidationResponse;
import me.suren.fullstackapp.authprovider.service.AuthTokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
                "POST /token/validate called applicationId={} generatedTime={}",
                request.applicationId(),
                request.generatedTime()
        );
        if (request.token() != null) {
            log.debug("Validating JWT tokenLength={}", request.token().length());
        }
        TokenValidationResponse response = authTokenService.validateToken(request);
        log.info("Token validation result: {}", response.validation());
        return response;
    }
}

