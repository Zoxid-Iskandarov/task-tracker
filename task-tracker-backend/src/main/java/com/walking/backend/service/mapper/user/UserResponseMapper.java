package com.walking.backend.service.mapper.user;

import com.walking.backend.domain.dto.user.UserResponse;
import com.walking.backend.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserResponseMapper {
    UserResponse toDto(User user);
}
