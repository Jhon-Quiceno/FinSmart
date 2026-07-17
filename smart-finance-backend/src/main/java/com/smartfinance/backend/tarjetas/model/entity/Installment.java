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
 * A single installment (cuota) of an {@link InstallmentPlan}, frozen at purchase time by
 * {@code AmortizationService} (Fase B.3): {@link #capitalAmount} + {@link #interestAmount} never
 * change afterwards, and their sum across every installment of a plan whose {@link #status} is
 * {@code PENDING} is that purchase's own remaining principal (design decision 2).
 *
 * <p>{@link #status} starts {@code PENDING} and flips to {@code BILLED} exactly once, when
 * {@code CycleCloseService} (Fase B.4) materializes this installment's {@link #interestAmount}
 * into an aggregated {@link CardMovementType#INTEREST} movement, recorded here as
 * {@link #interestMovement}.
 */
@Entity
@Table(name = "installments")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Installment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private InstallmentPlan plan;

    @Column(nullable = false)
    private Integer number;

    @Column(name = "capital_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal capitalAmount;

    @Column(name = "interest_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal interestAmount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private InstallmentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interest_movement_id")
    private CardMovement interestMovement;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
