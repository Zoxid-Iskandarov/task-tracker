package com.walking.backend.service.mapper.task;

import com.walking.backend.domain.dto.task.CreateTaskRequest;
import com.walking.backend.domain.model.Task;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CreateTaskRequestMapper {

    Task toEntity(CreateTaskRequest createTaskRequest);
}
