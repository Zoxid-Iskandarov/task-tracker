package com.walking.backend.service.mapper.comment;

import com.walking.backend.domain.dto.comment.CommentResponse;
import com.walking.backend.domain.dto.user.UserShortResponse;
import com.walking.backend.domain.model.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommentResponseMapper {

    @Mapping(target = "id", source = "comment.id")
    @Mapping(target = "author", source = "userShortResponse")
    @Mapping(target = "isEdited", expression = "java(isEdited(comment))")
    CommentResponse toDto(Comment comment, UserShortResponse userShortResponse);

    default boolean isEdited(Comment comment) {
        return comment.getUpdated().isAfter(comment.getCreated());
    }
}
