package com.walking.backend.service.mapper.attachment;

import com.walking.backend.domain.dto.attachment.TaskAttachmentDownloadResponse;
import com.walking.backend.domain.model.TaskAttachment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TaskAttachmentDownloadResponseMapper {

    @Mapping(target = "url", source = "url")
    TaskAttachmentDownloadResponse toDto(TaskAttachment taskAttachment, String url);
}
