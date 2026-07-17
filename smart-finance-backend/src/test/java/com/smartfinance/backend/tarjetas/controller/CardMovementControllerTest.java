package com.smartfinance.backend.tarjetas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartfinance.backend.common.config.SecurityConfig;
import com.smartfinance.backend.common.exception.ResourceNotFoundException;
import com.smartfinance.backend.common.security.JwtService;
import com.smartfinance.backend.tarjetas.model.dto.CardMovementResponse;
import com.smartfinance.backend.tarjetas.model.dto.CardPaymentRequest;
import com.smartfinance.backend.tarjetas.model.dto.CardPurchaseRequest;
import com.smartfinance.backend.tarjetas.model.entity.CardMovementType;
import com.smartfinance.backend.tarjetas.service.CardMovementService;
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

@WebMvcTest(CardMovementController.class)
@Import(SecurityConfig.class)
class CardMovementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private CardMovementService cardMovementService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static final String AUTH_HEADER = "Bearer test-token";

    private final CardMovementResponse purchase = new CardMovementResponse(
            5L, 10L, CardMovementType.PURCHASE, BigDecimal.valueOf(50_000), LocalDate.of(2026, 6, 1),
            "Mercado", BigDecimal.valueOf(250_000), 77L, null, null
    );

    private final CardMovementResponse payment = new CardMovementResponse(
            9L, 10L, CardMovementType.PAYMENT, BigDecimal.valueOf(80_000), LocalDate.of(2026, 6, 1),
            "Pago mensual", BigDecimal.valueOf(120_000), null, null, null
    );

    @BeforeEach
    void setUp() {
        Claims mockClaims = org.mockito.Mockito.mock(Claims.class);
        when(mockClaims.getSubject()).thenReturn("1");
        when(jwtService.parseAccessToken(any())).thenReturn(mockClaims);
        when(userRepository.existsById(1L)).thenReturn(true);
    }

    @Test
    void registerPurchaseReturns201WhenValid() throws Exception {
        CardPurchaseRequest request = new CardPurchaseRequest(BigDecimal.valueOf(50_000), LocalDate.of(2026, 6, 1), "Mercado", null);
        when(cardMovementService.registerPurchase(eq(10L), any(CardPurchaseRequest.class))).thenReturn(purchase);

        mockMvc.perform(post("/api/cards/10/purchases")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.expenseId").value(77L))
                .andExpect(jsonPath("$.cardBalanceAfter").value(250_000));
    }

    @Test
    void registerPurchaseReturns400WhenAmountIsZero() throws Exception {
        CardPurchaseRequest request = new CardPurchaseRequest(BigDecimal.ZERO, null, null, null);

        mockMvc.perform(post("/api/cards/10/purchases")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerPurchaseReturns400WhenOverLimit() throws Exception {
        CardPurchaseRequest request = new CardPurchaseRequest(BigDecimal.valueOf(999_999), null, null, null);
        when(cardMovementService.registerPurchase(eq(10L), any(CardPurchaseRequest.class)))
                .thenThrow(new IllegalArgumentException("La compra supera el cupo disponible de la tarjeta"));

        mockMvc.perform(post("/api/cards/10/purchases")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerPurchaseReturns400WhenInstallmentsRequested() throws Exception {
        CardPurchaseRequest request = new CardPurchaseRequest(BigDecimal.valueOf(700_000), null, null, 3);
        when(cardMovementService.registerPurchase(eq(10L), any(CardPurchaseRequest.class)))
                .thenThrow(new IllegalArgumentException("Las compras a cuotas todavía no están disponibles"));

        mockMvc.perform(post("/api/cards/10/purchases")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerPurchaseReturns404WhenCardBelongsToAnotherUser() throws Exception {
        CardPurchaseRequest request = new CardPurchaseRequest(BigDecimal.valueOf(50), null, null, null);
        when(cardMovementService.registerPurchase(eq(99L), any(CardPurchaseRequest.class)))
                .thenThrow(new ResourceNotFoundException("Tarjeta no encontrada"));

        mockMvc.perform(post("/api/cards/99/purchases")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void registerPaymentReturns201WhenValid() throws Exception {
        CardPaymentRequest request = new CardPaymentRequest(BigDecimal.valueOf(80_000), LocalDate.of(2026, 6, 1), "Pago mensual");
        when(cardMovementService.registerPayment(eq(10L), any(CardPaymentRequest.class))).thenReturn(payment);

        mockMvc.perform(post("/api/cards/10/payments")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9L))
                .andExpect(jsonPath("$.expenseId").doesNotExist())
                .andExpect(jsonPath("$.cardBalanceAfter").value(120_000));
    }

    @Test
    void registerPaymentReturns400WhenAmountExceedsCurrentBalance() throws Exception {
        CardPaymentRequest request = new CardPaymentRequest(BigDecimal.valueOf(999_999), null, null);
        when(cardMovementService.registerPayment(eq(10L), any(CardPaymentRequest.class)))
                .thenThrow(new IllegalArgumentException("El pago no puede superar el saldo actual de la tarjeta"));

        mockMvc.perform(post("/api/cards/10/payments")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerPaymentReturns404WhenCardBelongsToAnotherUser() throws Exception {
        CardPaymentRequest request = new CardPaymentRequest(BigDecimal.valueOf(50), null, null);
        when(cardMovementService.registerPayment(eq(99L), any(CardPaymentRequest.class)))
                .thenThrow(new ResourceNotFoundException("Tarjeta no encontrada"));

        mockMvc.perform(post("/api/cards/99/payments")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMovementsReturns200WithPagedResults() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        when(cardMovementService.getMovements(eq(10L), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(purchase), pageable, 1));

        mockMvc.perform(get("/api/cards/10/movements").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getMovementsReturns200FilteredByType() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        when(cardMovementService.getMovements(eq(10L), eq(CardMovementType.PAYMENT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(payment), pageable, 1));

        mockMvc.perform(get("/api/cards/10/movements").param("type", "PAYMENT").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("PAYMENT"));
    }

    @Test
    void getMovementsReturns404WhenCardBelongsToAnotherUser() throws Exception {
        when(cardMovementService.getMovements(eq(99L), eq(null), any(Pageable.class)))
                .thenThrow(new ResourceNotFoundException("Tarjeta no encontrada"));

        mockMvc.perform(get("/api/cards/99/movements").header("Authorization", AUTH_HEADER))
                .andExpect(status().isNotFound());
    }

    @Test
    void registerPurchaseReturns403WithoutAuthToken() throws Exception {
        CardPurchaseRequest request = new CardPurchaseRequest(BigDecimal.valueOf(50_000), null, null, null);

        mockMvc.perform(post("/api/cards/10/purchases")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
