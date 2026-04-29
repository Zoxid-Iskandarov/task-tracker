package com.walking.backend.repository;

import com.walking.backend.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("""
            select u from User u
            where (lower(u.username) like lower(concat('%', :query, '%'))
                        or lower(u.email) like lower(concat('%', :query, '%')))
                                and u.id not in (select m.user.id from BoardMember m where m.board.id = :boardId)
            """)
    Page<User> searchUsersByQueryAndExcludeBoardMembers(String query, Long boardId, Pageable pageable);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);
}
