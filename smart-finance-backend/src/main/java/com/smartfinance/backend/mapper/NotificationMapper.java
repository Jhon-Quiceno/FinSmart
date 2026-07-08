package com.smartfinance.backend.mapper;

import com.smartfinance.backend.dto.notification.NotificationResponse;
import com.smartfinance.backend.model.Notification;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper between {@link Notification} and its response DTO.
 *
 * <p>Notifications are never created or updated from a client-supplied DTO (they are always
 * built server-side by {@code NotificationService#createNotification}), so this mapper only
 * needs a read direction.
 */
@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);
}
