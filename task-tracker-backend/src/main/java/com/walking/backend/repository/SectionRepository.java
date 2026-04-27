package com.walking.backend.repository;

import com.walking.backend.domain.model.BoardRole;
import com.walking.backend.domain.model.Section;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {

    Page<Section> findAllByBoardId(Long boardId, Pageable pageable);

    boolean existsSectionByNameAndBoardId(String name, Long boardId);

    boolean existsByNameAndBoardIdAndIdNot(String name, Long boardId, Long sectionId);

    @Query("""
            select (count (s) > 0) from Section s
                        join s.board b
                        join b.members m
                                    where s.id = :sectionId and m.user.id = :userId and m.role in :roles
            """)
    boolean existsBySectionIdAndUserIdAndRoles(Long sectionId, Long userId, List<BoardRole> roles);

    @Query("""
            select (count (s) > 0) from Section s
                        join s.board b
                        join b.members m
                                    where s.id = :sectionId and m.user.id = :userId
            """)
    boolean existsBySectionIdAndUserId(Long sectionId, Long userId);
}
