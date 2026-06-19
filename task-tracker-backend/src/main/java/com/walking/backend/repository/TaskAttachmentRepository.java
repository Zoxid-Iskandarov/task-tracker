package com.walking.backend.repository;

import com.walking.backend.domain.model.TaskAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, Long> {

    List<TaskAttachment> findAllByTaskId(Long taskId);

    long countByTaskId(Long taskId);

    @Query("select ta.filePath from TaskAttachment ta where ta.task.section.id = :sectionId")
    List<String> findAllFilePathBySectionId(Long sectionId);

    @Query("select ta.filePath from TaskAttachment ta where ta.task.section.board.id = :boardId")
    List<String> findAllFilePathByBoardId(Long boardId);
}
