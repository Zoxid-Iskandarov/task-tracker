package com.walking.backend.service.mapper.user;

import com.walking.backend.domain.dto.user.UserProfileResponse;
import com.walking.backend.domain.model.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserProfileResponseMapper {

    @Mapping(target = "id", source = "userId")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "email", source = "user.email")
    UserProfileResponse toDto(UserProfile userProfile);
}
