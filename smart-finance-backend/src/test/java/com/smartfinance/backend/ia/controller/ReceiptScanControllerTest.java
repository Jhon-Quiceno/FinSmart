package com.smartfinance.backend.ia.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinance.backend.common.config.SecurityConfig;
import com.smartfinance.backend.common.security.JwtService;
import com.smartfinance.backend.gastos.model.entity.CategoryType;
import com.smartfinance.backend.ia.model.dto.ReceiptExtraction;
import com.smartfinance.backend.ia.model.dto.ReceiptScanRequest;
import com.smartfinance.backend.ia.service.ReceiptExtractionService;
import com.smartfinance.backend.usuario.repository.UserRepository;
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

@WebMvcTest(ReceiptScanController.class)
@Import(SecurityConfig.class)
class ReceiptScanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ReceiptExtractionService receiptExtractionService;

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
    void scanReturns200WithExtractionForValidReceipt() throws Exception {
        ReceiptExtraction extraction = new ReceiptExtraction(
                true, "Supermercado", BigDecimal.valueOf(45000), CategoryType.EXPENSE, 2L, "Alimentación"
        );
        when(receiptExtractionService.extractFromImage(eq(1L), any())).thenReturn(extraction);

        ReceiptScanRequest request = new ReceiptScanRequest("data:image/jpeg;base64,ZmFrZQ==");

        mockMvc.perform(post("/api/receipts/scan")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isReceipt").value(true))
                .andExpect(jsonPath("$.description").value("Supermercado"))
                .andExpect(jsonPath("$.amount").value(45000))
                .andExpect(jsonPath("$.categoryName").value("Alimentación"));
    }

    @Test
    void scanReturns200WithNotAReceiptForUnrelatedImage() throws Exception {
        when(receiptExtractionService.extractFromImage(eq(1L), any())).thenReturn(ReceiptExtraction.notAReceipt());

        ReceiptScanRequest request = new ReceiptScanRequest("data:image/jpeg;base64,ZmFrZQ==");

        mockMvc.perform(post("/api/receipts/scan")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isReceipt").value(false))
                .andExpect(jsonPath("$.amount").doesNotExist());
    }

    @Test
    void scanReturns403WithoutAuthToken() throws Exception {
        ReceiptScanRequest request = new ReceiptScanRequest("data:image/jpeg;base64,ZmFrZQ==");

        mockMvc.perform(post("/api/receipts/scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void scanReturns400WhenImageDataUriIsBlank() throws Exception {
        ReceiptScanRequest request = new ReceiptScanRequest("");

        mockMvc.perform(post("/api/receipts/scan")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
