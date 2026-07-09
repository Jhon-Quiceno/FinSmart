package com.smartfinance.backend.repository;

import com.smartfinance.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Atomically reserves one unit of {@code userId}'s monthly AI chat quota for {@code period}
     * (see {@code AiChatService#chat}), replacing a separate count-then-act check that was racy
     * under concurrent requests.
     *
     * <p>When {@code ai_chat_period} does not yet match {@code period} (new month, or the user has
     * never chatted before), the counter is reset to {@code 1} for the new period. Otherwise it is
     * incremented — but only if doing so would not exceed {@code limit}: the {@code WHERE} clause
     * guards against updating (and returns {@code 0} affected rows) whenever the stored period
     * already matches {@code period} AND {@code ai_chat_used >= limit}, so at most one of any
     * number of concurrent callers can win the reservation once the limit is reached.
     *
     * @return {@code 1} if the reservation succeeded, {@code 0} if the quota is already exhausted
     *         for {@code period}
     */
    @Modifying
    @Query("""
            UPDATE User u
               SET u.aiChatUsed = CASE WHEN u.aiChatPeriod = :period THEN u.aiChatUsed + 1 ELSE 1 END,
                   u.aiChatPeriod = :period
             WHERE u.id = :userId
               AND (u.aiChatPeriod IS NULL OR u.aiChatPeriod <> :period OR u.aiChatUsed < :limit)
            """)
    int reserveAiChatQuota(@Param("userId") Long userId, @Param("period") String period, @Param("limit") int limit);

    /**
     * Decrements {@code userId}'s AI chat counter for {@code period} by one (floored at zero),
     * refunding a reservation made by {@link #reserveAiChatQuota} when the AI provider call it was
     * guarding ultimately fails. Not called by {@code AiChatService#chat} today — see the design
     * note on that method for why the surrounding {@code @Transactional} rollback already covers
     * that case — but kept as an explicit, independently usable primitive.
     */
    @Modifying
    @Query("""
            UPDATE User u
               SET u.aiChatUsed = CASE WHEN u.aiChatUsed > 0 THEN u.aiChatUsed - 1 ELSE 0 END
             WHERE u.id = :userId
               AND u.aiChatPeriod = :period
            """)
    int releaseAiChatQuota(@Param("userId") Long userId, @Param("period") String period);

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
