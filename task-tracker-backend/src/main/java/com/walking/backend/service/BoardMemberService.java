package com.walking.backend.service;

import com.walking.backend.domain.dto.boardMember.BoardMemberFilter;
import com.walking.backend.domain.dto.boardMember.BoardMemberRequest;
import com.walking.backend.domain.dto.boardMember.BoardMemberResponse;
import com.walking.backend.domain.model.BoardMember;
import com.walking.backend.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BoardMemberService {

    Page<BoardMemberResponse> getMembers(Long boardId, BoardMemberFilter boardMemberFilter, Pageable pageable);

    BoardMember getById(Long boardId, Long userId);

    BoardMemberResponse addMember(Long boardId, BoardMemberRequest boardMemberRequest, CustomUserDetails userDetails);

    void removeMember(Long boardId, Long userId, Long currentUserId);
    
    BoardMemberResponse changeRole(Long boardId, BoardMemberRequest boardMemberRequest, Long currentUserId);

    void leaveBoard(Long boardId, Long currentUserId);
}
