package com.walking.backend.repository;

import com.walking.backend.domain.dto.user.UserProfileResponse;
import com.walking.backend.domain.dto.user.UserPublicProfileResponse;
import com.walking.backend.domain.dto.user.UserSearchResponse;
import com.walking.backend.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("""
            select new com.walking.backend.domain.dto.user.UserSearchResponse(
                        u.id,
                        u.username,
                        p.avatarUrl)
            from User u
                left join UserProfile p on u.id = p.userId
                where (lower(u.username) like lower(concat(:query, '%'))
                            or  lower(u.email) like lower(concat(:query, '%')))
                            and u.id not in (
                                        select m.user.id
                                        from BoardMember m
                                            where m.board.id = :boardId)
            """)
    Page<UserSearchResponse> searchUsersByQueryAndExcludeBoardMembers(String query, Long boardId, Pageable pageable);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    @Query("""
            select new com.walking.backend.domain.dto.user.UserProfileResponse(
                        u.id,
                        u.username,
                        u.email,
                        p.displayName,
                        p.avatarUrl,
                        p.bio)
            from User u
                left join UserProfile p on u.id = p.userId
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
                left join UserProfile p on u.id = p.userId
                where u.id = :userId
            """)
    Optional<UserPublicProfileResponse> findUserPublicProfileByUserId(Long userId);

    @Query("""
            select u from Section s
                    join s.board b
                    join b.members m
                    join m.user u
                where s.id = :sectionId and u.id in :assigneeIds
            """)
    Set<User> findAllBySectionIdAndAssigneeIds(Long sectionId, Set<Long> assigneeIds);
}
