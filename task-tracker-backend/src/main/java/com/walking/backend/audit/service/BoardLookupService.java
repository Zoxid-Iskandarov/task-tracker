package com.walking.backend.audit.service;

import com.walking.backend.domain.projection.BoardInfo;
import com.walking.backend.props.CacheNames;
import com.walking.backend.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BoardLookupService {
    private final BoardRepository boardRepository;

    @Cacheable(value = CacheNames.BOARD_INFO, key = "#id")
    public BoardInfo getBoardInfoById(Long id) {
        BoardInfo boardInfo = boardRepository.findBoardInfoById(id);

        if (boardInfo == null) {
            return new BoardInfo(id, "Unknown Board");
        }

        return boardInfo;
    }

    @Cacheable(value = CacheNames.BOARD_INFO_SECTION, key = "#sectionId")
    public BoardInfo getBoardInfoBySectionId(Long sectionId) {
        BoardInfo boardInfo = boardRepository.findBoardInfoBySectionId(sectionId);

        if (boardInfo == null) {
            return new BoardInfo(sectionId, "Unknown Board");
        }

        return boardInfo;
    }
}
