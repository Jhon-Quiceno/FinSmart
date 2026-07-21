package com.smartfinance.backend.integraciones.repository;

import com.smartfinance.backend.integraciones.model.entity.TelegramLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TelegramLinkRepository extends JpaRepository<TelegramLink, Long> {

    Optional<TelegramLink> findByTelegramChatId(String telegramChatId);

    boolean existsByUser_Id(Long userId);
}
