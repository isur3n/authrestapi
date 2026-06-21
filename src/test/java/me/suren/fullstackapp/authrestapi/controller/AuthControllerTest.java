package me.suren.fullstackapp.authrestapi.controller;

import me.suren.fullstackapp.authprovider.config.AuthProperties;
import me.suren.fullstackapp.authprovider.dto.TokenValidateRequest;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private AuthProperties authProperties;

        @Test
        void shouldReturn400WhenApplicationIdIsNotTenCharacters() throws Exception {
                mockMvc.perform(get("/token").param("applicationId", "abc"))
                                .andExpect(status().isBadRequest());
        }

    @Test
    void shouldGenerateAndValidateToken() throws Exception {
        String appId = "abcdefghij";

        MvcResult generateResult = mockMvc.perform(get("/token").param("applicationId", appId))
                        .andExpect(status().isOk())
                        .andReturn();

        String roundtripHeader = generateResult.getResponse().getHeader("x-api-roundtrip");
        org.junit.jupiter.api.Assertions.assertNotNull(roundtripHeader);
        org.junit.jupiter.api.Assertions.assertTrue(roundtripHeader.matches("\\d+"));

        JsonNode node = objectMapper.readTree(generateResult.getResponse().getContentAsString());
        Instant generatedTime = Instant.parse(node.get("generatedTime").asText());
        String token = node.get("token").asText();
        String responseAppId = node.get("applicationId").asText();
        org.junit.jupiter.api.Assertions.assertEquals(appId, responseAppId);

        TokenValidateRequest validateRequest = TokenValidateRequest.builder()
                        .applicationId(appId)
                        .generatedTime(generatedTime)
                        .token(token)
                        .build();

        MvcResult validateResult = mockMvc.perform(post("/token/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validateRequest)))
                        .andExpect(status().isOk())
                        .andExpect(content().json("{\"validation\":\"success\"}"))
                        .andReturn();

        String validateRoundtripHeader = validateResult.getResponse().getHeader("x-api-roundtrip");
        org.junit.jupiter.api.Assertions.assertNotNull(validateRoundtripHeader);
        org.junit.jupiter.api.Assertions.assertTrue(validateRoundtripHeader.matches("\\d+"));
    }

        @Test
        void shouldRejectWhenGeneratedTimeMismatch() throws Exception {
                String appId = "abcdefghij";

                MvcResult generateResult = mockMvc.perform(get("/token").param("applicationId", appId))
                                .andExpect(status().isOk())
                                .andReturn();

                JsonNode node = objectMapper.readTree(generateResult.getResponse().getContentAsString());
                Instant generatedTime = Instant.parse(node.get("generatedTime").asText());
                String token = node.get("token").asText();

                TokenValidateRequest validateRequest = TokenValidateRequest.builder()
                                .applicationId(appId)
                                .generatedTime(generatedTime.plusSeconds(1))
                                .token(token)
                                .build();

                mockMvc.perform(post("/token/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validateRequest)))
                                .andExpect(status().isOk())
                                .andExpect(content().json("{\"validation\":\"failed\"}"));
        }

        @Test
        void shouldRejectWhenJwtIsInvalid() throws Exception {
                String appId = "abcdefghij";

                MvcResult generateResult = mockMvc.perform(get("/token").param("applicationId", appId))
                                .andExpect(status().isOk())
                                .andReturn();

                JsonNode node = objectMapper.readTree(generateResult.getResponse().getContentAsString());
                Instant generatedTime = Instant.parse(node.get("generatedTime").asText());
                String token = node.get("token").asText();

                String invalidToken = token + "x";
                TokenValidateRequest validateRequest = TokenValidateRequest.builder()
                                .applicationId(appId)
                                .generatedTime(generatedTime)
                                .token(invalidToken)
                                .build();

                mockMvc.perform(post("/token/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validateRequest)))
                                .andExpect(status().isOk())
                                .andExpect(content().json("{\"validation\":\"failed\"}"));
        }

        @Test
        void shouldRejectWhenApplicationIdMismatch() throws Exception {
                String appId = "abcdefghij";

                MvcResult generateResult = mockMvc.perform(get("/token").param("applicationId", appId))
                                .andExpect(status().isOk())
                                .andReturn();

                JsonNode node = objectMapper.readTree(generateResult.getResponse().getContentAsString());
                Instant generatedTime = Instant.parse(node.get("generatedTime").asText());
                String token = node.get("token").asText();

                TokenValidateRequest validateRequest = TokenValidateRequest.builder()
                                .applicationId("different1")
                                .generatedTime(generatedTime)
                                .token(token)
                                .build();

                mockMvc.perform(post("/token/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validateRequest)))
                                .andExpect(status().isOk())
                                .andExpect(content().json("{\"validation\":\"failed\"}"));
        }

        @Test
        void shouldReturnExpiredWhenTokenIsExpired() throws Exception {
                String appId = "abcdefghij";
                java.security.Key signingKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                                authProperties.getJwt().getSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8));

                Instant generatedTime = Instant.now().minus(java.time.Duration.ofHours(2));
                Instant expiryTime = generatedTime.plus(java.time.Duration.ofHours(1));

                String expiredToken = io.jsonwebtoken.Jwts.builder()
                                .subject(appId)
                                .issuedAt(java.util.Date.from(generatedTime))
                                .expiration(java.util.Date.from(expiryTime))
                                .claim("applicationId", appId)
                                .claim("generatedTime", generatedTime.toEpochMilli())
                                .claim("expiryTime", expiryTime.toEpochMilli())
                                .claim("randomRevalidation", false)
                                .signWith(signingKey, io.jsonwebtoken.SignatureAlgorithm.HS256)
                                .compact();

                TokenValidateRequest validateRequest = TokenValidateRequest.builder()
                                .applicationId(appId)
                                .generatedTime(generatedTime)
                                .token(expiredToken)
                                .build();

                mockMvc.perform(post("/token/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validateRequest)))
                                .andExpect(status().isOk())
                                .andExpect(content().json("{\"validation\":\"expired\"}"));
        }
}
