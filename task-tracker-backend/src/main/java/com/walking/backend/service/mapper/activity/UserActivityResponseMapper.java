package com.walking.backend.service.mapper.activity;

import com.walking.backend.domain.dto.activity.UserActivityResponse;
import com.walking.backend.domain.model.UserActivity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserActivityResponseMapper {

    UserActivityResponse toDto(UserActivity userActivity);
}
