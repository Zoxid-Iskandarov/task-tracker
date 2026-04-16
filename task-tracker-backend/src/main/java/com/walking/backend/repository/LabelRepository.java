package com.walking.backend.repository;

import com.walking.backend.domain.model.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabelRepository extends JpaRepository<Label, Long> {

    List<Label> findAllByBoardId(Long boardId);

    List<Label> findAllByBoardIdAndNameContainingIgnoreCase(Long boardId, String name);

    boolean existsByNameAndBoardId(String name, Long boardId);

    boolean existsByNameAndBoardIdAndIdNot(String name, Long boardId, Long labelId);

    boolean existsByIdAndBoardUserId(Long labelId, Long userId);

    long countByBoardId(Long boardId);
}
