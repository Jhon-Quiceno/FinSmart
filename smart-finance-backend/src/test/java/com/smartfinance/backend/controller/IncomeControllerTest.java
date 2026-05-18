package com.smartfinance.backend.controller;

import com.smartfinance.backend.config.SecurityConfig;
import com.smartfinance.backend.dto.income.IncomeRequest;
import com.smartfinance.backend.dto.income.IncomeResponse;
import com.smartfinance.backend.repository.UserRepository;
import com.smartfinance.backend.service.IncomeService;
import com.smartfinance.backend.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IncomeController.class)
@Import(SecurityConfig.class)
class IncomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean
    private IncomeService incomeService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static final String AUTH_HEADER = "Bearer test-token";

    private final IncomeResponse incomeResponse = new IncomeResponse(
            1L, 1L, new BigDecimal("5000.00"), "Salario mensual",
            LocalDate.of(2026, 3, 15), false, "Empresa ACME",
            Instant.now(), Instant.now()
    );

    @BeforeEach
    void setUp() {
        Claims mockClaims = org.mockito.Mockito.mock(Claims.class);
        when(mockClaims.getSubject()).thenReturn("1");
        when(jwtService.parseAccessToken(any())).thenReturn(mockClaims);
        when(userRepository.existsById(1L)).thenReturn(true);
    }

    private Page<IncomeResponse> buildPage(List<IncomeResponse> content) {
        return new PageImpl<>(content, PageRequest.of(0, 10), content.size());
    }

    @Test
    void getIncomesReturns200WithPaginatedIncomes() throws Exception {
        when(incomeService.getIncomes(isNull(), isNull(), isNull(), any()))
                .thenReturn(buildPage(List.of(incomeResponse)));

        mockMvc.perform(get("/api/incomes").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Salario mensual"));
    }

    @Test
    void getIncomesWithMonthYearFilterReturns200() throws Exception {
        when(incomeService.getIncomes(eq(3), eq(2026), isNull(), any()))
                .thenReturn(buildPage(List.of(incomeResponse)));

        mockMvc.perform(get("/api/incomes?month=3&year=2026").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].date").value("2026-03-15"));
    }

    @Test
    void getIncomesWithPaginationReturnsPaginatedResponse() throws Exception {
        when(incomeService.getIncomes(isNull(), isNull(), isNull(), any()))
                .thenReturn(buildPage(List.of(incomeResponse)));

        mockMvc.perform(get("/api/incomes?page=0&size=10").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    void createIncomeReturns201WhenValid() throws Exception {
        IncomeRequest request = new IncomeRequest(
                1L, new BigDecimal("3000.00"), "Bonus", LocalDate.of(2026, 3, 20), true, "Empresa"
        );
        IncomeResponse response = new IncomeResponse(
                2L, 1L, new BigDecimal("3000.00"), "Bonus",
                LocalDate.of(2026, 3, 20), true, "Empresa",
                Instant.now(), Instant.now()
        );
        when(incomeService.createIncome(any(IncomeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/incomes")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.amount").value(3000.00));
    }

    @Test
    void createIncomeReturns400WhenAmountIsNull() throws Exception {
        IncomeRequest request = new IncomeRequest(
                1L, null, "Sin monto", LocalDate.of(2026, 3, 20), false, "N/A"
        );

        mockMvc.perform(post("/api/incomes")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createIncomeReturns400WhenDateIsNull() throws Exception {
        IncomeRequest request = new IncomeRequest(
                1L, new BigDecimal("1000.00"), "Sin fecha", null, false, "N/A"
        );

        mockMvc.perform(post("/api/incomes")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateIncomeReturns200WhenUpdatingOwnIncome() throws Exception {
        IncomeRequest request = new IncomeRequest(
                1L, new BigDecimal("5500.00"), "Salario actualizado", LocalDate.of(2026, 3, 15), false, "Empresa ACME"
        );
        IncomeResponse response = new IncomeResponse(
                1L, 1L, new BigDecimal("5500.00"), "Salario actualizado",
                LocalDate.of(2026, 3, 15), false, "Empresa ACME",
                Instant.now(), Instant.now()
        );
        when(incomeService.updateIncome(eq(1L), any(IncomeRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/incomes/1")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(5500.00));
    }

    @Test
    void updateIncomeReturns403WhenUpdatingAnotherUsersIncome() throws Exception {
        IncomeRequest request = new IncomeRequest(
                1L, new BigDecimal("100.00"), "Hack", LocalDate.of(2026, 3, 15), false, "N/A"
        );
        when(incomeService.updateIncome(eq(99L), any(IncomeRequest.class)))
                .thenThrow(new AccessDeniedException("No tienes permisos sobre este ingreso"));

        mockMvc.perform(put("/api/incomes/99")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteIncomeReturns204WhenDeletingOwnIncome() throws Exception {
        mockMvc.perform(delete("/api/incomes/1")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteIncomeReturns403WhenDeletingAnotherUsersIncome() throws Exception {
        org.mockito.Mockito.doThrow(new AccessDeniedException("No tienes permisos sobre este ingreso"))
                .when(incomeService).deleteIncome(99L);

        mockMvc.perform(delete("/api/incomes/99")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getIncomesReturns401WithoutAuthToken() throws Exception {
        mockMvc.perform(get("/api/incomes"))
                .andExpect(status().isForbidden());
    }

    private static <T> T isNull() {
        return org.mockito.ArgumentMatchers.isNull();
    }
}
