package com.walking.backend.repository;

import com.walking.backend.domain.model.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long>, JpaSpecificationExecutor<Board> {

    boolean existsBoardByNameAndUserIdAndIdNot(String name, Long userId, Long boarId);

    boolean existsBoardByNameAndUserId(String name, Long userId);

    boolean existsBoardByIdAndUserId(Long boardId, Long userId);
}
