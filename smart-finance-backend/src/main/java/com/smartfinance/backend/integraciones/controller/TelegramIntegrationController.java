package com.smartfinance.backend.integraciones.controller;

import com.smartfinance.backend.integraciones.model.dto.TelegramConfirmLinkRequest;
import com.smartfinance.backend.integraciones.model.dto.TelegramExpenseRequest;
import com.smartfinance.backend.integraciones.model.dto.TelegramLinkCodeResponse;
import com.smartfinance.backend.integraciones.model.dto.TelegramReceiptRequest;
import com.smartfinance.backend.integraciones.model.dto.TelegramReplyResponse;
import com.smartfinance.backend.integraciones.service.TelegramExpenseService;
import com.smartfinance.backend.integraciones.service.TelegramLinkService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints del bot de Telegram (Sprint 2, Frente 2), orquestado por n8n.
 *
 * <p>{@code POST /link-code} lo llama la app con JWT: genera un código de un solo uso. Las otras
 * tres rutas ({@code /confirm-link}, {@code /expenses} y {@code /receipts}) las llama n8n
 * server-to-server, sin JWT, autenticadas por
 * {@link com.smartfinance.backend.common.security.TelegramWebhookFilter} con un secreto compartido
 * (ver {@code SecurityConfig}).
 */
@RestController
@RequestMapping("/api/integrations/telegram")
public class TelegramIntegrationController {

    private static final String ACCOUNT_LINKED_MESSAGE =
            "Cuenta vinculada correctamente. Ya puede registrar gastos por Telegram.";

    private final TelegramLinkService telegramLinkService;
    private final TelegramExpenseService telegramExpenseService;

    public TelegramIntegrationController(
            TelegramLinkService telegramLinkService,
            TelegramExpenseService telegramExpenseService
    ) {
        this.telegramLinkService = telegramLinkService;
        this.telegramExpenseService = telegramExpenseService;
    }

    @PostMapping("/link-code")
    public ResponseEntity<TelegramLinkCodeResponse> generateLinkCode() {
        return ResponseEntity.ok(telegramLinkService.generateLinkCode());
    }

    @PostMapping("/confirm-link")
    public ResponseEntity<TelegramReplyResponse> confirmLink(@Valid @RequestBody TelegramConfirmLinkRequest request) {
        telegramLinkService.confirmLink(request.code(), request.chatId());
        return ResponseEntity.ok(new TelegramReplyResponse(ACCOUNT_LINKED_MESSAGE));
    }

    @PostMapping("/expenses")
    public ResponseEntity<TelegramReplyResponse> registerExpense(@Valid @RequestBody TelegramExpenseRequest request) {
        String reply = telegramExpenseService.registerFromMessage(request.chatId(), request.text());
        return ResponseEntity.ok(new TelegramReplyResponse(reply));
    }

    @PostMapping("/receipts")
    public ResponseEntity<TelegramReplyResponse> registerReceipt(@Valid @RequestBody TelegramReceiptRequest request) {
        String reply = telegramExpenseService.registerFromPhoto(request.chatId(), request.imageUrl());
        return ResponseEntity.ok(new TelegramReplyResponse(reply));
    }
}
