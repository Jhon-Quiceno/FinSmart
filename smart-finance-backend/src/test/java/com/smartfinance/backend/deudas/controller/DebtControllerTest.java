package com.smartfinance.backend.deudas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartfinance.backend.common.config.SecurityConfig;
import com.smartfinance.backend.deudas.model.dto.DebtRequest;
import com.smartfinance.backend.deudas.model.dto.DebtResponse;
import com.smartfinance.backend.deudas.model.dto.DebtUpdateRequest;
import com.smartfinance.backend.common.exception.ResourceNotFoundException;
import com.smartfinance.backend.usuario.repository.UserRepository;
import com.smartfinance.backend.deudas.service.DebtService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DebtController.class)
@Import(SecurityConfig.class)
class DebtControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private DebtService debtService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static final String AUTH_HEADER = "Bearer test-token";

    private final DebtResponse creditCardDebt = new DebtResponse(
            1L, "Tarjeta de crédito", BigDecimal.valueOf(1000), BigDecimal.valueOf(1000),
            BigDecimal.valueOf(2.5), LocalDate.of(2026, 12, 1), null, null
    );

    @BeforeEach
    void setUp() {
        Claims mockClaims = org.mockito.Mockito.mock(Claims.class);
        when(mockClaims.getSubject()).thenReturn("1");
        when(jwtService.parseAccessToken(any())).thenReturn(mockClaims);
        when(userRepository.existsById(1L)).thenReturn(true);
    }

    @Test
    void getDebtsReturns200WithPagedResults() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        when(debtService.getDebts(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(creditCardDebt), pageable, 1));

        mockMvc.perform(get("/api/debts").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Tarjeta de crédito"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void createDebtReturns201WhenValid() throws Exception {
        DebtRequest request = new DebtRequest("Tarjeta de crédito", BigDecimal.valueOf(1000), BigDecimal.valueOf(2.5), null);
        when(debtService.createDebt(any(DebtRequest.class))).thenReturn(creditCardDebt);

        mockMvc.perform(post("/api/debts")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.remainingAmount").value(1000));
    }

    @Test
    void createDebtReturns400WhenTotalAmountIsZero() throws Exception {
        DebtRequest request = new DebtRequest("Tarjeta de crédito", BigDecimal.ZERO, null, null);

        mockMvc.perform(post("/api/debts")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDebtReturns400WhenNameIsMissing() throws Exception {
        String invalidBody = """
                {"totalAmount": 1000}
                """;

        mockMvc.perform(post("/api/debts")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDebtReturns400WhenInterestRateIsNegative() throws Exception {
        DebtRequest request = new DebtRequest("Tarjeta de crédito", BigDecimal.valueOf(1000), BigDecimal.valueOf(-1), null);

        mockMvc.perform(post("/api/debts")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateDebtReturns200WhenUpdatingOwnDebt() throws Exception {
        DebtUpdateRequest request = new DebtUpdateRequest("Tarjeta actualizada", BigDecimal.valueOf(3.0), LocalDate.of(2026, 12, 15));
        when(debtService.updateDebt(eq(1L), any(DebtUpdateRequest.class))).thenReturn(creditCardDebt);

        mockMvc.perform(put("/api/debts/1")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void updateDebtReturns404WhenUpdatingAnotherUsersDebt() throws Exception {
        DebtUpdateRequest request = new DebtUpdateRequest("Tarjeta actualizada", null, null);
        when(debtService.updateDebt(eq(99L), any(DebtUpdateRequest.class)))
                .thenThrow(new ResourceNotFoundException("Deuda no encontrada"));

        mockMvc.perform(put("/api/debts/99")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteDebtReturns204WhenDeletingOwnDebt() throws Exception {
        mockMvc.perform(delete("/api/debts/1")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteDebtReturns404WhenDeletingAnotherUsersDebt() throws Exception {
        doThrow(new ResourceNotFoundException("Deuda no encontrada"))
                .when(debtService).deleteDebt(1L);

        mockMvc.perform(delete("/api/debts/1")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDebtsReturns403WithoutAuthToken() throws Exception {
        mockMvc.perform(get("/api/debts"))
                .andExpect(status().isForbidden());
    }
}
