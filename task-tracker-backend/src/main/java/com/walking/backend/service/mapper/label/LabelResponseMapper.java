package com.walking.backend.service.mapper.label;

import com.walking.backend.domain.dto.label.LabelResponse;
import com.walking.backend.domain.model.Label;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface LabelResponseMapper {

    @Mapping(target = "boardId", source = "board.id")
    LabelResponse toDto(Label label);
}
