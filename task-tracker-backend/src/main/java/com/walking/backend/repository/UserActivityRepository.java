package com.walking.backend.repository;

import com.walking.backend.domain.dto.activity.BoardActivityResponse;
import com.walking.backend.domain.model.UserActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

    @Query("""
            select new com.walking.backend.domain.dto.activity.BoardActivityResponse(
                        a.userId,
                        a.username,
                        p.displayName,
                        p.avatarUrl,
                        a.activityType,
                        a.description,
                        a.created)
            from UserActivity a
                left join UserProfile p on a.userId = p.userId
                where a.boardId = :boardId
            """)
    Page<BoardActivityResponse> findAllByBoardId(Long boardId, Pageable pageable);

    Page<UserActivity> findAllByUserId(Long userId, Pageable pageable);
}
