package com.smartfinance.backend.mapper;

import com.smartfinance.backend.dto.auth.UserResponse;
import com.smartfinance.backend.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
