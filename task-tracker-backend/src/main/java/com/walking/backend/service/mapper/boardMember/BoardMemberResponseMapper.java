package com.walking.backend.service.mapper.boardMember;

import com.walking.backend.domain.dto.boardMember.BoardMemberResponse;
import com.walking.backend.domain.model.BoardMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BoardMemberResponseMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "email", source = "user.email")
    BoardMemberResponse toDto(BoardMember boardMember);
}
