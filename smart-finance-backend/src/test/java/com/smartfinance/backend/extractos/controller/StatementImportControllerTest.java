package com.smartfinance.backend.extractos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartfinance.backend.common.config.SecurityConfig;
import com.smartfinance.backend.common.security.JwtService;
import com.smartfinance.backend.extractos.exception.EmptyStatementTextException;
import com.smartfinance.backend.extractos.exception.StatementPasswordException;
import com.smartfinance.backend.extractos.exception.UnsupportedStatementFileException;
import com.smartfinance.backend.extractos.model.MovementType;
import com.smartfinance.backend.extractos.model.dto.ImportConfirmRow;
import com.smartfinance.backend.extractos.model.dto.ImportPreviewRow;
import com.smartfinance.backend.extractos.model.dto.StatementConfirmRequest;
import com.smartfinance.backend.extractos.model.dto.StatementImportResultResponse;
import com.smartfinance.backend.extractos.model.dto.StatementPreviewResponse;
import com.smartfinance.backend.extractos.service.StatementImportService;
import com.smartfinance.backend.gastos.model.entity.PaymentMethodType;
import com.smartfinance.backend.usuario.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatementImportController.class)
@Import(SecurityConfig.class)
class StatementImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private StatementImportService statementImportService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static final String AUTH_HEADER = "Bearer test-token";

    @BeforeEach
    void setUp() {
        Claims mockClaims = org.mockito.Mockito.mock(Claims.class);
        when(mockClaims.getSubject()).thenReturn("1");
        when(jwtService.parseAccessToken(any())).thenReturn(mockClaims);
        when(userRepository.existsById(1L)).thenReturn(true);
    }

    @Test
    void previewReturns200WithExtractedRows() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "extracto.csv", MediaType.TEXT_PLAIN_VALUE, "contenido".getBytes()
        );
        StatementPreviewResponse response = new StatementPreviewResponse(
                List.of(new ImportPreviewRow(
                        LocalDate.of(2026, 6, 1), "Compra", BigDecimal.valueOf(100),
                        MovementType.EXPENSE, false, null, null
                )),
                1, 0
        );
        when(statementImportService.preview(any(), isNull())).thenReturn(response);

        mockMvc.perform(multipart("/api/statement-imports/preview")
                        .file(file)
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(1))
                .andExpect(jsonPath("$.duplicateRows").value(0))
                .andExpect(jsonPath("$.rows[0].description").value("Compra"));
    }

    @Test
    void previewReturns400WhenFileFormatIsNotSupported() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "extracto.docx", MediaType.TEXT_PLAIN_VALUE, "contenido".getBytes()
        );
        when(statementImportService.preview(any(), isNull()))
                .thenThrow(new UnsupportedStatementFileException("El formato del archivo no es compatible."));

        mockMvc.perform(multipart("/api/statement-imports/preview")
                        .file(file)
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void previewReturns422WhenExtractedTextIsEmpty() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "extracto.pdf", MediaType.APPLICATION_PDF_VALUE, "contenido".getBytes()
        );
        when(statementImportService.preview(any(), isNull()))
                .thenThrow(new EmptyStatementTextException("No se encontró texto en el archivo."));

        mockMvc.perform(multipart("/api/statement-imports/preview")
                        .file(file)
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void previewReturns422WhenPdfPasswordIsInvalid() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "extracto.pdf", MediaType.APPLICATION_PDF_VALUE, "contenido".getBytes()
        );
        when(statementImportService.preview(any(), any()))
                .thenThrow(new StatementPasswordException("La contraseña del PDF es incorrecta."));

        mockMvc.perform(multipart("/api/statement-imports/preview")
                        .file(file)
                        .param("password", "incorrecta")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void confirmReturns200WithCreatedCount() throws Exception {
        StatementConfirmRequest request = new StatementConfirmRequest(List.of(
                new ImportConfirmRow(
                        MovementType.EXPENSE, BigDecimal.valueOf(100), LocalDate.of(2026, 6, 1),
                        "Compra", null, PaymentMethodType.CASH
                )
        ));
        when(statementImportService.confirm(any(StatementConfirmRequest.class)))
                .thenReturn(new StatementImportResultResponse(1));

        mockMvc.perform(post("/api/statement-imports/confirm")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(1));
    }

    @Test
    void confirmReturns400WhenRowsAreEmpty() throws Exception {
        String invalidBody = """
                {"rows": []}
                """;

        mockMvc.perform(post("/api/statement-imports/confirm")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void confirmReturns400WhenRowAmountIsMissing() throws Exception {
        String invalidBody = """
                {"rows": [{"movementType": "EXPENSE", "date": "2026-06-01", "description": "Compra"}]}
                """;

        mockMvc.perform(post("/api/statement-imports/confirm")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void previewReturns401WithoutAuthToken() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "extracto.csv", MediaType.TEXT_PLAIN_VALUE, "contenido".getBytes()
        );

        mockMvc.perform(multipart("/api/statement-imports/preview").file(file).with(csrf()))
                .andExpect(status().isForbidden());
    }
}
