package com.smartfinance.backend.tarjetas.model.entity;

import com.smartfinance.backend.usuario.model.entity.User;
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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A revolving credit card owned by a {@link User}.
 *
 * <p>{@link #currentBalance} is the cached, always-consistent sum of every {@link CardMovement}
 * recorded against this card (mirroring {@link com.smartfinance.backend.deudas.model.entity.Debt#getRemainingAmount()}).
 * It must only ever change through the atomic {@code @Modifying} updates on
 * {@code CreditCardRepository} (increment/decrement, added in Fase B.2) driven by a concrete
 * {@link CardMovement} — never overwritten directly, so every change is traceable to a ledger
 * entry. Available credit ({@code creditLimit - currentBalance}) is always derived at read time,
 * never stored as its own column.
 */
@Entity
@Table(name = "credit_cards")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreditCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 100)
    private String bank;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardFranchise franchise;

    @Column(name = "credit_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal creditLimit;

    /** Effective monthly rate (e.g. {@code 0.0250} for 2.5% E.M.). */
    @Column(name = "monthly_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal monthlyRate;

    @Column(name = "cutoff_day", nullable = false)
    private Integer cutoffDay;

    @Column(name = "payment_due_day", nullable = false)
    private Integer paymentDueDay;

    @Column(name = "current_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentBalance;

    /**
     * Guards {@code CardCycleCloseJob} (Fase B.4) against closing the same billing cycle twice:
     * {@code NULL} until the first cycle close, then set to the date of the most recently closed
     * cycle.
     */
    @Column(name = "last_cutoff_date")
    private LocalDate lastCutoffDate;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
