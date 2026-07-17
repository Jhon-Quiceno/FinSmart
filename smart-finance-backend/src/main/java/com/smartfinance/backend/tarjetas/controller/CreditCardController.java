package com.smartfinance.backend.tarjetas.controller;

import com.smartfinance.backend.tarjetas.model.dto.CreditCardRequest;
import com.smartfinance.backend.tarjetas.model.dto.CreditCardResponse;
import com.smartfinance.backend.tarjetas.model.dto.CreditCardUpdateRequest;
import com.smartfinance.backend.tarjetas.service.CreditCardService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for the current user's {@link com.smartfinance.backend.tarjetas.model.entity.CreditCard} records.
 */
@RestController
@RequestMapping("/api/cards")
public class CreditCardController {

    private final CreditCardService creditCardService;

    public CreditCardController(CreditCardService creditCardService) {
        this.creditCardService = creditCardService;
    }

    @GetMapping
    public ResponseEntity<Page<CreditCardResponse>> getCards(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(creditCardService.getCards(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreditCardResponse> getCard(@PathVariable("id") Long cardId) {
        return ResponseEntity.ok(creditCardService.getCard(cardId));
    }

    @PostMapping
    public ResponseEntity<CreditCardResponse> createCard(@Valid @RequestBody CreditCardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(creditCardService.createCard(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CreditCardResponse> updateCard(
            @PathVariable("id") Long cardId,
            @Valid @RequestBody CreditCardUpdateRequest request
    ) {
        return ResponseEntity.ok(creditCardService.updateCard(cardId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable("id") Long cardId) {
        creditCardService.deleteCard(cardId);
        return ResponseEntity.noContent().build();
    }
}
