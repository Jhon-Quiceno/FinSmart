package com.smartfinance.backend.analisis.controller;

import com.smartfinance.backend.common.config.ClockConfig;
import com.smartfinance.backend.common.config.SecurityConfig;
import com.smartfinance.backend.analisis.model.dto.AnalysisSummaryResponse;
import com.smartfinance.backend.analisis.model.dto.PredictionResponse;
import com.smartfinance.backend.analisis.model.dto.RecommendationResponse;
import com.smartfinance.backend.analisis.model.dto.RecommendationType;
import com.smartfinance.backend.analisis.model.dto.Severity;
import com.smartfinance.backend.usuario.repository.UserRepository;
import com.smartfinance.backend.common.security.SecurityUtils;
import com.smartfinance.backend.analisis.service.FinancialAnalysisService;
import com.smartfinance.backend.common.security.JwtService;
import com.smartfinance.backend.analisis.service.MonthEndPredictionService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalysisController.class)
@Import({SecurityConfig.class, ClockConfig.class})
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FinancialAnalysisService financialAnalysisService;

    @MockitoBean
    private MonthEndPredictionService monthEndPredictionService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static final String AUTH_HEADER = "Bearer test-token";

    private final AnalysisSummaryResponse summaryResponse = new AnalysisSummaryResponse(
            2026, 6,
            BigDecimal.valueOf(3000), BigDecimal.valueOf(1800), BigDecimal.valueOf(1200),
            new BigDecimal("0.6000"), false,
            BigDecimal.valueOf(500), new BigDecimal("0.1667"), BigDecimal.valueOf(14400),
            List.of(), List.of(), List.of()
    );

    private final List<RecommendationResponse> recommendationsResponse = List.of(
            new RecommendationResponse(RecommendationType.INFO, Severity.LOW, null, "Tus finanzas están saludables este mes. ¡Sigue así!")
    );

    @BeforeEach
    void setUp() {
        Claims mockClaims = org.mockito.Mockito.mock(Claims.class);
        when(mockClaims.getSubject()).thenReturn("1");
        when(jwtService.parseAccessToken(any())).thenReturn(mockClaims);
        when(userRepository.existsById(1L)).thenReturn(true);
    }

    @Test
    void getSummaryReturns200WithDefaultPeriodWhenNoParamsProvided() throws Exception {
        when(financialAnalysisService.getSummary(isNull(), isNull())).thenReturn(summaryResponse);

        mockMvc.perform(get("/api/analysis/summary").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodYear").value(2026))
                .andExpect(jsonPath("$.periodMonth").value(6))
                .andExpect(jsonPath("$.totalIncome").value(3000))
                .andExpect(jsonPath("$.expenseAlert").value(false));
    }

    @Test
    void getSummaryReturns200WithGivenYearAndMonth() throws Exception {
        when(financialAnalysisService.getSummary(eq(2026), eq(3))).thenReturn(summaryResponse);

        mockMvc.perform(get("/api/analysis/summary?year=2026&month=3").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodYear").value(2026));
    }

    @Test
    void getSummaryReturns403WithoutAuthToken() throws Exception {
        mockMvc.perform(get("/api/analysis/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getSummaryReturns400WhenMonthIsOutOfRange() throws Exception {
        mockMvc.perform(get("/api/analysis/summary?year=2026&month=13").header("Authorization", AUTH_HEADER))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSummaryReturns400WhenYearIsOutOfRange() throws Exception {
        mockMvc.perform(get("/api/analysis/summary?year=1999&month=6").header("Authorization", AUTH_HEADER))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRecommendationsReturns200WithRuleResults() throws Exception {
        when(financialAnalysisService.getRecommendations(isNull(), isNull())).thenReturn(recommendationsResponse);

        mockMvc.perform(get("/api/analysis/recommendations").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("INFO"));
    }

    @Test
    void getRecommendationsReturns403WithoutAuthToken() throws Exception {
        mockMvc.perform(get("/api/analysis/recommendations"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRecommendationsReturns400WhenMonthIsOutOfRange() throws Exception {
        mockMvc.perform(get("/api/analysis/recommendations?year=2026&month=0").header("Authorization", AUTH_HEADER))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPredictionReturns200WithProjectionForCurrentUser() throws Exception {
        PredictionResponse prediction = new PredictionResponse(
                new BigDecimal("3100.00"), new BigDecimal("900.00"), new BigDecimal("100.00"),
                30, new BigDecimal("130.00"), false
        );
        when(monthEndPredictionService.computePrediction(eq(1L), any(LocalDate.class))).thenReturn(prediction);

        mockMvc.perform(get("/api/analysis/prediction").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectedExpense").value(3100.00))
                .andExpect(jsonPath("$.projectedBalance").value(900.00))
                .andExpect(jsonPath("$.avgDailySpend").value(100.00))
                .andExpect(jsonPath("$.daysRemaining").value(30))
                .andExpect(jsonPath("$.recommendedDailyMax").value(130.00))
                .andExpect(jsonPath("$.alert").value(false));
    }

    @Test
    void getPredictionReturns403WithoutAuthToken() throws Exception {
        mockMvc.perform(get("/api/analysis/prediction"))
                .andExpect(status().isForbidden());
    }
}
