package com.smartfinance.backend.tarjetas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartfinance.backend.common.config.SecurityConfig;
import com.smartfinance.backend.common.exception.ResourceNotFoundException;
import com.smartfinance.backend.common.security.JwtService;
import com.smartfinance.backend.tarjetas.model.dto.CreditCardRequest;
import com.smartfinance.backend.tarjetas.model.dto.CreditCardResponse;
import com.smartfinance.backend.tarjetas.model.dto.CreditCardUpdateRequest;
import com.smartfinance.backend.tarjetas.model.entity.CardFranchise;
import com.smartfinance.backend.tarjetas.service.CreditCardService;
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

@WebMvcTest(CreditCardController.class)
@Import(SecurityConfig.class)
class CreditCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private CreditCardService creditCardService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static final String AUTH_HEADER = "Bearer test-token";

    private final CreditCardResponse card = new CreditCardResponse(
            1L, "Tarjeta Visa", "Bancolombia", CardFranchise.VISA,
            BigDecimal.valueOf(1_000_000), BigDecimal.valueOf(0.025), 5, 20,
            BigDecimal.ZERO, BigDecimal.valueOf(1_000_000), null, null, null
    );

    @BeforeEach
    void setUp() {
        Claims mockClaims = org.mockito.Mockito.mock(Claims.class);
        when(mockClaims.getSubject()).thenReturn("1");
        when(jwtService.parseAccessToken(any())).thenReturn(mockClaims);
        when(userRepository.existsById(1L)).thenReturn(true);
    }

    @Test
    void getCardsReturns200WithPagedResults() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        when(creditCardService.getCards(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(card), pageable, 1));

        mockMvc.perform(get("/api/cards").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Tarjeta Visa"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getCardReturns200WhenOwnedByCurrentUser() throws Exception {
        when(creditCardService.getCard(1L)).thenReturn(card);

        mockMvc.perform(get("/api/cards/1").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.availableCredit").value(1_000_000));
    }

    @Test
    void getCardReturns404WhenCardBelongsToAnotherUser() throws Exception {
        when(creditCardService.getCard(99L)).thenThrow(new ResourceNotFoundException("Tarjeta no encontrada"));

        mockMvc.perform(get("/api/cards/99").header("Authorization", AUTH_HEADER))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCardReturns201WhenValid() throws Exception {
        CreditCardRequest request = new CreditCardRequest(
                "Tarjeta Visa", "Bancolombia", CardFranchise.VISA,
                BigDecimal.valueOf(1_000_000), BigDecimal.valueOf(0.025), 5, 20
        );
        when(creditCardService.createCard(any(CreditCardRequest.class))).thenReturn(card);

        mockMvc.perform(post("/api/cards")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.currentBalance").value(0));
    }

    @Test
    void createCardReturns400WhenCreditLimitIsZero() throws Exception {
        CreditCardRequest request = new CreditCardRequest(
                "Tarjeta Visa", "Bancolombia", CardFranchise.VISA,
                BigDecimal.ZERO, BigDecimal.valueOf(0.025), 5, 20
        );

        mockMvc.perform(post("/api/cards")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCardReturns400WhenMonthlyRateIsNegative() throws Exception {
        CreditCardRequest request = new CreditCardRequest(
                "Tarjeta Visa", "Bancolombia", CardFranchise.VISA,
                BigDecimal.valueOf(1_000_000), BigDecimal.valueOf(-1), 5, 20
        );

        mockMvc.perform(post("/api/cards")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCardReturns400WhenCutoffDayIsOutOfRange() throws Exception {
        CreditCardRequest request = new CreditCardRequest(
                "Tarjeta Visa", "Bancolombia", CardFranchise.VISA,
                BigDecimal.valueOf(1_000_000), BigDecimal.valueOf(0.025), 32, 20
        );

        mockMvc.perform(post("/api/cards")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCardReturns400WhenNameIsMissing() throws Exception {
        String invalidBody = """
                {"franchise": "VISA", "creditLimit": 1000000, "monthlyRate": 0.025, "cutoffDay": 5, "paymentDueDay": 20}
                """;

        mockMvc.perform(post("/api/cards")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCardReturns200WhenUpdatingOwnCard() throws Exception {
        CreditCardUpdateRequest request = new CreditCardUpdateRequest("Tarjeta Visa Gold", "Davivienda", BigDecimal.valueOf(0.03), 10, 25);
        when(creditCardService.updateCard(eq(1L), any(CreditCardUpdateRequest.class))).thenReturn(card);

        mockMvc.perform(put("/api/cards/1")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void updateCardReturns404WhenUpdatingAnotherUsersCard() throws Exception {
        CreditCardUpdateRequest request = new CreditCardUpdateRequest("Otro nombre", null, BigDecimal.valueOf(0.02), 5, 20);
        when(creditCardService.updateCard(eq(99L), any(CreditCardUpdateRequest.class)))
                .thenThrow(new ResourceNotFoundException("Tarjeta no encontrada"));

        mockMvc.perform(put("/api/cards/99")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCardReturns204WhenDeletingOwnCard() throws Exception {
        mockMvc.perform(delete("/api/cards/1")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCardReturns404WhenDeletingAnotherUsersCard() throws Exception {
        doThrow(new ResourceNotFoundException("Tarjeta no encontrada"))
                .when(creditCardService).deleteCard(1L);

        mockMvc.perform(delete("/api/cards/1")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCardsReturns403WithoutAuthToken() throws Exception {
        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isForbidden());
    }
}
