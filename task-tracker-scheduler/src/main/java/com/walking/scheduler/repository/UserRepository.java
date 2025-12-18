package com.walking.scheduler.repository;

import com.walking.scheduler.domain.model.User;
import com.walking.scheduler.domain.projection.UserProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("""
            select distinct new com.walking.scheduler.domain.projection.UserProjection(u.id, u.username, u.email)
                        from User u 
                                    join Task t on u.id = t.user.id 
                                                where t.isCompleted = false 
                                                            or (t.isCompleted = true and t.updated >= :since)
            """)
    Page<UserProjection> findUserProjectionsForDailyReport(@Param("since") LocalDateTime since, Pageable pageable);
}
