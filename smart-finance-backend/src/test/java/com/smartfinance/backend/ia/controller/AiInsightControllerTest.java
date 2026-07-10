package com.smartfinance.backend.ia.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartfinance.backend.common.config.SecurityConfig;
import com.smartfinance.backend.ia.model.dto.InsightResponse;
import com.smartfinance.backend.ia.exception.AiProviderNotConfiguredException;
import com.smartfinance.backend.usuario.repository.UserRepository;
import com.smartfinance.backend.ia.service.AiInsightService;
import com.smartfinance.backend.common.security.JwtService;
import com.smartfinance.backend.ia.service.ai.AiChatOrchestrator;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiInsightController.class)
@Import(SecurityConfig.class)
class AiInsightControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private AiInsightService aiInsightService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static final String AUTH_HEADER = "Bearer test-token";

    @BeforeEach
    void setUp() {
        Claims mockClaims = org.mockito.Mockito.mock(Claims.class);
        when(mockClaims.getSubject()).thenReturn("1");
        when(jwtService.parseAccessToken(any())).thenReturn(mockClaims);
        when(userRepository.existsById(1L)).thenReturn(true);
    }

    @Test
    void getLatestReturns200WhenAnInsightExists() throws Exception {
        when(aiInsightService.getLatestInsight()).thenReturn(
                new InsightResponse(1L, "- Ahorra más", "groq", "llama-3.3", Instant.parse("2026-07-03T10:00:00Z"))
        );

        mockMvc.perform(get("/api/ai/insights").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("- Ahorra más"));
    }

    @Test
    void getLatestReturns204WhenNoneExistsYet() throws Exception {
        when(aiInsightService.getLatestInsight()).thenReturn(null);

        mockMvc.perform(get("/api/ai/insights").header("Authorization", AUTH_HEADER))
                .andExpect(status().isNoContent());
    }

    @Test
    void generateReturns200WithNewlyGeneratedInsight() throws Exception {
        when(aiInsightService.generateInsight()).thenReturn(
                new InsightResponse(2L, "- Reduce gastos en comida", "groq", "llama-3.3", Instant.parse("2026-07-03T10:00:00Z"))
        );

        mockMvc.perform(post("/api/ai/insights/generate").header("Authorization", AUTH_HEADER).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("- Reduce gastos en comida"));
    }

    @Test
    void generateReturns503WhenNoProviderIsConfigured() throws Exception {
        when(aiInsightService.generateInsight()).thenThrow(
                new AiProviderNotConfiguredException(AiChatOrchestrator.GENERIC_MESSAGE)
        );

        mockMvc.perform(post("/api/ai/insights/generate").header("Authorization", AUTH_HEADER).with(csrf()))
                .andExpect(status().isServiceUnavailable());
    }
}
