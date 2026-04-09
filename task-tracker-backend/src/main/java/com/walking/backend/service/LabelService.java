package com.walking.backend.service;

import com.walking.backend.domain.dto.label.CreateLabelRequest;
import com.walking.backend.domain.dto.label.LabelResponse;
import com.walking.backend.domain.dto.label.UpdateLabelRequest;
import com.walking.backend.domain.model.Label;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface LabelService {

    List<LabelResponse> getLabels(Long boardId);

    Label getLabelById(Long labelId);

    LabelResponse createLabel(CreateLabelRequest createLabelRequest);

    LabelResponse updateLabel(UpdateLabelRequest updateLabelRequest, Long labelId);

    void deleteLabel(Long labelId);
}
