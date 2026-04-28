package com.walking.backend.repository;

import com.walking.backend.domain.model.BoardMember;
import com.walking.backend.domain.model.BoardMemberId;
import com.walking.backend.domain.model.BoardRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardMemberRepository extends JpaRepository<BoardMember, BoardMemberId> {

    boolean existsByIdBoardIdAndIdUserIdAndRoleIn(Long boardId, Long userId, List<BoardRole> roles);

    boolean existsByIdBoardIdAndIdUserId(Long boardId, Long userId);

    Optional<BoardMember> findByIdBoardIdAndIdUserId(Long boardId, Long userId);

    long countByIdBoardIdAndRole(Long boardId, BoardRole role);
}
