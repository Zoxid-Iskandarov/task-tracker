package com.walking.backend.service.impl;

import com.walking.backend.audit.annotation.TrackActivity;
import com.walking.backend.audit.service.ActivityService;
import com.walking.backend.domain.dto.board.BoardRequest;
import com.walking.backend.domain.dto.board.BoardResponse;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.domain.model.Board;
import com.walking.backend.domain.model.BoardMember;
import com.walking.backend.domain.model.BoardRole;
import com.walking.backend.domain.model.User;
import com.walking.backend.repository.BoardRepository;
import com.walking.backend.repository.TaskAttachmentRepository;
import com.walking.backend.service.BoardService;
import com.walking.backend.service.UserService;
import com.walking.backend.service.mapper.board.BoardRequestMapper;
import com.walking.backend.service.mapper.board.BoardResponseMapper;
import com.walking.backend.storage.service.ResourceCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.walking.backend.domain.model.ActivityType.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {
    private final BoardRepository boardRepository;
    private final TaskAttachmentRepository taskAttachmentRepository;
    private final UserService userService;
    private final ActivityService activityService;
    private final ResourceCleanupService resourceCleanupService;
    private final BoardRequestMapper boardRequestMapper;
    private final BoardResponseMapper boardResponseMapper;

    @Override
    public Page<BoardResponse> getBoards(Long userId, Pageable pageable) {
        return boardRepository.findAllByUserId(userId, pageable)
                .map(boardResponseMapper::toDto);
    }

    @Override
    public Board getProxyBoardById(Long boardId) {
        return boardRepository.getReferenceById(boardId);
    }

    @Override
    @Transactional
    @TrackActivity(type = BOARD_CREATED, description = "'Created board ' + #result.name")
    public BoardResponse createBoard(BoardRequest boardRequest, Long userId) {
        Board board = boardRequestMapper.toEntity(boardRequest);
        User user = userService.getProxyUserById(userId);

        BoardMember member = new BoardMember(board, user, BoardRole.OWNER);
        board.getMembers().add(member);

        Board savedBoard = boardRepository.save(board);

        return boardResponseMapper.toDto(savedBoard);
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.canManageBoard(#boardId, principal.id)")
    public BoardResponse updateBoard(BoardRequest boardRequest, Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ObjectNotFoundException("Board with id %d not found".formatted(boardId)));

        String oldName = board.getName();
        String newName = boardRequest.name();

        board.setName(newName);

        Board updatedBoard = boardRepository.save(board);

        activityService.publish(updatedBoard, BOARD_UPDATED, "Renamed board from %s to %s".formatted(oldName, newName));

        return boardResponseMapper.toDto(updatedBoard);
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.canManageBoard(#boardId, principal.id)")
    public void deleteBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ObjectNotFoundException("Board with id %d not found".formatted(boardId)));

        List<String> filePaths = taskAttachmentRepository.findAllFilePathByBoardId(boardId);

        boardRepository.delete(board);

        activityService.publish(board, BOARD_DELETED, "Deleted board %s".formatted(board.getName()));
        resourceCleanupService.cleanupFiles(filePaths);
    }
}
