package com.smartfinance.backend.repository;

import com.smartfinance.backend.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenId(UUID tokenId);

    void deleteByExpiresAtBefore(Instant dateTime);
}
