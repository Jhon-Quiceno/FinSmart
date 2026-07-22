package com.smartfinance.backend.ia.model.dto;

import com.smartfinance.backend.gastos.model.entity.CategoryType;

import java.math.BigDecimal;

/**
 * Result of {@code ReceiptExtractionService#extractFromImage}: whether the image was recognized as
 * a real receipt/invoice and, if so, the merchant/description, the TOTAL amount, the decided
 * movement type, and (best-effort) the matched category from that decided type.
 *
 * @param isReceipt    whether the image was recognized as a real, legible receipt or invoice
 * @param description  the merchant name or a short description, or {@code null} when {@link #isReceipt}
 *                      is {@code false}
 * @param amount       the extracted TOTAL amount (not an individual line item), or {@code null}
 *                      when {@link #isReceipt} is {@code false}
 * @param movementType the decided movement type ({@link CategoryType#EXPENSE} for the common case
 *                      of a purchase receipt, {@link CategoryType#INCOME} for the rarer refund/credit
 *                      note case), or {@code null} when {@link #isReceipt} is {@code false}
 * @param categoryId   identifier of the matched category, or {@code null} when the user has no
 *                      categories of {@link #movementType} yet, the assistant found no suitable
 *                      match, or {@link #isReceipt} is {@code false}
 * @param categoryName name of the matched category, or {@code null} under the same conditions as
 *                      {@link #categoryId}
 */
public record ReceiptExtraction(
        boolean isReceipt,
        String description,
        BigDecimal amount,
        CategoryType movementType,
        Long categoryId,
        String categoryName
) {

    /**
     * @return the shared "couldn't extract anything usable from this image" result: {@link #isReceipt}
     *         {@code false} and every other field {@code null} — used both when the model itself
     *         reports {@code isReceipt: false} and when the model's response could not be
     *         interpreted at all (see {@code ReceiptExtractionService}'s defensive parsing)
     */
    public static ReceiptExtraction notAReceipt() {
        return new ReceiptExtraction(false, null, null, null, null, null);
    }
}
