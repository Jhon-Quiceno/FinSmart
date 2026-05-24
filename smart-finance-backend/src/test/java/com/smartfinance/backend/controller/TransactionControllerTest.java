package com.smartfinance.backend.controller;

import com.smartfinance.backend.config.SecurityConfig;
import com.smartfinance.backend.dto.transaction.TransactionRequest;
import com.smartfinance.backend.dto.transaction.TransactionResponse;
import com.smartfinance.backend.model.PaymentMethodType;
import com.smartfinance.backend.model.TransactionType;
import com.smartfinance.backend.repository.UserRepository;
import com.smartfinance.backend.service.JwtService;
import com.smartfinance.backend.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

@WebMvcTest(TransactionController.class)
@Import(SecurityConfig.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static final String AUTH_HEADER = "Bearer test-token";

    private final TransactionResponse transactionResponse = new TransactionResponse(
            1L,
            2L,
            "Efectivo",
            4L,
            "Renta",
            TransactionType.EXPENSE,
            new BigDecimal("2500.00"),
            "Renta mensual",
            LocalDate.of(2026, 3, 1),
            PaymentMethodType.TRANSFER,
            null,
            Instant.now(),
            Instant.now()
    );

    @BeforeEach
    void setUp() {
        Claims mockClaims = org.mockito.Mockito.mock(Claims.class);
        when(mockClaims.getSubject()).thenReturn("1");
        when(jwtService.parseAccessToken(any())).thenReturn(mockClaims);
        when(userRepository.existsById(1L)).thenReturn(true);
    }

    private Page<TransactionResponse> buildPage(List<TransactionResponse> content) {
        return new PageImpl<>(content, PageRequest.of(0, 10), content.size());
    }

    @Test
    void getTransactionsReturns200WithPaginatedTransactions() throws Exception {
        when(transactionService.getTransactions(isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(buildPage(List.of(transactionResponse)));

        mockMvc.perform(get("/api/transactions").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Renta mensual"));
    }

    @Test
    void getTransactionsWithFiltersReturns200() throws Exception {
        when(transactionService.getTransactions(
                eq(TransactionType.EXPENSE),
                eq(4L),
                eq(2L),
                eq(LocalDate.of(2026, 3, 1)),
                eq(LocalDate.of(2026, 3, 31)),
                any()
        )).thenReturn(buildPage(List.of(transactionResponse)));

        mockMvc.perform(get("/api/transactions?type=EXPENSE&category=4&account=2&from=2026-03-01&to=2026-03-31")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].categoryId").value(4));
    }

    @Test
    void createTransactionReturns201WhenValid() throws Exception {
        TransactionRequest request = new TransactionRequest(
                2L,
                4L,
                TransactionType.EXPENSE,
                new BigDecimal("1500.00"),
                "Supermercado",
                LocalDate.of(2026, 3, 10),
                PaymentMethodType.CASH,
                "Compra del mes"
        );
        TransactionResponse response = new TransactionResponse(
                2L,
                2L,
                "Efectivo",
                4L,
                "Super Cat",
                TransactionType.EXPENSE,
                new BigDecimal("1500.00"),
                "Supermercado",
                LocalDate.of(2026, 3, 10),
                PaymentMethodType.CASH,
                "Compra del mes",
                Instant.now(),
                Instant.now()
        );
        when(transactionService.createTransaction(any(TransactionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.paymentMethod").value("CASH"));
    }

    @Test
    void createTransactionReturns400WhenAmountIsNull() throws Exception {
        TransactionRequest request = new TransactionRequest(
                2L,
                4L,
                TransactionType.EXPENSE,
                null,
                "Sin monto",
                LocalDate.of(2026, 3, 10),
                PaymentMethodType.CASH,
                null
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTransactionReturns400WhenDateIsNull() throws Exception {
        TransactionRequest request = new TransactionRequest(
                2L,
                4L,
                TransactionType.EXPENSE,
                new BigDecimal("500.00"),
                "Sin fecha",
                null,
                PaymentMethodType.CASH,
                null
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateTransactionReturns200WhenUpdatingOwnTransaction() throws Exception {
        TransactionRequest request = new TransactionRequest(
                2L,
                4L,
                TransactionType.EXPENSE,
                new BigDecimal("2600.00"),
                "Renta actualizada",
                LocalDate.of(2026, 3, 1),
                PaymentMethodType.TRANSFER,
                "Ajuste"
        );
        TransactionResponse response = new TransactionResponse(
                1L,
                2L,
                "Cuenta nómina",
                4L,
                "Renta Cat",
                TransactionType.EXPENSE,
                new BigDecimal("2600.00"),
                "Renta actualizada",
                LocalDate.of(2026, 3, 1),
                PaymentMethodType.TRANSFER,
                "Ajuste",
                Instant.now(),
                Instant.now()
        );
        when(transactionService.updateTransaction(eq(1L), any(TransactionRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/transactions/1")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(2600.00));
    }

    @Test
    void updateTransactionReturns403WhenUpdatingAnotherUsersTransaction() throws Exception {
        TransactionRequest request = new TransactionRequest(
                2L,
                4L,
                TransactionType.EXPENSE,
                new BigDecimal("100.00"),
                "Hack",
                LocalDate.of(2026, 3, 1),
                PaymentMethodType.CASH,
                null
        );
        when(transactionService.updateTransaction(eq(99L), any(TransactionRequest.class)))
                .thenThrow(new AccessDeniedException("No tienes permisos sobre esta transacción"));

        mockMvc.perform(put("/api/transactions/99")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteTransactionReturns204WhenDeletingOwnTransaction() throws Exception {
        mockMvc.perform(delete("/api/transactions/1")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTransactionReturns403WhenDeletingAnotherUsersTransaction() throws Exception {
        org.mockito.Mockito.doThrow(new AccessDeniedException("No tienes permisos sobre esta transacción"))
                .when(transactionService).deleteTransaction(99L);

        mockMvc.perform(delete("/api/transactions/99")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTransactionsReturns401WithoutAuthToken() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isForbidden());
    }

    private static <T> T isNull() {
        return org.mockito.ArgumentMatchers.isNull();
    }
}
