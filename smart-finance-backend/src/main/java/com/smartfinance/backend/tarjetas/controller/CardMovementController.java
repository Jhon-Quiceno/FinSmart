package com.smartfinance.backend.tarjetas.controller;

import com.smartfinance.backend.tarjetas.model.dto.CardMovementResponse;
import com.smartfinance.backend.tarjetas.model.dto.CardPaymentRequest;
import com.smartfinance.backend.tarjetas.model.dto.CardPurchaseRequest;
import com.smartfinance.backend.tarjetas.model.entity.CardMovementType;
import com.smartfinance.backend.tarjetas.service.CardMovementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints REST para los {@link com.smartfinance.backend.tarjetas.model.entity.CardMovement}
 * registrados contra una tarjeta propiedad del usuario actual.
 */
@RestController
@RequestMapping("/api/cards/{cardId}")
public class CardMovementController {

    private final CardMovementService cardMovementService;

    public CardMovementController(CardMovementService cardMovementService) {
        this.cardMovementService = cardMovementService;
    }

    @PostMapping("/purchases")
    public ResponseEntity<CardMovementResponse> registerPurchase(
            @PathVariable("cardId") Long cardId,
            @Valid @RequestBody CardPurchaseRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardMovementService.registerPurchase(cardId, request));
    }

    @PostMapping("/payments")
    public ResponseEntity<CardMovementResponse> registerPayment(
            @PathVariable("cardId") Long cardId,
            @Valid @RequestBody CardPaymentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardMovementService.registerPayment(cardId, request));
    }

    @GetMapping("/movements")
    public ResponseEntity<Page<CardMovementResponse>> getMovements(
            @PathVariable("cardId") Long cardId,
            @RequestParam(name = "type", required = false) CardMovementType type,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(cardMovementService.getMovements(cardId, type, pageable));
    }
}
