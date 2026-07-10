package com.smartfinance.backend.servicios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartfinance.backend.common.config.SecurityConfig;
import com.smartfinance.backend.servicios.model.dto.RecurringPaymentPayResponse;
import com.smartfinance.backend.servicios.model.dto.RecurringPaymentRequest;
import com.smartfinance.backend.servicios.model.dto.RecurringPaymentResponse;
import com.smartfinance.backend.servicios.model.dto.RecurringPaymentUpdateRequest;
import com.smartfinance.backend.servicios.exception.RecurringPaymentAlreadyPaidException;
import com.smartfinance.backend.common.exception.ResourceNotFoundException;
import com.smartfinance.backend.servicios.model.entity.RecurringFrequency;
import com.smartfinance.backend.usuario.repository.UserRepository;
import com.smartfinance.backend.common.security.JwtService;
import com.smartfinance.backend.servicios.service.RecurringPaymentService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecurringPaymentController.class)
@Import(SecurityConfig.class)
class RecurringPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private RecurringPaymentService recurringPaymentService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static final String AUTH_HEADER = "Bearer test-token";

    private final RecurringPaymentResponse netflix = new RecurringPaymentResponse(
            1L, "Netflix", BigDecimal.valueOf(15), RecurringFrequency.MONTHLY,
            LocalDate.of(2026, 7, 10), true, null, null
    );

    @BeforeEach
    void setUp() {
        Claims mockClaims = org.mockito.Mockito.mock(Claims.class);
        when(mockClaims.getSubject()).thenReturn("1");
        when(jwtService.parseAccessToken(any())).thenReturn(mockClaims);
        when(userRepository.existsById(1L)).thenReturn(true);
    }

    @Test
    void getRecurringPaymentsReturns200WithPagedResults() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        when(recurringPaymentService.getRecurringPayments(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(netflix), pageable, 1));

        mockMvc.perform(get("/api/recurring").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].isActive").value(true))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void createRecurringPaymentReturns201WhenValid() throws Exception {
        RecurringPaymentRequest request = new RecurringPaymentRequest(
                "Netflix", BigDecimal.valueOf(15), RecurringFrequency.MONTHLY, LocalDate.of(2026, 7, 10)
        );
        when(recurringPaymentService.createRecurringPayment(any(RecurringPaymentRequest.class))).thenReturn(netflix);

        mockMvc.perform(post("/api/recurring")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nextPaymentDate").value("2026-07-10"));
    }

    @Test
    void createRecurringPaymentReturns400WhenAmountIsZero() throws Exception {
        RecurringPaymentRequest request = new RecurringPaymentRequest(
                "Netflix", BigDecimal.ZERO, RecurringFrequency.MONTHLY, LocalDate.of(2026, 7, 10)
        );

        mockMvc.perform(post("/api/recurring")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRecurringPaymentReturns400WhenFrequencyIsMissing() throws Exception {
        String invalidBody = """
                {"name": "Netflix", "amount": 15, "firstPaymentDate": "2026-07-10"}
                """;

        mockMvc.perform(post("/api/recurring")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateRecurringPaymentReturns200WhenUpdatingOwnRecord() throws Exception {
        RecurringPaymentUpdateRequest request = new RecurringPaymentUpdateRequest("Netflix Premium", BigDecimal.valueOf(20), RecurringFrequency.MONTHLY);
        when(recurringPaymentService.updateRecurringPayment(eq(1L), any(RecurringPaymentUpdateRequest.class))).thenReturn(netflix);

        mockMvc.perform(put("/api/recurring/1")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void updateRecurringPaymentReturns404WhenUpdatingAnotherUsersRecord() throws Exception {
        RecurringPaymentUpdateRequest request = new RecurringPaymentUpdateRequest("Otro nombre", BigDecimal.TEN, RecurringFrequency.WEEKLY);
        when(recurringPaymentService.updateRecurringPayment(eq(99L), any(RecurringPaymentUpdateRequest.class)))
                .thenThrow(new ResourceNotFoundException("Pago recurrente no encontrado"));

        mockMvc.perform(put("/api/recurring/99")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRecurringPaymentReturns204WhenDeletingOwnRecord() throws Exception {
        mockMvc.perform(delete("/api/recurring/1")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteRecurringPaymentReturns404WhenDeletingAnotherUsersRecord() throws Exception {
        doThrow(new ResourceNotFoundException("Pago recurrente no encontrado"))
                .when(recurringPaymentService).deleteRecurringPayment(1L);

        mockMvc.perform(delete("/api/recurring/1")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void toggleRecurringPaymentReturns200WithFlippedActiveFlag() throws Exception {
        RecurringPaymentResponse toggled = new RecurringPaymentResponse(
                1L, "Netflix", BigDecimal.valueOf(15), RecurringFrequency.MONTHLY,
                LocalDate.of(2026, 7, 10), false, null, null
        );
        when(recurringPaymentService.toggleRecurringPayment(1L)).thenReturn(toggled);

        mockMvc.perform(patch("/api/recurring/1/toggle")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void toggleRecurringPaymentReturns404WhenTogglingAnotherUsersRecord() throws Exception {
        when(recurringPaymentService.toggleRecurringPayment(99L))
                .thenThrow(new ResourceNotFoundException("Pago recurrente no encontrado"));

        mockMvc.perform(patch("/api/recurring/99/toggle")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void payRecurringPaymentReturns200WithUpdatedRecurringPaymentAndExpenseId() throws Exception {
        RecurringPaymentResponse advanced = new RecurringPaymentResponse(
                1L, "Netflix", BigDecimal.valueOf(15), RecurringFrequency.MONTHLY,
                LocalDate.of(2026, 8, 10), true, null, null
        );
        RecurringPaymentPayResponse payResponse = new RecurringPaymentPayResponse(advanced, 77L);
        when(recurringPaymentService.payRecurringPayment(1L)).thenReturn(payResponse);

        mockMvc.perform(patch("/api/recurring/1/pay")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expenseId").value(77L))
                .andExpect(jsonPath("$.recurringPayment.nextPaymentDate").value("2026-08-10"));
    }

    @Test
    void payRecurringPaymentReturns404WhenPayingAnotherUsersRecord() throws Exception {
        when(recurringPaymentService.payRecurringPayment(99L))
                .thenThrow(new ResourceNotFoundException("Pago recurrente no encontrado"));

        mockMvc.perform(patch("/api/recurring/99/pay")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void payRecurringPaymentReturns409WhenAlreadyPaidConcurrently() throws Exception {
        when(recurringPaymentService.payRecurringPayment(1L))
                .thenThrow(new RecurringPaymentAlreadyPaidException("Este servicio ya fue marcado como pagado"));

        mockMvc.perform(patch("/api/recurring/1/pay")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void getRecurringPaymentsReturns403WithoutAuthToken() throws Exception {
        mockMvc.perform(get("/api/recurring"))
                .andExpect(status().isForbidden());
    }
}
