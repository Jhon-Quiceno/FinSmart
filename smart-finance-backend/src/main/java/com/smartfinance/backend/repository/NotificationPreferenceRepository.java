package com.smartfinance.backend.repository;

import com.smartfinance.backend.model.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Persistence access for {@link NotificationPreference}, always scoped by owner.
 */
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    Optional<NotificationPreference> findByUser_Id(Long userId);
}
