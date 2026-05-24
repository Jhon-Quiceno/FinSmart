package com.smartfinance.backend.controller;

import com.smartfinance.backend.dto.transaction.TransactionRequest;
import com.smartfinance.backend.dto.transaction.TransactionResponse;
import com.smartfinance.backend.model.TransactionType;
import com.smartfinance.backend.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * REST endpoints for managing transactions.
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Retrieves a filtered list of transactions for the authenticated user.
     *
     * @param type       transaction type filter
     * @param categoryId category filter
     * @param accountId  account filter
     * @param fromDate   start date filter
     * @param toDate     end date filter
     * @param pageable   pagination parameters
     * @return paginated transactions
     */
    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getTransactions(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false, name = "category") Long categoryId,
            @RequestParam(required = false, name = "account") Long accountId,
            @RequestParam(required = false, name = "from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false, name = "to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Pageable pageable
    ) {
        return ResponseEntity.ok(transactionService.getTransactions(type, categoryId, accountId, fromDate, toDate, pageable));
    }

    /**
     * Creates a new transaction for the authenticated user.
     *
     * @param request transaction payload
     * @return created transaction
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.createTransaction(request));
    }

    /**
     * Updates an existing transaction owned by the authenticated user.
     *
     * @param transactionId transaction identifier
     * @param request       transaction payload
     * @return updated transaction
     */
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @PathVariable("id") Long transactionId,
            @Valid @RequestBody TransactionRequest request
    ) {
        return ResponseEntity.ok(transactionService.updateTransaction(transactionId, request));
    }

    /**
     * Deletes a transaction owned by the authenticated user.
     *
     * @param transactionId transaction identifier
     * @return empty response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable("id") Long transactionId) {
        transactionService.deleteTransaction(transactionId);
        return ResponseEntity.noContent().build();
    }
}
