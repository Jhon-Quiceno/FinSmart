package com.smartfinance.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartfinance.backend.config.SecurityConfig;
import com.smartfinance.backend.dto.ai.ChatMessageResponse;
import com.smartfinance.backend.dto.ai.ChatReplyResponse;
import com.smartfinance.backend.dto.ai.ChatRequest;
import com.smartfinance.backend.exception.AiProviderNotConfiguredException;
import com.smartfinance.backend.exception.AiProviderRateLimitException;
import com.smartfinance.backend.model.AiMessageRole;
import com.smartfinance.backend.repository.UserRepository;
import com.smartfinance.backend.service.AiChatService;
import com.smartfinance.backend.service.JwtService;
import com.smartfinance.backend.service.ai.AiChatOrchestrator;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiChatController.class)
@Import(SecurityConfig.class)
class AiChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private AiChatService aiChatService;

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
    void chatReturns200WithAssistantReply() throws Exception {
        ChatRequest request = new ChatRequest("¿cómo voy este mes?");
        when(aiChatService.chat(eq(request))).thenReturn(
                new ChatReplyResponse("Vas bien este mes", "groq", "llama-3.3", Instant.parse("2026-07-03T10:00:00Z"))
        );

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Vas bien este mes"))
                .andExpect(jsonPath("$.providerName").value("groq"));
    }

    @Test
    void chatReturns400WhenMessageIsBlank() throws Exception {
        String invalidBody = """
                {"message": ""}
                """;

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chatReturns503WhenNoProviderIsConfigured() throws Exception {
        ChatRequest request = new ChatRequest("hola");
        when(aiChatService.chat(eq(request))).thenThrow(
                new AiProviderNotConfiguredException(AiChatOrchestrator.GENERIC_MESSAGE)
        );

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(AiChatOrchestrator.GENERIC_MESSAGE));
    }

    @Test
    void chatReturns429WhenProviderRateLimited() throws Exception {
        ChatRequest request = new ChatRequest("hola");
        when(aiChatService.chat(eq(request))).thenThrow(new AiProviderRateLimitException("groq"));

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("El proveedor de IA alcanzó su límite de uso. Intenta de nuevo en unos minutos."));
    }

    @Test
    void getHistoryReturns200WithPagedResults() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        ChatMessageResponse message = new ChatMessageResponse(
                1L, AiMessageRole.USER, "hola", null, null, Instant.parse("2026-07-03T10:00:00Z")
        );
        when(aiChatService.getHistory(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(message), pageable, 1));

        mockMvc.perform(get("/api/ai/chat/history").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].content").value("hola"));
    }
}
