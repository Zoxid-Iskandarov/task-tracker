package com.walking.backend.repository;

import com.walking.backend.domain.dto.user.UserProfileResponse;
import com.walking.backend.domain.dto.user.UserPublicProfileResponse;
import com.walking.backend.domain.dto.user.UserShortResponse;
import com.walking.backend.domain.model.UserProfile;
import com.walking.backend.domain.projection.TaskAssigneeProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    @Query("""
            select new com.walking.backend.domain.dto.user.UserShortResponse(
                        u.id,
                        u.username,
                        p.displayName,
                        p.avatarUrl)
            from UserProfile p
                    join p.user u
                where u.id in :userIds
            """)
    List<UserShortResponse> findUserShortsByIds(Set<Long> userIds);

    @Query("""
            select new com.walking.backend.domain.dto.user.UserShortResponse(
                        u.id,
                        u.username,
                        p.displayName,
                        p.avatarUrl)
            from UserProfile p
                    join p.user u
                where u.id = :userId
            """)
    UserShortResponse findUserShortById(Long userId);

    @Query("""
            select new com.walking.backend.domain.projection.TaskAssigneeProjection(
                        t.id,
                        u.id,
                        u.username,
                        p.displayName,
                        p.avatarUrl)
            from Task t
                    join t.assignees u
                    join UserProfile p on p.user.id = u.id
                where t.id in :taskIds
            """)
    List<TaskAssigneeProjection> findAssigneeProjectionByTaskIds(Set<Long> taskIds);

    @Query("""
            select new com.walking.backend.domain.dto.user.UserProfileResponse(
                        u.id,
                        u.username,
                        u.email,
                        p.displayName,
                        p.avatarUrl,
                        p.bio)
            from User u
                    join UserProfile p on u.id = p.userId
                where u.id = :userId
            """)
    Optional<UserProfileResponse> findUserProfileByUserId(Long userId);

    @Query("""
            select new com.walking.backend.domain.dto.user.UserPublicProfileResponse(
                        u.id,
                        u.username,
                        p.displayName,
                        p.avatarUrl,
                        p.bio)
            from User u
                    join UserProfile p on u.id = p.userId
                where u.id = :userId
            """)
    Optional<UserPublicProfileResponse> findUserPublicProfileByUserId(Long userId);
}
