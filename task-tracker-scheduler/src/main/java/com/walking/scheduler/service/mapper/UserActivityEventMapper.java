package com.walking.scheduler.service.mapper;

import com.walking.scheduler.domain.dto.UserActivityEvent;
import com.walking.scheduler.domain.model.UserActivity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserActivityEventMapper {

    @Mapping(target = "activityType", source = "type")
    @Mapping(target = "isProcessed", constant = "false")
    UserActivity toEntity(UserActivityEvent userActivityEvent);
}
