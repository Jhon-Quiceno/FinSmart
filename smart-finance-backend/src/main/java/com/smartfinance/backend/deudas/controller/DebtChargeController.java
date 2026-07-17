package com.smartfinance.backend.deudas.controller;

import com.smartfinance.backend.deudas.model.dto.DebtChargeRequest;
import com.smartfinance.backend.deudas.model.dto.DebtChargeResponse;
import com.smartfinance.backend.deudas.model.dto.DebtResponse;
import com.smartfinance.backend.deudas.service.DebtChargeService;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for {@link com.smartfinance.backend.deudas.model.entity.DebtCharge} records registered
 * against a debt owned by the current user.
 *
 * <p>Unlike {@link DebtPaymentController#createPayment}, {@link #createCharge} returns the
 * updated {@link DebtResponse} instead of the created charge — the client's primary interest
 * after registering a charge is the debt's new {@code remainingAmount}.
 */
@RestController
@RequestMapping("/api/debts/{debtId}/charges")
public class DebtChargeController {

    private final DebtChargeService debtChargeService;

    public DebtChargeController(DebtChargeService debtChargeService) {
        this.debtChargeService = debtChargeService;
    }

    @GetMapping
    public ResponseEntity<Page<DebtChargeResponse>> getCharges(
            @PathVariable("debtId") Long debtId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(debtChargeService.getCharges(debtId, pageable));
    }

    @PostMapping
    public ResponseEntity<DebtResponse> createCharge(
            @PathVariable("debtId") Long debtId,
            @Valid @RequestBody DebtChargeRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(debtChargeService.createCharge(debtId, request));
    }
}
