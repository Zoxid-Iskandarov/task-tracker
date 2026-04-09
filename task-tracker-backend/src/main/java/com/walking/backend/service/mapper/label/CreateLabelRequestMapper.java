package com.walking.backend.service.mapper.label;

import com.walking.backend.domain.dto.label.CreateLabelRequest;
import com.walking.backend.domain.model.Label;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CreateLabelRequestMapper {

    Label toEntity(CreateLabelRequest createLabelRequest);
}
