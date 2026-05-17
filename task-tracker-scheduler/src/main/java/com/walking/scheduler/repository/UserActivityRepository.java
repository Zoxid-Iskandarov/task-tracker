package com.walking.scheduler.repository;

import com.walking.scheduler.domain.model.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

    @Query(value = """
            select distinct email
            from task_tracker_scheduler_db.public.user_activity
            where is_processed = false
            order by email
            limit :limit
            """, nativeQuery = true)
    List<String> findUnprocessedEmails(int limit);

    List<UserActivity> findAllByEmailInAndIsProcessedFalse(List<String> emails);

    @Modifying
    @Query("""
            update UserActivity u
            set u.isProcessed = true
            where u.id in :ids
            """)
    void markAsProcessed(List<Long> ids);
}
