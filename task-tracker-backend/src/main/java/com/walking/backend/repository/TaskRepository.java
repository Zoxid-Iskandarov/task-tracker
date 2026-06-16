package com.walking.backend.repository;

import com.walking.backend.domain.model.BoardRole;
import com.walking.backend.domain.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    @Query("""
            select t from Task t
                        left join fetch t.labels
                                    where t.id = :taskId
            """)
    Optional<Task> findByIdWithLabels(Long taskId);

    @Override
    @EntityGraph(attributePaths = {"labels"})
    Page<Task> findAll(Specification<Task> spec, Pageable pageable);

    @Query("select max(t.position) from Task t where t.section.id = :sectionId")
    Double findMaxPositionBySectionId(Long sectionId);

    Optional<Task> findByIdAndSectionId(Long taskId, Long sectionId);

    List<Task> findAllBySectionIdOrderByPositionAsc(Long sectionId);

    @Query("""
            select (count (t) > 0) from Task t
                        join t.section s
                        join s.board b
                        join b.members m
                                    where t.id = :taskId and m.user.id = :userId
            """)
    boolean existsByTaskIdAndUserId(Long taskId, Long userId);

    @Query("""
            select (count (t) > 0) from Task t
                        join t.section s
                        join s.board b
                        join b.members m
                                    where t.id = :taskId and m.user.id = :userId and m.role in :roles
            """)
    boolean existsByTaskIdAndUserIdAndRoles(Long taskId, Long userId, List<BoardRole> roles);

    @Query(value = """
            SELECT COUNT(*) > 0
            FROM task_assignee
                WHERE task_id = :taskId AND user_id = :userId
            """, nativeQuery = true)
    boolean existsAssigneeByTaskIdAndUserId(Long taskId, Long userId);

    @Modifying
    @Query(value = """
            DELETE FROM task_assignee
                WHERE user_id = :userId
                    AND task_id IN (
                        SELECT t.id
                        FROM task t
                                JOIN section s ON t.section_id = s.id
                            WHERE s.board_id = :boardId)
            """, nativeQuery = true)
    void removeAssigneeFromBoardTasks(Long boardId, Long userId);
}
