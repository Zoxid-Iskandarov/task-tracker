package com.walking.backend.integration.service;

import com.walking.backend.audit.service.ActivityService;
import com.walking.backend.domain.dto.section.CreateSectionRequest;
import com.walking.backend.domain.dto.section.SectionResponse;
import com.walking.backend.domain.dto.section.UpdateSectionRequest;
import com.walking.backend.domain.exception.DuplicateException;
import com.walking.backend.domain.model.Section;
import com.walking.backend.integration.IntegrationTestBase;
import com.walking.backend.integration.annotation.WithMockUser;
import com.walking.backend.repository.SectionRepository;
import com.walking.backend.service.SectionService;
import com.walking.backend.storage.service.ResourceCleanupService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static com.walking.backend.domain.model.ActivityType.SECTION_DELETED;
import static com.walking.backend.domain.model.ActivityType.SECTION_UPDATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@WithMockUser
@RequiredArgsConstructor
public class SectionServiceIT extends IntegrationTestBase {
    private final SectionService sectionService;
    private final ResourceCleanupService resourceCleanupService;
    private final ActivityService activityService;

    private final SectionRepository sectionRepository;

    @Test
    void getSections_whenBoardExistsAndUserHasAccess_shouldReturnSectionsPage() {
        var pageable = PageRequest.of(0, 10);

        Page<SectionResponse> actual = sectionService.getSections(1L, pageable);

        assertThat(actual.getContent()).isNotEmpty();
        assertThat(actual.getContent())
                .extracting(SectionResponse::name)
                .contains("To Do");
    }

    @Test
    @WithMockUser(id = 99L, username = "stranger")
    void getSections_whenUserHasNoAccess_shouldThrowAccessDeniedException() {
        var pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> sectionService.getSections(1L, pageable))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void createSection_whenValidRequestAndUserCanEdit_shouldCreateSection() {
        var request = new CreateSectionRequest("Backlog", 1L);

        SectionResponse response = sectionService.createSection(request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Backlog");

        Optional<Section> saved = sectionRepository.findById(response.id());
        assertThat(saved).isPresent();
        assertThat(saved.get().getName()).isEqualTo("Backlog");
    }

    @Test
    void createSection_whenSectionAlreadyExists_shouldThrowDuplicateException() {
        var request = new CreateSectionRequest("To Do", 1L); // "To Do" already exists in board 1 from data.sql

        assertThatThrownBy(() -> sectionService.createSection(request))
                .isInstanceOf(DuplicateException.class)
                .hasMessage("Section To Do already exists in this board");
    }

    @Test
    @WithMockUser(id = 3L, username = "john_snow") // EDITOR on board 1 (can edit board)
    void createSection_whenUserCanEditBoard_shouldSucceed() {
        var request = new CreateSectionRequest("Testing Section", 1L);

        SectionResponse response = sectionService.createSection(request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Testing Section");
    }

    @Test
    @WithMockUser(id = 1L, username = "john_doe") // VIEWER on board 2, no access to board 1
    void createSection_whenUserCannotEditBoard_shouldThrowAccessDeniedException() {
        var request = new CreateSectionRequest("New Section", 1L);

        assertThatThrownBy(() -> sectionService.createSection(request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateSection_whenValidRequestAndUserCanEdit_shouldUpdateSectionAndPublishActivity() {
        var request = new UpdateSectionRequest("Done");

        SectionResponse response = sectionService.updateSection(request, 1L);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Done");

        Section updated = sectionRepository.findById(1L).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Done");

        verify(activityService).publish(any(), eq(SECTION_UPDATED), eq("Renamed section from To Do to Done"));
    }

    @Test
    void updateSection_whenSectionNotFound_shouldThrowAccessDeniedException() {
        var request = new UpdateSectionRequest("New Name");

        assertThatThrownBy(() -> sectionService.updateSection(request, 99L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateSection_whenNameAlreadyExistsInBoard_shouldThrowDuplicateException() {
        sectionService.createSection(new CreateSectionRequest("Done", 1L));

        var request = new UpdateSectionRequest("Done");

        assertThatThrownBy(() -> sectionService.updateSection(request, 1L))
                .isInstanceOf(DuplicateException.class)
                .hasMessage("Section Done already exists in this board");
    }

    @Test
    @WithMockUser(id = 1L, username = "john_doe")
    void updateSection_whenUserCannotEditSection_shouldThrowAccessDeniedException() {
        var request = new UpdateSectionRequest("New Name");

        assertThatThrownBy(() -> sectionService.updateSection(request, 1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteSection_whenSectionExistsAndUserCanEdit_shouldDeleteSectionAndCleanupFilesAndPublishActivity() {
        sectionService.deleteSection(1L);

        assertThat(sectionRepository.findById(1L)).isEmpty();

        verify(activityService).publish(any(), eq(SECTION_DELETED), eq("Deleted section To Do"));
        verify(resourceCleanupService).cleanupFiles(anyList());
    }

    @Test
    void deleteSection_whenSectionNotFound_shouldThrowAccessDeniedException() {
        assertThatThrownBy(() -> sectionService.deleteSection(99L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(id = 1L, username = "john_doe")
    void deleteSection_whenUserCannotEditSection_shouldThrowAccessDeniedException() {
        assertThatThrownBy(() -> sectionService.deleteSection(1L))
                .isInstanceOf(AccessDeniedException.class);
    }
}
