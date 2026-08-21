package com.smartfinance.backend.servicios.repository;

import com.smartfinance.backend.servicios.model.entity.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Persistence access for {@link PushToken}, always scoped by owner.
 */
public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    List<PushToken> findByUser_Id(Long userId);

    Optional<PushToken> findByUser_IdAndDeviceId(Long userId, String deviceId);

    /**
     * Deletes at most one row, scoped to the owner so a client can never remove another user's
     * token by guessing a deviceId. {@code @Modifying} instead of a derived delete method so this
     * runs as a single statement and the caller can assert on the affected-row count.
     *
     * @param userId   owner of the token
     * @param deviceId the device identifier to remove
     * @return number of rows deleted (0 or 1)
     */
    @Modifying
    @Query("delete from PushToken p where p.user.id = :userId and p.deviceId = :deviceId")
    int deleteByUser_IdAndDeviceId(@Param("userId") Long userId, @Param("deviceId") String deviceId);
}
