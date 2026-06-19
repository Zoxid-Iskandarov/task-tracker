package com.walking.backend.domain.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityEvent {
    private Long userId;
    private String username;
    private String email;
    private Long boardId;
    private String boardName;
    private String type;
    private String description;
    private LocalDateTime created;
}
