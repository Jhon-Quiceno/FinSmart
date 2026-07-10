package com.smartfinance.backend.deudas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartfinance.backend.common.config.SecurityConfig;
import com.smartfinance.backend.deudas.model.dto.DebtPaymentRequest;
import com.smartfinance.backend.deudas.model.dto.DebtPaymentResponse;
import com.smartfinance.backend.common.exception.ResourceNotFoundException;
import com.smartfinance.backend.usuario.repository.UserRepository;
import com.smartfinance.backend.deudas.service.DebtPaymentService;
import com.smartfinance.backend.common.security.JwtService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DebtPaymentController.class)
@Import(SecurityConfig.class)
class DebtPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private DebtPaymentService debtPaymentService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static final String AUTH_HEADER = "Bearer test-token";

    private final DebtPaymentResponse payment = new DebtPaymentResponse(
            1L, 10L, BigDecimal.valueOf(200), LocalDate.of(2026, 6, 1), "Abono mensual", null, 77L
    );

    @BeforeEach
    void setUp() {
        Claims mockClaims = org.mockito.Mockito.mock(Claims.class);
        when(mockClaims.getSubject()).thenReturn("1");
        when(jwtService.parseAccessToken(any())).thenReturn(mockClaims);
        when(userRepository.existsById(1L)).thenReturn(true);
    }

    @Test
    void getPaymentsReturns200WithPagedResults() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        when(debtPaymentService.getPayments(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(payment), pageable, 1));

        mockMvc.perform(get("/api/debts/10/payments").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void createPaymentReturns201WhenValid() throws Exception {
        DebtPaymentRequest request = new DebtPaymentRequest(BigDecimal.valueOf(200), LocalDate.of(2026, 6, 1), "Abono mensual");
        when(debtPaymentService.createPayment(eq(10L), any(DebtPaymentRequest.class))).thenReturn(payment);

        mockMvc.perform(post("/api/debts/10/payments")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.expenseId").value(77L));
    }

    @Test
    void createPaymentReturns400WhenAmountExceedsRemainingAmount() throws Exception {
        DebtPaymentRequest request = new DebtPaymentRequest(BigDecimal.valueOf(9999), null, null);
        when(debtPaymentService.createPayment(eq(10L), any(DebtPaymentRequest.class)))
                .thenThrow(new IllegalArgumentException("El abono no puede superar el saldo restante de la deuda"));

        mockMvc.perform(post("/api/debts/10/payments")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPaymentReturns400WhenAmountIsZero() throws Exception {
        DebtPaymentRequest request = new DebtPaymentRequest(BigDecimal.ZERO, null, null);

        mockMvc.perform(post("/api/debts/10/payments")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPaymentReturns404WhenDebtBelongsToAnotherUser() throws Exception {
        DebtPaymentRequest request = new DebtPaymentRequest(BigDecimal.valueOf(50), null, null);
        when(debtPaymentService.createPayment(eq(99L), any(DebtPaymentRequest.class)))
                .thenThrow(new ResourceNotFoundException("Deuda no encontrada"));

        mockMvc.perform(post("/api/debts/99/payments")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPaymentsReturns403WithoutAuthToken() throws Exception {
        mockMvc.perform(get("/api/debts/10/payments"))
                .andExpect(status().isForbidden());
    }
}
