package com.walking.backend.repository;

import com.walking.backend.domain.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    boolean existsByIdAndSectionBoardUserId(Long taskId, Long userId);

    @Query("""
            select t from Task t
                        left join fetch t.labels
                                    where t.id = :taskId
            """)
    Optional<Task> findByIdWithLabels(Long taskId);
}
