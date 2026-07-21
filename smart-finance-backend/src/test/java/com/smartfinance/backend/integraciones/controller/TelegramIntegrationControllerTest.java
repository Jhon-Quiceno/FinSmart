package com.smartfinance.backend.integraciones.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinance.backend.common.config.SecurityConfig;
import com.smartfinance.backend.common.security.JwtService;
import com.smartfinance.backend.integraciones.exception.TelegramChatNotLinkedException;
import com.smartfinance.backend.integraciones.model.dto.TelegramConfirmLinkRequest;
import com.smartfinance.backend.integraciones.model.dto.TelegramExpenseRequest;
import com.smartfinance.backend.integraciones.model.dto.TelegramLinkCodeResponse;
import com.smartfinance.backend.integraciones.model.dto.TelegramReceiptRequest;
import com.smartfinance.backend.integraciones.service.TelegramExpenseService;
import com.smartfinance.backend.integraciones.service.TelegramLinkService;
import com.smartfinance.backend.usuario.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code /link-code} stays behind JWT + CSRF like the rest of the authenticated API.
 * {@code /confirm-link} and {@code /expenses} are server-to-server (n8n): {@code permitAll} +
 * CSRF-ignored in {@link SecurityConfig}, authenticated instead by
 * {@link com.smartfinance.backend.common.security.TelegramWebhookFilter} via the
 * {@code X-Telegram-Webhook-Secret} header, configured here to {@code "test-secret"}.
 */
@WebMvcTest(TelegramIntegrationController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.integrations.telegram.webhook-secret=test-secret")
class TelegramIntegrationControllerTest {

    private static final String WEBHOOK_SECRET_HEADER = "X-Telegram-Webhook-Secret";
    private static final String WEBHOOK_SECRET = "test-secret";
    private static final String AUTH_HEADER = "Bearer test-token";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private TelegramLinkService telegramLinkService;

    @MockitoBean
    private TelegramExpenseService telegramExpenseService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @BeforeEach
    void setUp() {
        Claims mockClaims = org.mockito.Mockito.mock(Claims.class);
        when(mockClaims.getSubject()).thenReturn("1");
        when(jwtService.parseAccessToken(any())).thenReturn(mockClaims);
        when(userRepository.existsById(1L)).thenReturn(true);
    }

    @Test
    void linkCodeReturns200WithGeneratedCodeWhenAuthenticated() throws Exception {
        when(telegramLinkService.generateLinkCode()).thenReturn(new TelegramLinkCodeResponse("ABCD2345", 600));

        mockMvc.perform(post("/api/integrations/telegram/link-code")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ABCD2345"))
                .andExpect(jsonPath("$.expiresInSeconds").value(600));
    }

    @Test
    void linkCodeReturns403WithoutAuthToken() throws Exception {
        mockMvc.perform(post("/api/integrations/telegram/link-code").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void confirmLinkDoesNotRequireCsrfTokenBecauseItIsServerToServer() throws Exception {
        TelegramConfirmLinkRequest request = new TelegramConfirmLinkRequest("ABCD2345", "chat-1");

        mockMvc.perform(post("/api/integrations/telegram/confirm-link")
                        .header(WEBHOOK_SECRET_HEADER, WEBHOOK_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value(
                        "Cuenta vinculada correctamente. Ya puede registrar gastos por Telegram."));
    }

    @Test
    void confirmLinkReturns401WithoutTheWebhookSecretHeader() throws Exception {
        TelegramConfirmLinkRequest request = new TelegramConfirmLinkRequest("ABCD2345", "chat-1");

        mockMvc.perform(post("/api/integrations/telegram/confirm-link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expensesDoesNotRequireCsrfTokenBecauseItIsServerToServer() throws Exception {
        TelegramExpenseRequest request = new TelegramExpenseRequest("chat-1", "Uber 15000");
        when(telegramExpenseService.registerFromMessage("chat-1", "Uber 15000"))
                .thenReturn("✅ Gasto registrado: Uber — $15.000 (Transporte)");

        mockMvc.perform(post("/api/integrations/telegram/expenses")
                        .header(WEBHOOK_SECRET_HEADER, WEBHOOK_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("✅ Gasto registrado: Uber — $15.000 (Transporte)"));
    }

    @Test
    void expensesReturns401WithoutTheWebhookSecretHeader() throws Exception {
        TelegramExpenseRequest request = new TelegramExpenseRequest("chat-1", "Uber 15000");

        mockMvc.perform(post("/api/integrations/telegram/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Regresión de una revisión de seguridad: {@code TelegramWebhookFilter.shouldNotFilter()}
     * usa match exacto de path ({@code Set.contains}), mientras que {@code SecurityConfig} usa
     * {@code PathPattern} de Spring Security para las mismas dos rutas — dos mecanismos que
     * podrían divergir en un cambio futuro. Con barra final, el filtro no reconoce la ruta y no
     * la protege (ver {@code TelegramWebhookFilterTest#trailingSlashVariantIsNotRecognizedByThisFilterAndFallsThroughToTheChain}),
     * pero esto NO abre un agujero de autenticación: sin el header del secreto, {@code /expenses/}
     * tampoco calza con el {@code permitAll} de {@code SecurityConfig} (sin wildcard, y
     * {@code PathPattern} no matchea una barra final adicional por defecto), así que la solicitud
     * cae en {@code anyRequest().authenticated()} y se rechaza igual, esta vez por falta de JWT.
     * Se verifica end-to-end (contra el filtro real, registrado en {@link SecurityConfig}) que
     * ninguna de las dos capas deja pasar la solicitud.
     */
    @Test
    void expensesWithTrailingSlashIsNeverAuthorizedByEitherMechanism() throws Exception {
        TelegramExpenseRequest request = new TelegramExpenseRequest("chat-1", "Uber 15000");

        mockMvc.perform(post("/api/integrations/telegram/expenses/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void expensesReturns404WhenChatIsNotLinked() throws Exception {
        TelegramExpenseRequest request = new TelegramExpenseRequest("chat-1", "Uber 15000");
        doThrow(new TelegramChatNotLinkedException("Todavía no vinculaste tu cuenta."))
                .when(telegramExpenseService).registerFromMessage("chat-1", "Uber 15000");

        mockMvc.perform(post("/api/integrations/telegram/expenses")
                        .header(WEBHOOK_SECRET_HEADER, WEBHOOK_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void receiptsDoesNotRequireCsrfTokenBecauseItIsServerToServer() throws Exception {
        TelegramReceiptRequest request = new TelegramReceiptRequest("chat-1", "data:image/jpeg;base64,abc");
        when(telegramExpenseService.registerFromPhoto("chat-1", "data:image/jpeg;base64,abc"))
                .thenReturn("✅ Gasto registrado desde la foto: TESCO — $7 (Supermercado)");

        mockMvc.perform(post("/api/integrations/telegram/receipts")
                        .header(WEBHOOK_SECRET_HEADER, WEBHOOK_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("✅ Gasto registrado desde la foto: TESCO — $7 (Supermercado)"));
    }

    @Test
    void receiptsReturns401WithoutTheWebhookSecretHeader() throws Exception {
        TelegramReceiptRequest request = new TelegramReceiptRequest("chat-1", "data:image/jpeg;base64,abc");

        mockMvc.perform(post("/api/integrations/telegram/receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void receiptsReturns400WhenImageUrlIsMissing() throws Exception {
        TelegramReceiptRequest request = new TelegramReceiptRequest("chat-1", "  ");

        mockMvc.perform(post("/api/integrations/telegram/receipts")
                        .header(WEBHOOK_SECRET_HEADER, WEBHOOK_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void receiptsReturns404WhenChatIsNotLinked() throws Exception {
        TelegramReceiptRequest request = new TelegramReceiptRequest("chat-1", "data:image/jpeg;base64,abc");
        doThrow(new TelegramChatNotLinkedException("Todavía no vinculaste tu cuenta."))
                .when(telegramExpenseService).registerFromPhoto("chat-1", "data:image/jpeg;base64,abc");

        mockMvc.perform(post("/api/integrations/telegram/receipts")
                        .header(WEBHOOK_SECRET_HEADER, WEBHOOK_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}
