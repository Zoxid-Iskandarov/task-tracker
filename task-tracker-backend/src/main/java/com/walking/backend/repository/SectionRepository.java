package com.walking.backend.repository;

import com.walking.backend.domain.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long>, JpaSpecificationExecutor<Section> {

    boolean existsSectionByNameAndBoardId(String name, Long boardId);

    boolean existsSectionByIdAndBoardUserId(Long sectionId, Long userId);

    boolean existsByNameAndBoardIdAndIdNot(String name, Long boardId, Long sectionId);
}
