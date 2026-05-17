package com.walking.scheduler.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_activity")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

    @Column(name = "is_processed", nullable = false)
    private Boolean isProcessed;

    @Column(nullable = false, updatable = false)
    private LocalDateTime created;

    @LastModifiedDate
    private LocalDateTime updated;
}
