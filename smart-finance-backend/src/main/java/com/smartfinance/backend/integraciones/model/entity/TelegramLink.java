package com.smartfinance.backend.integraciones.model.entity;

import com.smartfinance.backend.usuario.model.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Vínculo entre un chat de Telegram y el {@link User} que lo confirmó, usado por
 * {@code TelegramExpenseService} para resolver a qué usuario pertenece un mensaje entrante del
 * bot de Telegram (orquestado por n8n).
 *
 * <p>{@link #telegramChatId} es único: un mismo chat de Telegram solo puede estar vinculado a un
 * usuario a la vez (ver {@code V24__create_telegram_links.sql}). Re-vincular un chat ya vinculado
 * actualiza el {@link #user} de esta fila en lugar de crear una nueva (ver
 * {@code TelegramLinkService#confirmLink}).
 */
@Entity
@Table(name = "telegram_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TelegramLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "telegram_chat_id", nullable = false, unique = true, length = 50)
    private String telegramChatId;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    @PrePersist
    void onPrePersist() {
        if (linkedAt == null) {
            linkedAt = Instant.now();
        }
    }
}
