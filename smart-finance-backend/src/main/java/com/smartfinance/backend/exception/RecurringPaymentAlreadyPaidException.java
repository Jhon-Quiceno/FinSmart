package com.smartfinance.backend.exception;

/**
 * Raised when {@code PATCH /api/recurring/{id}/pay} loses the race to advance
 * {@code nextPaymentDate} — another execution (client retry after timeout, double-click, a
 * second open tab) already processed this payment first. Mapped to HTTP 409 by
 * {@link GlobalExceptionHandler} so the caller can distinguish "already handled by a
 * concurrent request" from a generic failure and avoid blindly retrying.
 */
public class RecurringPaymentAlreadyPaidException extends RuntimeException {

    public RecurringPaymentAlreadyPaidException(String message) {
        super(message);
    }
}
