package com.walking.backend.audit.service;

import com.walking.backend.domain.projection.BoardInfo;
import com.walking.backend.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BoardLookupService {
    private final BoardRepository boardRepository;

    public BoardInfo getBoardInfoById(Long id) {
        BoardInfo boardInfo = boardRepository.findBoardInfoById(id);

        if (boardInfo == null) {
            return new BoardInfo(id, "Unknown Board");
        }

        return boardInfo;
    }

    public BoardInfo getBoardInfoBySectionId(Long sectionId) {
        BoardInfo boardInfo = boardRepository.findBoardInfoBySectionId(sectionId);

        if (boardInfo == null) {
            return new BoardInfo(sectionId, "Unknown Board");
        }

        return boardInfo;
    }
}
