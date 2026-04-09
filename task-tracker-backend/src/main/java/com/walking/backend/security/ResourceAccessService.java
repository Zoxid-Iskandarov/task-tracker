package com.walking.backend.security;

import com.walking.backend.repository.BoardRepository;
import com.walking.backend.repository.LabelRepository;
import com.walking.backend.repository.SectionRepository;
import com.walking.backend.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResourceAccessService {
    private final BoardRepository boardRepository;
    private final SectionRepository sectionRepository;
    private final TaskRepository taskRepository;
    private final LabelRepository labelRepository;

    public boolean isOwnerOfBoard(Long boardId, Long userId) {
        return boardRepository.existsBoardByIdAndUserId(boardId, userId);
    }

    public boolean isOwnerOfSection(Long sectionId, Long userId) {
        return sectionRepository.existsSectionByIdAndBoardUserId(sectionId, userId);
    }

    public boolean isOwnerOfTask(Long taskId, Long userId) {
        return taskRepository.existsByIdAndSectionBoardUserId(taskId, userId);
    }

    public boolean isOwnerOfLabel(Long labelId, Long userId) {
        return labelRepository.existsByIdAndBoardUserId(labelId, userId);
    }
}
