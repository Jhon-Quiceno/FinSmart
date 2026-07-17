package com.smartfinance.backend.tarjetas.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
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
import java.util.ArrayList;
import java.util.List;

/**
 * The fixed-capital amortization plan for a single deferred purchase (2+ cuotas), owning the
 * originating {@link CardMovement} (type {@link CardMovementType#INSTALLMENT_PURCHASE}) via a
 * {@code UNIQUE} FK.
 *
 * <p>{@link #rateAtPurchase} is frozen from {@link CreditCard#getMonthlyRate()} at purchase time
 * and never recalculated afterwards, even if the card's rate later changes (spec "Tasa congelada
 * ante cambios posteriores"). There is no cached remaining-principal field: it is always derived
 * as {@code SUM(installments.capitalAmount WHERE status = PENDING)}, since {@link #installments}
 * is the auditable per-purchase ledger (see design decision 2).
 */
@Entity
@Table(name = "installment_plans")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movement_id", nullable = false, unique = true)
    private CardMovement movement;

    @Column(name = "installment_count", nullable = false)
    private Integer installmentCount;

    /** Frozen copy of {@link CreditCard#getMonthlyRate()} at the moment this plan was created. */
    @Column(name = "rate_at_purchase", nullable = false, precision = 6, scale = 4)
    private BigDecimal rateAtPurchase;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Installment> installments = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
