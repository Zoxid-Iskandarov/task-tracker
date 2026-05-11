package com.walking.backend.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_activity")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class UserActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false, updatable = false)
    private String username;

    @Column(nullable = false, updatable = false)
    private String email;

    @Column(name = "board_id", nullable = false, updatable = false)
    private Long boardId;

    @Column(name = "board_name", nullable = false, updatable = false)
    private String boardName;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, updatable = false)
    private ActivityType activityType;

    @Column(nullable = false, updatable = false)
    private String description;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime created;
}
