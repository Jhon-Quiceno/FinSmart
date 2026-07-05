package com.smartfinance.backend.mapper;

import com.smartfinance.backend.dto.notification.NotificationPreferenceRequest;
import com.smartfinance.backend.dto.notification.NotificationPreferenceResponse;
import com.smartfinance.backend.model.NotificationPreference;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper between {@link NotificationPreference} and its request/response DTOs.
 *
 * <p>{@link #updateEntityFromRequest} ignores {@code id}, {@code user}, {@code createdAt} and
 * {@code updatedAt} — those are managed by {@code NotificationService}
 * (owner assignment and auditing), never by the client payload.
 */
@Mapper(componentModel = "spring")
public interface NotificationPreferenceMapper {

    NotificationPreferenceResponse toResponse(NotificationPreference preference);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(
            NotificationPreferenceRequest request, @MappingTarget NotificationPreference preference
    );
}
