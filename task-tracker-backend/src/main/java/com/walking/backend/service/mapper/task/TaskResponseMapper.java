package com.walking.backend.service.mapper.task;

import com.walking.backend.domain.dto.task.TaskResponse;
import com.walking.backend.domain.model.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TaskResponseMapper {

    @Mapping(target = "sectionId", source = "section.id")
    TaskResponse toDto(Task task);
}
