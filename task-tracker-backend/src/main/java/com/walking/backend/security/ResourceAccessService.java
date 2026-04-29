package com.walking.backend.security;

import com.walking.backend.domain.model.BoardRole;
import com.walking.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.walking.backend.domain.model.BoardRole.EDITOR;
import static com.walking.backend.domain.model.BoardRole.OWNER;

@Component
@RequiredArgsConstructor
public class ResourceAccessService {
    private final BoardMemberRepository boardMemberRepository;
    private final SectionRepository sectionRepository;
    private final TaskRepository taskRepository;
    private final LabelRepository labelRepository;

    public boolean canUseLabel(Long labelId, Long userId) {
        return labelRepository.existsByLabelIdAndUserIdAndRoles(labelId, userId, List.of(OWNER, EDITOR));
    }

    public boolean canManageLabel(Long labelId, Long userId) {
        return labelRepository.existsByLabelIdAndUserIdAndRoles(labelId, userId, List.of(OWNER));
    }

    public boolean canEditTask(Long taskId, Long userId) {
        return taskRepository.existsByTaskIdAndUserIdAndRoles(taskId, userId, List.of(OWNER, EDITOR));
    }

    public boolean canViewTask(Long taskId, Long userId) {
        return taskRepository.existsByTaskIdAndUserId(taskId, userId);
    }

    public boolean canViewSection(Long sectionId, Long userId) {
        return sectionRepository.existsBySectionIdAndUserId(sectionId, userId);
    }

    public boolean canEditSection(Long sectionId, Long userId) {
        return sectionRepository.existsBySectionIdAndUserIdAndRoles(sectionId, userId, List.of(OWNER, EDITOR));
    }

    public boolean canViewBoard(Long boardId, Long userId) {
        return boardMemberRepository.existsByIdBoardIdAndIdUserId(boardId, userId);
    }

    public boolean canManageBoard(Long boardId, Long userId) {
        return hasBoardRole(boardId, userId, OWNER);
    }

    public boolean canEditBoard(Long boardId, Long userId) {
        return hasBoardRole(boardId, userId, OWNER, EDITOR);
    }

    private boolean hasBoardRole(Long boardId, Long userId, BoardRole... roles) {
        return boardMemberRepository.existsByIdBoardIdAndIdUserIdAndRoleIn(boardId, userId, List.of(roles));
    }
}
