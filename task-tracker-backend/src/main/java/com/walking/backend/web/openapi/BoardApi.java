package com.walking.backend.web.openapi;

import com.walking.backend.domain.dto.board.BoardRequest;
import com.walking.backend.domain.dto.board.BoardResponse;
import com.walking.backend.domain.dto.boardMember.BoardMemberFilter;
import com.walking.backend.domain.dto.boardMember.BoardMemberRequest;
import com.walking.backend.domain.dto.boardMember.BoardMemberResponse;
import com.walking.backend.domain.dto.label.LabelResponse;
import com.walking.backend.domain.dto.section.SectionResponse;
import com.walking.backend.domain.dto.task.TaskFilter;
import com.walking.backend.domain.dto.task.TaskPreviewResponse;
import com.walking.backend.domain.dto.user.UserSearchFilter;
import com.walking.backend.domain.dto.user.UserSearchResponse;
import com.walking.backend.security.principal.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Tag(name = "Boards Management", description = "Endpoints for managing collaborative workspace boards, sections, tasks, labels, and board members")
@SecurityRequirement(name = "Bearer Authentication")
public interface BoardApi {

    @Operation(summary = "Get paginated list of user's boards", description = "Retrieves all boards where the currently authenticated user is either an owner or a member.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of boards retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    Page<BoardResponse> getBoards(
            @ParameterObject Pageable pageable,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "Get all sections for a specific board", description = "Retrieves a paginated list of vertical columns (sections) inside the specified board.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sections retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User has no access to this board"),
            @ApiResponse(responseCode = "404", description = "Board not found")
    })
    Page<SectionResponse> getSections(
            @Parameter(description = "ID of the board") Long boardId,
            @ParameterObject Pageable pageable
    );

    @Operation(summary = "Get or filter board labels", description = "Retrieves a list of tags/labels available on the board, optionally filtered by name.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Labels retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - No access to board")
    })
    List<LabelResponse> getLabels(
            @Parameter(description = "ID of the board") Long boardId,
            @Parameter(description = "Optional label name filter") String name
    );

    @Operation(summary = "Search and filter tasks inside a board", description = "Advanced search for tasks using complex criteria like statuses, labels, or assigned members.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Filtered tasks retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid filter parameters")
    })
    Page<TaskPreviewResponse> searchTasks(
            @Parameter(description = "ID of the board") Long boardId,
            @ParameterObject TaskFilter taskFilter,
            @ParameterObject Pageable pageable
    );

    @Operation(summary = "Create a new board", description = "Creates a brand new workspace. The creating user automatically becomes the Owner.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Board successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid board request data")
    })
    ResponseEntity<?> createBoard(
            @RequestBody @Validated BoardRequest boardRequest,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "Update board info", description = "Updates metadata of the board (e.g. title or description).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Board updated successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Only owners/managers can edit board info"),
            @ApiResponse(responseCode = "404", description = "Board not found")
    })
    BoardResponse updateBoard(
            @Parameter(description = "ID of the board to update") Long boardId,
            @RequestBody @Validated BoardRequest boardRequest
    );

    @Operation(summary = "Delete a board", description = "Permanently removes the board along with all its sections, tasks, and data.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Board successfully deleted"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Only the Owner can delete a board")
    })
    ResponseEntity<?> deleteBoard(@Parameter(description = "ID of the board to delete") Long boardId);

    @Operation(summary = "Search users to invite", description = "Finds system users by username or email who are not yet members of this board, making them available for invitations.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results retrieved successfully")
    })
    Page<UserSearchResponse> searchUsers(
            @Parameter(description = "ID of the current board") Long boardId,
            @ParameterObject @Validated UserSearchFilter userSearchFilter,
            @ParameterObject Pageable pageable
    );

    @Operation(summary = "Get board members list", description = "Retrieves a list of all users who have access to this board, with their roles, filtered by conditions.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Members list retrieved successfully")
    })
    Page<BoardMemberResponse> getMembers(
            @Parameter(description = "ID of the board") Long boardId,
            @ParameterObject BoardMemberFilter boardMemberFilter,
            @ParameterObject Pageable pageable
    );

    @Operation(summary = "Add a new member to the board", description = "Invites or directly attaches a user to the board with a specified initial role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Member added successfully"),
            @ApiResponse(responseCode = "400", description = "User is already a member or invalid role configuration")
    })
    BoardMemberResponse addMember(
            @Parameter(description = "ID of the board") Long boardId,
            @RequestBody @Validated BoardMemberRequest boardMemberRequest,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "Remove a member from the board", description = "Revokes user's access to the board. Cannot remove the board Owner.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Member removed successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions to kick members")
    })
    ResponseEntity<?> removeMember(
            @Parameter(description = "ID of the board") Long boardId,
            @Parameter(description = "ID of the user to remove") Long userId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "Change a member's role", description = "Updates workspace permissions for a specific member (e.g., upgrading from Viewer to Editor).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role successfully changed"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Only owners/managers can change roles")
    })
    BoardMemberResponse changeRole(
            @Parameter(description = "ID of the board") Long boardId,
            @RequestBody @Validated BoardMemberRequest boardMemberRequest,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "Leave the board", description = "Allows the current user to voluntarily leave the board workspace. Owners cannot leave without transferring ownership.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Successfully left the board"),
            @ApiResponse(responseCode = "400", description = "Owner cannot leave the board before transferring ownership")
    })
    ResponseEntity<?> leaveBoard(
            @Parameter(description = "ID of the board to leave") Long boardId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );
}
