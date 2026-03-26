package com.example.authrestapi.controller;

import com.example.authrestapi.dto.TokenValidateRequest;
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

        JsonNode node = objectMapper.readTree(generateResult.getResponse().getContentAsString());
        Instant generatedTime = Instant.parse(node.get("generatedTime").asText());
        String token = node.get("token").asText();

        TokenValidateRequest validateRequest = TokenValidateRequest.builder()
                .generatedTime(generatedTime)
                .token(token)
                .build();

        mockMvc.perform(post("/token/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string(appId));
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
                .generatedTime(generatedTime.plusSeconds(1))
                .token(token)
                .build();

        mockMvc.perform(post("/token/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validateRequest)))
                .andExpect(status().isUnauthorized());
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
                .generatedTime(generatedTime)
                .token(invalidToken)
                .build();

        mockMvc.perform(post("/token/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validateRequest)))
                .andExpect(status().isUnauthorized());
    }
}

