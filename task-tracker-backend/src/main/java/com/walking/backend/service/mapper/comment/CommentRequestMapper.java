package com.walking.backend.service.mapper.comment;

import com.walking.backend.domain.dto.comment.CommentRequest;
import com.walking.backend.domain.model.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommentRequestMapper {

    Comment toEntity(CommentRequest commentRequest);
}
