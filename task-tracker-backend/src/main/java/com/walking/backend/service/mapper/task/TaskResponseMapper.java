package com.walking.backend.service.mapper.task;

import com.walking.backend.domain.dto.task.TaskResponse;
import com.walking.backend.domain.model.Task;
import com.walking.backend.service.mapper.label.LabelResponseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = LabelResponseMapper.class)
public interface TaskResponseMapper {

    @Mapping(target = "sectionId", source = "section.id")
    @Mapping(target = "labels", source = "labels")
    TaskResponse toDto(Task task);
}
