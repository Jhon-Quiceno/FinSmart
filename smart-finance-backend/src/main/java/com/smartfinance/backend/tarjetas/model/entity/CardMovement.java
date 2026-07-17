package com.smartfinance.backend.tarjetas.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A single, immutable ledger entry against a {@link CreditCard} (compra, pago, interés
 * materializado, etc.) — the signed source of truth {@link CreditCard#getCurrentBalance()} is
 * derived from (spec "Inmutabilidad de movimientos": no update endpoint is ever exposed for this
 * entity, mirroring {@link com.smartfinance.backend.deudas.model.entity.DebtCharge}/
 * {@link com.smartfinance.backend.deudas.model.entity.DebtPayment}).
 *
 * <p>{@link #amount} is always stored positive; the effect on the card's balance (increase vs.
 * decrease) is determined by {@link #type}, not the sign of {@link #amount}.
 */
@Entity
@Table(name = "card_movements")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CardMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private CreditCard card;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private CardMovementType type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "movement_date", nullable = false)
    private LocalDate date;

    @Column(length = 255)
    private String description;

    /**
     * Set only on aggregated {@link CardMovementType#INTEREST} movements created by
     * {@code CardCycleCloseJob} (Fase B.4); used for cycle-close idempotency/audit. {@code NULL}
     * for every other movement type.
     */
    @Column(name = "cycle_close_date")
    private LocalDate cycleCloseDate;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /**
     * Back-reference to the {@link InstallmentPlan} this movement originated (only set when
     * {@link #type} is {@link CardMovementType#INSTALLMENT_PURCHASE}). This side owns no FK
     * column — {@link InstallmentPlan#getMovement()} is the owning side.
     */
    @OneToOne(mappedBy = "movement", fetch = FetchType.LAZY)
    private InstallmentPlan installmentPlan;
}
