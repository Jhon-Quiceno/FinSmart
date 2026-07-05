package com.smartfinance.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartfinance.backend.config.SecurityConfig;
import com.smartfinance.backend.dto.ai.CategorizeRequest;
import com.smartfinance.backend.dto.ai.CategorizeResponse;
import com.smartfinance.backend.repository.UserRepository;
import com.smartfinance.backend.service.AiCategorizationService;
import com.smartfinance.backend.service.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiCategorizationController.class)
@Import(SecurityConfig.class)
class AiCategorizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private AiCategorizationService service;

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
    void categorizeReturns200WithMatchedCategory() throws Exception {
        CategorizeRequest request = new CategorizeRequest("Almuerzo en restaurante", BigDecimal.valueOf(25000));
        when(service.categorize(eq(request))).thenReturn(new CategorizeResponse(1L, "Comida"));

        mockMvc.perform(post("/api/ai/categorize")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.categoryName").value("Comida"));
    }

    @Test
    void categorizeReturns200WithNullMatchWhenNoneFound() throws Exception {
        CategorizeRequest request = new CategorizeRequest("Pago de arriendo", null);
        when(service.categorize(eq(request))).thenReturn(new CategorizeResponse(null, null));

        mockMvc.perform(post("/api/ai/categorize")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void categorizeReturns400WhenDescriptionIsBlank() throws Exception {
        String invalidBody = """
                {"description": "", "amount": null}
                """;

        mockMvc.perform(post("/api/ai/categorize")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }
}
