package com.walking.backend.service.mapper.section;

import com.walking.backend.domain.dto.section.SectionResponse;
import com.walking.backend.domain.model.Section;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SectionResponseMapper {

    @Mapping(target = "boardId", source = "board.id")
    SectionResponse toDto(Section section);
}
