package com.smartfinance.backend.controller;

import com.smartfinance.backend.dto.report.MonthlyReportResponse;
import com.smartfinance.backend.dto.report.ReportMovementRow;
import com.smartfinance.backend.model.PaymentMethodType;
import com.smartfinance.backend.service.ReportService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

/**
 * REST endpoints for the current user's monthly financial report and its CSV/JSON export.
 *
 * <p>{@code year}/{@code month} are optional on both endpoints and default to the current
 * calendar year/month when omitted, matching {@link AnalysisController}'s convention. Both are
 * bounded ({@code year} in {@code [2000, 2100]}, {@code month} in {@code [1, 12]}); out-of-range
 * values fail method-level validation and are mapped to a {@code 400} by
 * {@link com.smartfinance.backend.exception.GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/reports")
@Validated
public class ReportController {

    private static final String CSV_FORMAT = "csv";

    private final ReportService reportService;
    private final ObjectMapper objectMapper;

    public ReportController(ReportService reportService, ObjectMapper objectMapper) {
        this.reportService = reportService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/monthly")
    public ResponseEntity<MonthlyReportResponse> getMonthlyReport(
            @RequestParam(required = false) @Min(2000) @Max(2100) Integer year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer month
    ) {
        return ResponseEntity.ok(reportService.getMonthlyReport(year, month));
    }

    /**
     * Plain JSON read model of the current user's monthly movements, intended for an in-app
     * table (fetched via XHR/fetch and rendered on screen) rather than a file download — unlike
     * {@link #exportMonthlyReport}, this never sets a {@code Content-Disposition} header.
     *
     * @param year  requested year, or {@code null} to default to the current calendar year
     * @param month requested month (1-12), or {@code null} to default to the current month
     * @return the resolved period's movements with HTTP 200
     */
    @GetMapping("/movements")
    public ResponseEntity<List<ReportMovementRow>> getMovements(
            @RequestParam(required = false) @Min(2000) @Max(2100) Integer year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer month
    ) {
        return ResponseEntity.ok(reportService.getMovements(year, month));
    }

    /**
     * Streams the current user's monthly movements directly to the response as a downloadable
     * file, writing the CSV manually (no third-party CSV dependency) or delegating to the
     * autowired Jackson {@link ObjectMapper} for JSON.
     *
     * @param year     requested year, or {@code null} to default to the current calendar year
     * @param month    requested month (1-12), or {@code null} to default to the current month
     * @param format   {@code "csv"} (default) or {@code "json"}
     * @param response servlet response the export is written to
     * @throws IOException if writing to the response output stream fails
     */
    @GetMapping("/export")
    public void exportMonthlyReport(
            @RequestParam(required = false) @Min(2000) @Max(2100) Integer year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer month,
            @RequestParam(defaultValue = CSV_FORMAT) String format,
            HttpServletResponse response
    ) throws IOException {
        List<ReportMovementRow> movements = reportService.getMovements(year, month);
        YearMonth period = ReportService.resolvePeriod(year, month);

        if (CSV_FORMAT.equalsIgnoreCase(format)) {
            writeCsv(response, movements, period);
        } else {
            writeJson(response, movements, period);
        }
    }

    private void writeCsv(HttpServletResponse response, List<ReportMovementRow> movements, YearMonth period)
            throws IOException {
        response.setContentType("text/csv");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition(period, "csv"));

        try (PrintWriter writer = response.getWriter()) {
            writer.println("Fecha,Tipo,Categoria,Descripcion,Monto,Metodo de pago");
            for (ReportMovementRow row : movements) {
                writer.println(String.join(",",
                        row.date().toString(),
                        translateType(row.type()),
                        escapeCsv(row.categoryName()),
                        escapeCsv(row.description()),
                        String.format(Locale.ROOT, "%.2f", row.amount()),
                        translatePaymentMethod(row.paymentMethod())
                ));
            }
        }
    }

    /**
     * Translates the raw movement type to a human-readable Spanish label for the CSV file only.
     * The JSON contract ({@link #writeJson}, {@link #getMovements}) intentionally keeps the raw
     * {@code "INCOME"}/{@code "EXPENSE"} values, since the frontend already applies its own
     * label mapping for on-screen display.
     */
    private static String translateType(String type) {
        return "INCOME".equals(type) ? "Ingreso" : "EXPENSE".equals(type) ? "Gasto" : type;
    }

    /**
     * Translates a payment method to the same Spanish labels used on the frontend (see
     * {@code expenses-table.tsx}) for the CSV file only. The JSON contract keeps the raw enum
     * name.
     */
    private static String translatePaymentMethod(PaymentMethodType paymentMethod) {
        if (paymentMethod == null) {
            return "";
        }
        return switch (paymentMethod) {
            case CASH -> "Efectivo";
            case DEBIT_CARD -> "Tarjeta de Debito";
            case CREDIT_CARD -> "Tarjeta de Credito";
            case TRANSFER -> "Transferencia";
            case OTHER -> "Otro";
        };
    }

    private void writeJson(HttpServletResponse response, List<ReportMovementRow> movements, YearMonth period)
            throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition(period, "json"));

        objectMapper.writeValue(response.getOutputStream(), movements);
    }

    private static String buildContentDisposition(YearMonth period, String extension) {
        return "attachment; filename=\"finsmart-" + period.getYear() + "-"
                + String.format(Locale.ROOT, "%02d", period.getMonthValue()) + "." + extension + "\"";
    }

    /**
     * Minimal RFC4180-style escaping: wraps the value in quotes (doubling any embedded quotes)
     * only when it contains a comma, quote or newline that would otherwise break the CSV row.
     * Also neutralizes formula/CSV injection (CWE-1236) by prefixing a leading
     * {@code =}, {@code +}, {@code -} or {@code @} with a single quote, since spreadsheet
     * applications would otherwise interpret the field as an executable formula.
     *
     * @param value raw field value, possibly {@code null}
     * @return a CSV-safe representation of {@code value}, or an empty string when {@code null}
     */
    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) {
            value = "'" + value;
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
