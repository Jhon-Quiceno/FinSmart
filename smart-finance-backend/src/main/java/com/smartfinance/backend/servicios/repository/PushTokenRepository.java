package com.smartfinance.backend.servicios.repository;

import com.smartfinance.backend.servicios.model.entity.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence access for {@link PushToken}, always scoped by owner.
 */
public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    List<PushToken> findByUser_Id(Long userId);

    Optional<PushToken> findByUser_IdAndDeviceId(Long userId, String deviceId);
}
