package com.walking.backend.service.mapper.task;

import com.walking.backend.domain.dto.task.TaskPreviewResponse;
import com.walking.backend.domain.dto.user.UserShortResponse;
import com.walking.backend.domain.model.Task;
import com.walking.backend.service.mapper.label.LabelResponseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = LabelResponseMapper.class)
public interface TaskPreviewResponseMapper {

    @Mapping(target = "sectionId", source = "task.section.id")
    @Mapping(target = "labels", source = "task.labels")
    @Mapping(target = "assignees", source = "assignees")
    TaskPreviewResponse toDto(Task task, List<UserShortResponse> assignees);
}
