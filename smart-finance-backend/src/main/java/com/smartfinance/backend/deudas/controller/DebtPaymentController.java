package com.smartfinance.backend.deudas.controller;

import com.smartfinance.backend.deudas.model.dto.DebtPaymentRequest;
import com.smartfinance.backend.deudas.model.dto.DebtPaymentResponse;
import com.smartfinance.backend.deudas.service.DebtPaymentService;
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
 * REST endpoints for {@link com.smartfinance.backend.deudas.model.entity.DebtPayment} records registered
 * against a debt owned by the current user.
 */
@RestController
@RequestMapping("/api/debts/{debtId}/payments")
public class DebtPaymentController {

    private final DebtPaymentService debtPaymentService;

    public DebtPaymentController(DebtPaymentService debtPaymentService) {
        this.debtPaymentService = debtPaymentService;
    }

    @GetMapping
    public ResponseEntity<Page<DebtPaymentResponse>> getPayments(
            @PathVariable("debtId") Long debtId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(debtPaymentService.getPayments(debtId, pageable));
    }

    @PostMapping
    public ResponseEntity<DebtPaymentResponse> createPayment(
            @PathVariable("debtId") Long debtId,
            @Valid @RequestBody DebtPaymentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(debtPaymentService.createPayment(debtId, request));
    }
}
