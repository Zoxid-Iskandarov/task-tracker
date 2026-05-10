package com.walking.backend.repository;

import com.walking.backend.domain.model.UserActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

    Page<UserActivity> findAllByBoardIdOrderByCreatedDesc(Long boardId, Pageable pageable);

    Page<UserActivity> findAllByUserIdOrderByCreatedDesc(Long userId, Pageable pageable);
}
