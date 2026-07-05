package com.smartfinance.backend.exception;

/**
 * Raised when {@code PATCH /api/recurring/{id}/pay} is called before the recurring payment's
 * {@code nextPaymentDate} has actually arrived. Without this guard, clicking "Marcar como
 * pagado" repeatedly (each click evaluated at a different point in time, so the
 * {@link RecurringPaymentAlreadyPaidException} race guard never triggers) would create a
 * duplicate {@link com.smartfinance.backend.model.Expense} and advance {@code nextPaymentDate}
 * on every click. Mapped to HTTP 409 by {@link GlobalExceptionHandler} so the caller can
 * distinguish "not due yet" from a generic failure.
 */
public class RecurringPaymentNotDueYetException extends RuntimeException {

    public RecurringPaymentNotDueYetException(String message) {
        super(message);
    }
}
