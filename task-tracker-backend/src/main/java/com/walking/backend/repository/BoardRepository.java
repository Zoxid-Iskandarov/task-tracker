package com.walking.backend.repository;

import com.walking.backend.domain.model.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    @Query("""
            select distinct b from Board b
                        join b.members m
                                    where m.user.id = :userId
            """)
    Page<Board> findAllByUserId(Long userId, Pageable pageable);
}
