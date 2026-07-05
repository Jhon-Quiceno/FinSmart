package com.smartfinance.backend.repository;

import com.smartfinance.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Ids of every active user, backing {@code MonthEndPredictionJob} (Sprint 5, Batch 3), which
     * computes a per-user projection for each id returned here.
     */
    @Query("SELECT u.id FROM User u WHERE u.active = true")
    List<Long> findAllIdsByActiveTrue();

    /**
     * Ids of every user who has logged in at least once, backing {@code InactivityReminderJob}
     * (Sprint 5, Batch 3) — a user who never logged in has nothing to be "inactive" from.
     */
    @Query("SELECT u.id FROM User u WHERE u.lastLoginAt IS NOT NULL")
    List<Long> findAllIdsWithLastLoginNotNull();
}
