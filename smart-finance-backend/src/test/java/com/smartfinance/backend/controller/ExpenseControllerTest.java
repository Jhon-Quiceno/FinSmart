package com.smartfinance.backend.controller;

import com.smartfinance.backend.config.SecurityConfig;
import com.smartfinance.backend.dto.expense.ExpenseRequest;
import com.smartfinance.backend.dto.expense.ExpenseResponse;
import com.smartfinance.backend.repository.UserRepository;
import com.smartfinance.backend.service.ExpenseService;
import com.smartfinance.backend.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
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

@WebMvcTest(ExpenseController.class)
@Import(SecurityConfig.class)
class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean
    private ExpenseService expenseService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static final String AUTH_HEADER = "Bearer test-token";

    private final ExpenseResponse expenseResponse = new ExpenseResponse(
            1L, 4L, "Renta", new BigDecimal("2500.00"), "Renta mensual",
            LocalDate.of(2026, 3, 1), true, "TRANSFERENCIA",
            Instant.now(), Instant.now()
    );

    @BeforeEach
    void setUp() {
        Claims mockClaims = org.mockito.Mockito.mock(Claims.class);
        when(mockClaims.getSubject()).thenReturn("1");
        when(jwtService.parseAccessToken(any())).thenReturn(mockClaims);
        when(userRepository.existsById(1L)).thenReturn(true);
    }

    private Page<ExpenseResponse> buildPage(List<ExpenseResponse> content) {
        return new PageImpl<>(content, PageRequest.of(0, 10), content.size());
    }

    @Test
    void getExpensesReturns200WithPaginatedExpenses() throws Exception {
        when(expenseService.getExpenses(isNull(), isNull(), isNull(), any()))
                .thenReturn(buildPage(List.of(expenseResponse)));

        mockMvc.perform(get("/api/expenses").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Renta mensual"));
    }

    @Test
    void getExpensesWithFiltersReturns200() throws Exception {
        when(expenseService.getExpenses(eq(4L), eq(LocalDate.of(2026, 3, 1)), eq(LocalDate.of(2026, 3, 31)), any()))
                .thenReturn(buildPage(List.of(expenseResponse)));

        mockMvc.perform(get("/api/expenses?category=4&from=2026-03-01&to=2026-03-31")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].categoryId").value(4));
    }

    @Test
    void createExpenseReturns201WhenValid() throws Exception {
        ExpenseRequest request = new ExpenseRequest(
                4L, new BigDecimal("1500.00"), "Supermercado", LocalDate.of(2026, 3, 10), false, "EFECTIVO"
        );
        ExpenseResponse response = new ExpenseResponse(
                2L, 4L, "Super Cat", new BigDecimal("1500.00"), "Supermercado",
                LocalDate.of(2026, 3, 10), false, "EFECTIVO",
                Instant.now(), Instant.now()
        );
        when(expenseService.createExpense(any(ExpenseRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.paymentMethod").value("EFECTIVO"));
    }

    @Test
    void createExpenseReturns400WhenAmountIsNull() throws Exception {
        ExpenseRequest request = new ExpenseRequest(
                4L, null, "Sin monto", LocalDate.of(2026, 3, 10), false, "EFECTIVO"
        );

        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createExpenseReturns400WhenDateIsNull() throws Exception {
        ExpenseRequest request = new ExpenseRequest(
                4L, new BigDecimal("500.00"), "Sin fecha", null, false, "EFECTIVO"
        );

        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateExpenseReturns200WhenUpdatingOwnExpense() throws Exception {
        ExpenseRequest request = new ExpenseRequest(
                4L, new BigDecimal("2600.00"), "Renta actualizada", LocalDate.of(2026, 3, 1), true, "TRANSFERENCIA"
        );
        ExpenseResponse response = new ExpenseResponse(
                1L, 4L, "Renta Cat", new BigDecimal("2600.00"), "Renta actualizada",
                LocalDate.of(2026, 3, 1), true, "TRANSFERENCIA",
                Instant.now(), Instant.now()
        );
        when(expenseService.updateExpense(eq(1L), any(ExpenseRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/expenses/1")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(2600.00));
    }

    @Test
    void updateExpenseReturns403WhenUpdatingAnotherUsersExpense() throws Exception {
        ExpenseRequest request = new ExpenseRequest(
                4L, new BigDecimal("100.00"), "Hack", LocalDate.of(2026, 3, 1), false, "EFECTIVO"
        );
        when(expenseService.updateExpense(eq(99L), any(ExpenseRequest.class)))
                .thenThrow(new AccessDeniedException("No tienes permisos sobre este gasto"));

        mockMvc.perform(put("/api/expenses/99")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteExpenseReturns204WhenDeletingOwnExpense() throws Exception {
        mockMvc.perform(delete("/api/expenses/1")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteExpenseReturns403WhenDeletingAnotherUsersExpense() throws Exception {
        org.mockito.Mockito.doThrow(new AccessDeniedException("No tienes permisos sobre este gasto"))
                .when(expenseService).deleteExpense(99L);

        mockMvc.perform(delete("/api/expenses/99")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getExpensesReturns401WithoutAuthToken() throws Exception {
        mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isForbidden());
    }

    private static <T> T isNull() {
        return org.mockito.ArgumentMatchers.isNull();
    }
}
