package com.walking.backend.service;

import com.walking.backend.domain.dto.section.CreateSectionRequest;
import com.walking.backend.domain.dto.section.SectionResponse;
import com.walking.backend.domain.dto.section.UpdateSectionRequest;
import com.walking.backend.domain.model.Section;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SectionService {

    Page<SectionResponse> getSections(Long boardId, Pageable pageable);

    Section getProxySectionById(Long sectionId);

    SectionResponse createSection(CreateSectionRequest createSectionRequest);

    SectionResponse updateSection(UpdateSectionRequest updateSectionRequest, Long sectionId);

    void deleteSection(Long sectionId);
}
