package com.walking.backend.service.mapper.attachment;

import com.walking.backend.domain.dto.attachment.TaskAttachmentResponse;
import com.walking.backend.domain.dto.user.UserShortResponse;
import com.walking.backend.domain.model.TaskAttachment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TaskAttachmentResponseMapper {

    @Mapping(target = "id", source = "taskAttachment.id")
    @Mapping(target = "uploadedBy", source = "userShortResponse")
    TaskAttachmentResponse toDto(TaskAttachment taskAttachment, UserShortResponse userShortResponse);
}
