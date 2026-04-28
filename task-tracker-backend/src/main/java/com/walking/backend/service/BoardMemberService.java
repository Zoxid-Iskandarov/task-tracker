package com.walking.backend.service;

import com.walking.backend.domain.dto.boardMember.BoardMemberRequest;
import com.walking.backend.domain.dto.boardMember.BoardMemberResponse;
import com.walking.backend.domain.model.BoardMember;
import com.walking.backend.security.CustomUserDetails;

public interface BoardMemberService {

    BoardMember getById(Long boardId, Long userId);

    BoardMemberResponse addMember(Long boardId, BoardMemberRequest boardMemberRequest, CustomUserDetails userDetails);

    void removeMember(Long boardId, Long userId, Long currentUserId);
    
    BoardMemberResponse changeRole(Long boardId, BoardMemberRequest boardMemberRequest, Long currentUserId);

    void leaveBoard(Long boardId, Long currentUserId);
}
