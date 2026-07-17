package com.smartfinance.backend.deudas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartfinance.backend.common.config.SecurityConfig;
import com.smartfinance.backend.common.exception.ResourceNotFoundException;
import com.smartfinance.backend.common.security.JwtService;
import com.smartfinance.backend.deudas.model.dto.DebtChargeRequest;
import com.smartfinance.backend.deudas.model.dto.DebtChargeResponse;
import com.smartfinance.backend.deudas.model.dto.DebtResponse;
import com.smartfinance.backend.deudas.service.DebtChargeService;
import com.smartfinance.backend.usuario.repository.UserRepository;
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

@WebMvcTest(DebtChargeController.class)
@Import(SecurityConfig.class)
class DebtChargeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private DebtChargeService debtChargeService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static final String AUTH_HEADER = "Bearer test-token";

    private final DebtChargeResponse charge = new DebtChargeResponse(
            1L, 10L, BigDecimal.valueOf(200), LocalDate.of(2026, 6, 1), "Compra supermercado", null
    );

    private final DebtResponse updatedDebt = new DebtResponse(
            10L, "Tarjeta Visa", BigDecimal.valueOf(1000), BigDecimal.valueOf(800), null, null, null, null
    );

    @BeforeEach
    void setUp() {
        Claims mockClaims = org.mockito.Mockito.mock(Claims.class);
        when(mockClaims.getSubject()).thenReturn("1");
        when(jwtService.parseAccessToken(any())).thenReturn(mockClaims);
        when(userRepository.existsById(1L)).thenReturn(true);
    }

    @Test
    void getChargesReturns200WithPagedResults() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        when(debtChargeService.getCharges(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(charge), pageable, 1));

        mockMvc.perform(get("/api/debts/10/charges").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void createChargeReturns201WithUpdatedDebtWhenValid() throws Exception {
        DebtChargeRequest request = new DebtChargeRequest(BigDecimal.valueOf(200), LocalDate.of(2026, 6, 1), "Compra supermercado");
        when(debtChargeService.createCharge(eq(10L), any(DebtChargeRequest.class))).thenReturn(updatedDebt);

        mockMvc.perform(post("/api/debts/10/charges")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.remainingAmount").value(800));
    }

    @Test
    void createChargeReturns400WhenAmountIsZero() throws Exception {
        DebtChargeRequest request = new DebtChargeRequest(BigDecimal.ZERO, null, null);

        mockMvc.perform(post("/api/debts/10/charges")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createChargeReturns404WhenDebtBelongsToAnotherUser() throws Exception {
        DebtChargeRequest request = new DebtChargeRequest(BigDecimal.valueOf(50), null, null);
        when(debtChargeService.createCharge(eq(99L), any(DebtChargeRequest.class)))
                .thenThrow(new ResourceNotFoundException("Deuda no encontrada"));

        mockMvc.perform(post("/api/debts/99/charges")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createChargeReturns403WithoutAuthToken() throws Exception {
        DebtChargeRequest request = new DebtChargeRequest(BigDecimal.valueOf(50), null, null);

        mockMvc.perform(post("/api/debts/10/charges")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
