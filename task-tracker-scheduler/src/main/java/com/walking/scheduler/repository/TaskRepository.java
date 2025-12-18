package com.walking.scheduler.repository;

import com.walking.scheduler.domain.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("""
            select t from Task t 
                        join fetch t.user 
                                    where t.user.id in :userIds 
                                                and (t.isCompleted = false 
                                                or (t.isCompleted = true and t.updated >= :since))
            """)
    List<Task> findRelevantForUsers(@Param("userIds") List<Long> userIds, @Param("since") LocalDateTime since);
}
