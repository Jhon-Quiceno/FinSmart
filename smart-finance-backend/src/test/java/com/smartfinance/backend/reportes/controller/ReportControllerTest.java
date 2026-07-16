package com.smartfinance.backend.reportes.controller;

import com.smartfinance.backend.common.config.SecurityConfig;
import com.smartfinance.backend.reportes.model.dto.MonthlyReportResponse;
import com.smartfinance.backend.reportes.model.dto.ReportMovementRow;
import com.smartfinance.backend.gastos.model.entity.PaymentMethodType;
import com.smartfinance.backend.usuario.repository.UserRepository;
import com.smartfinance.backend.common.security.JwtService;
import com.smartfinance.backend.reportes.service.ReportService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@Import(SecurityConfig.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static final String AUTH_HEADER = "Bearer test-token";

    private final MonthlyReportResponse monthlyReport = new MonthlyReportResponse(
            2026, 6,
            BigDecimal.valueOf(3000), BigDecimal.valueOf(1800), BigDecimal.valueOf(1200),
            new BigDecimal("0.6000"), new BigDecimal("0.1667"),
            List.of(), List.of()
    );

    private final List<ReportMovementRow> movements = List.of(
            new ReportMovementRow(
                    LocalDate.of(2026, 6, 15), "INCOME", null, "Sueldo",
                    BigDecimal.valueOf(1500), null
            ),
            new ReportMovementRow(
                    LocalDate.of(2026, 6, 10), "EXPENSE", "Comida", "Supermercado, verduras",
                    BigDecimal.valueOf(50), PaymentMethodType.DEBIT_CARD
            )
    );

    @BeforeEach
    void setUp() {
        Claims mockClaims = org.mockito.Mockito.mock(Claims.class);
        when(mockClaims.getSubject()).thenReturn("1");
        when(jwtService.parseAccessToken(any())).thenReturn(mockClaims);
        when(userRepository.existsById(1L)).thenReturn(true);
    }

    @Test
    void getMonthlyReportReturns200WithDefaultPeriodWhenNoParamsProvided() throws Exception {
        when(reportService.getMonthlyReport(isNull(), isNull())).thenReturn(monthlyReport);

        mockMvc.perform(get("/api/reports/monthly").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodYear").value(2026))
                .andExpect(jsonPath("$.periodMonth").value(6))
                .andExpect(jsonPath("$.totalIncome").value(3000))
                .andExpect(jsonPath("$.totalExpense").value(1800))
                .andExpect(jsonPath("$.savings").value(1200));
    }

    @Test
    void getMonthlyReportReturns200WithGivenYearAndMonth() throws Exception {
        when(reportService.getMonthlyReport(eq(2026), eq(3))).thenReturn(monthlyReport);

        mockMvc.perform(get("/api/reports/monthly?year=2026&month=3").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodYear").value(2026));
    }

    @Test
    void getMonthlyReportReturns400WhenMonthIsOutOfRange() throws Exception {
        mockMvc.perform(get("/api/reports/monthly?year=2026&month=13").header("Authorization", AUTH_HEADER))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMonthlyReportReturns400WhenYearIsOutOfRange() throws Exception {
        mockMvc.perform(get("/api/reports/monthly?year=1999&month=6").header("Authorization", AUTH_HEADER))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMonthlyReportReturns403WithoutAuthToken() throws Exception {
        mockMvc.perform(get("/api/reports/monthly"))
                .andExpect(status().isForbidden());
    }

    @Test
    void exportReturnsCsvWithHeaderRowsAndAttachmentDisposition() throws Exception {
        when(reportService.getMovements(eq(2026), eq(6))).thenReturn(movements);

        mockMvc.perform(get("/api/reports/export?year=2026&month=6&format=csv").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"korofin-2026-06.csv\""))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Fecha,Tipo,Categoria,Descripcion,Monto,Metodo de pago")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ingreso")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Gasto")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("1500.00")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("50.00")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tarjeta de Debito")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"Supermercado, verduras\"")));
    }

    @Test
    void exportEscapesLeadingFormulaCharactersToPreventCsvInjection() throws Exception {
        List<ReportMovementRow> movementsWithFormulaLikeCategory = List.of(
                new ReportMovementRow(
                        LocalDate.of(2026, 6, 10), "EXPENSE", "=cmd|'/c calc'!A1", "Gasto sospechoso",
                        BigDecimal.valueOf(50), PaymentMethodType.DEBIT_CARD
                )
        );
        when(reportService.getMovements(eq(2026), eq(6))).thenReturn(movementsWithFormulaLikeCategory);

        mockMvc.perform(get("/api/reports/export?year=2026&month=6&format=csv").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("'=cmd|'/c calc'!A1")));
    }

    @Test
    void exportDefaultsToCsvWhenFormatParamIsOmitted() throws Exception {
        when(reportService.getMovements(eq(2026), eq(6))).thenReturn(movements);

        mockMvc.perform(get("/api/reports/export?year=2026&month=6").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"));
    }

    @Test
    void exportReturnsJsonWithMovementFieldsAndAttachmentDisposition() throws Exception {
        when(reportService.getMovements(eq(2026), eq(6))).thenReturn(movements);

        mockMvc.perform(get("/api/reports/export?year=2026&month=6&format=json").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(header().stringValues("Content-Type", org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.startsWith("application/json"))))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"korofin-2026-06.json\""))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("INCOME"))
                .andExpect(jsonPath("$[1].categoryName").value("Comida"))
                .andExpect(jsonPath("$[1].paymentMethod").value("DEBIT_CARD"));
    }

    @Test
    void exportReturns403WithoutAuthToken() throws Exception {
        mockMvc.perform(get("/api/reports/export"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMovementsReturns200WithMovementsForGivenYearAndMonth() throws Exception {
        when(reportService.getMovements(eq(2026), eq(6))).thenReturn(movements);

        mockMvc.perform(get("/api/reports/movements?year=2026&month=6").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("INCOME"))
                .andExpect(jsonPath("$[1].categoryName").value("Comida"))
                .andExpect(jsonPath("$[1].paymentMethod").value("DEBIT_CARD"));
    }

    @Test
    void getMovementsReturns200WithDefaultPeriodWhenNoParamsProvided() throws Exception {
        when(reportService.getMovements(isNull(), isNull())).thenReturn(movements);

        mockMvc.perform(get("/api/reports/movements").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getMovementsReturns400WhenMonthIsOutOfRange() throws Exception {
        mockMvc.perform(get("/api/reports/movements?year=2026&month=13").header("Authorization", AUTH_HEADER))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMovementsReturns400WhenYearIsOutOfRange() throws Exception {
        mockMvc.perform(get("/api/reports/movements?year=1999&month=6").header("Authorization", AUTH_HEADER))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMovementsReturns403WithoutAuthToken() throws Exception {
        mockMvc.perform(get("/api/reports/movements"))
                .andExpect(status().isForbidden());
    }
}
