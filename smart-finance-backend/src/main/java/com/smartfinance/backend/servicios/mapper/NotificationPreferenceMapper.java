package com.smartfinance.backend.servicios.mapper;

import com.smartfinance.backend.servicios.model.dto.NotificationPreferenceRequest;
import com.smartfinance.backend.servicios.model.dto.NotificationPreferenceResponse;
import com.smartfinance.backend.servicios.model.entity.NotificationPreference;
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
