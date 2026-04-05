package com.walking.backend.service.mapper.section;

import com.walking.backend.domain.dto.section.CreateSectionRequest;
import com.walking.backend.domain.model.Section;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CreateSectionRequestMapper {

    Section toEntity(CreateSectionRequest createSectionRequest);
}
