package com.walking.backend.service.mapper.board;

import com.walking.backend.domain.dto.board.BoardResponse;
import com.walking.backend.domain.model.Board;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BoardResponseMapper {

    BoardResponse toDto(Board board);
}
