package com.walking.backend.repository;

import com.walking.backend.domain.dto.user.UserShortResponse;
import com.walking.backend.domain.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
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

}
