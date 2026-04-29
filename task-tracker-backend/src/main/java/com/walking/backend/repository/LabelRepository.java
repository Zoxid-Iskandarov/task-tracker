package com.walking.backend.repository;

import com.walking.backend.domain.model.BoardRole;
import com.walking.backend.domain.model.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabelRepository extends JpaRepository<Label, Long> {

    List<Label> findAllByBoardId(Long boardId);

    List<Label> findAllByBoardIdAndNameContainingIgnoreCase(Long boardId, String name);

    boolean existsByNameAndBoardId(String name, Long boardId);

    boolean existsByNameAndBoardIdAndIdNot(String name, Long boardId, Long labelId);

    long countByBoardId(Long boardId);

    @Query("""
            select (count (l) > 0) from Label l
                        join l.board b
                        join b.members m
                                    where l.id = :labelId and m.user.id = :userId and m.role in :roles
            """)
    boolean existsByLabelIdAndUserIdAndRoles(Long labelId, Long userId, List<BoardRole> roles);
}
