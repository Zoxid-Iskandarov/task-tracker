package com.walking.backend.web.controller;

import com.walking.backend.domain.dto.label.CreateLabelRequest;
import com.walking.backend.domain.dto.label.LabelResponse;
import com.walking.backend.domain.dto.label.UpdateLabelRequest;
import com.walking.backend.service.LabelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/labels")
@RequiredArgsConstructor
public class LabelController {
    private final LabelService labelService;

    @PostMapping
    public ResponseEntity<LabelResponse> createLabel(@RequestBody @Validated CreateLabelRequest createLabelRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(labelService.createLabel(createLabelRequest));
    }

    @PutMapping("/{labelId}")
    public LabelResponse updateLabel(
            @RequestBody @Validated UpdateLabelRequest updateLabelRequest,
            @PathVariable Long labelId) {
        return labelService.updateLabel(updateLabelRequest, labelId);
    }

    @DeleteMapping("/{labelId}")
    public ResponseEntity<?> deleteLabel(@PathVariable Long labelId) {
        labelService.deleteLabel(labelId);

        return ResponseEntity.noContent().build();
    }
}
