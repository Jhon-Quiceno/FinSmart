package com.smartfinance.backend.usuario.mapper;

import com.smartfinance.backend.usuario.model.dto.UserResponse;
import com.smartfinance.backend.usuario.model.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
