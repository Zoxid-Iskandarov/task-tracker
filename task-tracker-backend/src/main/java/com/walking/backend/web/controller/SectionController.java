package com.walking.backend.web.controller;

import com.walking.backend.domain.dto.section.CreateSectionRequest;
import com.walking.backend.domain.dto.section.SectionResponse;
import com.walking.backend.domain.dto.section.UpdateSectionRequest;
import com.walking.backend.service.SectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sections")
@RequiredArgsConstructor
public class SectionController {
    private final SectionService sectionService;

    @GetMapping("/{boardId}")
    public Page<SectionResponse> getSections(@PathVariable Long boardId, @PageableDefault(30) Pageable pageable) {
        return sectionService.getSections(boardId, pageable);
    }

    @PostMapping
    public ResponseEntity<?> createSection(@RequestBody CreateSectionRequest createSectionRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sectionService.createSection(createSectionRequest));
    }

    @PutMapping("/{sectionId}")
    public SectionResponse updateSection(@RequestBody UpdateSectionRequest updateSectionRequest,
                                         @PathVariable Long sectionId) {
        return sectionService.updateSection(updateSectionRequest, sectionId);
    }

    @DeleteMapping("/{sectionId}")
    public ResponseEntity<?> deleteSection(@PathVariable Long sectionId) {
        sectionService.deleteSection(sectionId);

        return ResponseEntity.noContent()
                .build();
    }
}
