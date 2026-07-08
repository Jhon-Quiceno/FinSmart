package com.smartfinance.backend.repository;

import com.smartfinance.backend.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Persistence access for {@link Notification}, always scoped by owner.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByIdAndUser_Id(Long id, Long userId);

    Page<Notification> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUser_IdAndReadFalse(Long userId);

    /**
     * Backs {@code NotificationService#createNotification}'s pre-flight dedupe check. The
     * definitive guard is the {@code uk_notifications_user_dedupe_key} partial unique index
     * (see {@code V7__create_notifications_and_preferences.sql}); this method only avoids an
     * unnecessary insert attempt in the common (non-racing) case.
     */
    boolean existsByUser_IdAndDedupeKey(Long userId, String dedupeKey);

    /**
     * Bulk-marks every unread notification for {@code userId} as read in a single statement,
     * instead of loading and saving each row individually.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = CURRENT_TIMESTAMP "
            + "WHERE n.user.id = :userId AND n.read = false")
    int markAllAsReadForUser(@Param("userId") Long userId);
}
