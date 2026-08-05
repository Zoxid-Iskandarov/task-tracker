package com.walking.backend.integration.service;

import com.walking.backend.audit.service.ActivityService;
import com.walking.backend.domain.dto.label.CreateLabelRequest;
import com.walking.backend.domain.dto.label.LabelResponse;
import com.walking.backend.domain.dto.label.UpdateLabelRequest;
import com.walking.backend.domain.exception.DuplicateException;
import com.walking.backend.domain.exception.LabelLimitExceededException;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.domain.model.Label;
import com.walking.backend.integration.IntegrationTestBase;
import com.walking.backend.integration.annotation.WithMockUser;
import com.walking.backend.props.AppProperties;
import com.walking.backend.repository.LabelRepository;
import com.walking.backend.service.LabelService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static com.walking.backend.domain.model.ActivityType.LABEL_DELETED;
import static com.walking.backend.domain.model.ActivityType.LABEL_UPDATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@WithMockUser
@RequiredArgsConstructor
public class LabelServiceIT extends IntegrationTestBase {
    private final LabelService labelService;
    private final LabelRepository labelRepository;
    private final ActivityService activityService;
    private final AppProperties appProperties;

    @Test
    void getLabels_whenBoardExistsAndUserHasAccess_shouldReturnLabelsList() {
        List<LabelResponse> actual = labelService.getLabels(1L, null);

        assertThat(actual).isNotEmpty();
        assertThat(actual)
                .extracting(LabelResponse::name)
                .contains("Bug");
    }

    @Test
    void getLabels_whenNameFilterProvided_shouldReturnFilteredLabels() {
        List<LabelResponse> actual = labelService.getLabels(1L, "bug");

        assertThat(actual).hasSize(1);
        assertThat(actual.getFirst().name()).isEqualTo("Bug");
    }

    @Test
    void getLabels_whenNoLabelsMatchFilter_shouldReturnEmptyList() {
        List<LabelResponse> actual = labelService.getLabels(1L, "nonexistent");

        assertThat(actual).isEmpty();
    }

    @Test
    @WithMockUser(id = 99L, username = "stranger")
    void getLabels_whenUserHasNoAccess_shouldThrowAccessDeniedException() {
        assertThatThrownBy(() -> labelService.getLabels(1L, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getLabelById_whenLabelExists_shouldReturnLabel() {
        Label label = labelService.getLabelById(1L);

        assertThat(label).isNotNull();
        assertThat(label.getId()).isEqualTo(1L);
        assertThat(label.getName()).isEqualTo("Bug");
    }

    @Test
    void getLabelById_whenLabelNotFound_shouldThrowObjectNotFoundException() {
        assertThatThrownBy(() -> labelService.getLabelById(99L))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessage("Label with id 99 not found");
    }

    @Test
    void createLabel_whenValidRequestAndUserCanManageBoard_shouldCreateLabel() {
        var request = new CreateLabelRequest("Feature", "BLUE", 1L);

        LabelResponse response = labelService.createLabel(request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Feature");
        assertThat(response.colour()).isEqualTo("BLUE");

        Optional<Label> saved = labelRepository.findById(response.id());
        assertThat(saved).isPresent();
        assertThat(saved.get().getName()).isEqualTo("Feature");
    }

    @Test
    void createLabel_whenLabelLimitExceeded_shouldThrowLabelLimitExceededException() {
        for (int i = 0; i < appProperties.getLabel().getMaxPerBoard() - 1; i++) {
            labelService.createLabel(new CreateLabelRequest("Label " + i, "BLUE", 1L));
        }

        var request = new CreateLabelRequest("One More", "GREEN", 1L);

        assertThatThrownBy(() -> labelService.createLabel(request))
                .isInstanceOf(LabelLimitExceededException.class)
                .hasMessageContaining("Board cannot contain more than");
    }

    @Test
    void createLabel_whenLabelAlreadyExists_shouldThrowDuplicateException() {
        var request = new CreateLabelRequest("Bug", "GREEN", 1L); // "Bug" already exists in board 1

        assertThatThrownBy(() -> labelService.createLabel(request))
                .isInstanceOf(DuplicateException.class)
                .hasMessage("Label Bug already exists in this board");
    }

    @Test
    @WithMockUser(id = 3L, username = "john_snow") // EDITOR on board 1 (only OWNER can manage board)
    void createLabel_whenUserCannotManageBoard_shouldThrowAccessDeniedException() {
        var request = new CreateLabelRequest("Feature", "BLUE", 1L);

        assertThatThrownBy(() -> labelService.createLabel(request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateLabel_whenValidRequestAndUserCanManageLabel_shouldUpdateLabelAndPublishActivity() {
        var request = new UpdateLabelRequest("Critical Bug", "DARK_RED");

        LabelResponse response = labelService.updateLabel(request, 1L);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Critical Bug");
        assertThat(response.colour()).isEqualTo("DARK_RED");

        Label updated = labelRepository.findById(1L).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Critical Bug");

        verify(activityService).publish(any(), eq(LABEL_UPDATED), eq("Renamed label from Bug to Critical Bug"));
    }

    @Test
    @WithMockUser(id = 3L, username = "john_snow") // EDITOR on board 1 (only OWNER can manage board)
    void updateLabel_whenUserCannotManageLabel_shouldThrowAccessDeniedException() {
        var request = new UpdateLabelRequest("New Name", "BLUE");

        assertThatThrownBy(() -> labelService.updateLabel(request, 1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateLabel_whenLabelNotFound_shouldThrowAccessDenied() {
        var request = new UpdateLabelRequest("New Name", "BLUE");

        assertThatThrownBy(() -> labelService.updateLabel(request, 99L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateLabel_whenNameAlreadyExists_shouldThrowDuplicateException() {
        labelService.createLabel(new CreateLabelRequest("Feature", "BLUE", 1L));

        var request = new UpdateLabelRequest("Feature", "RED");

        assertThatThrownBy(() -> labelService.updateLabel(request, 1L))
                .isInstanceOf(DuplicateException.class)
                .hasMessage("Label Feature already exists in this board");
    }

    @Test
    void updateLabel_whenOnlyColourChanged_shouldPublishUpdatedActivity() {
        var request = new UpdateLabelRequest("Bug", "GREEN");

        LabelResponse response = labelService.updateLabel(request, 1L);

        assertThat(response.colour()).isEqualTo("GREEN");

        verify(activityService).publish(any(), eq(LABEL_UPDATED), eq("Updated label Bug"));
    }

    @Test
    void deleteLabel_whenLabelExistsAndUserCanManageLabel_shouldDeleteLabelAndPublishActivity() {
        labelService.deleteLabel(1L);

        assertThat(labelRepository.findById(1L)).isEmpty();

        verify(activityService).publish(any(), eq(LABEL_DELETED), eq("Deleted label Bug"));
    }

    @Test
    @WithMockUser(id = 3L, username = "john_snow")
    void deleteLabel_whenUserCannotManageLabel_shouldThrowAccessDeniedException() {
        assertThatThrownBy(() -> labelService.deleteLabel(1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteLabel_whenLabelNotFound_shouldThrowAccessDenied() {
        assertThatThrownBy(() -> labelService.deleteLabel(99L))
                .isInstanceOf(AccessDeniedException.class);
    }
}
