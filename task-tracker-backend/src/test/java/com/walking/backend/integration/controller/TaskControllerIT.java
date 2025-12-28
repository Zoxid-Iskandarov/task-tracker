package com.walking.backend.integration.controller;

import com.walking.backend.integration.IntegrationTestBase;
import com.walking.backend.integration.annotation.WithMockUser;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser
@RequiredArgsConstructor
public class TaskControllerIT extends IntegrationTestBase {
    private final MockMvc mockMvc;

    @Test
    void getTasks_whenOnlyUserIdProvided_returnTasks() throws Exception {
        mockMvc.perform(get("/tasks")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.numberOfElements").value(5))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Implement user registration"))
                .andExpect(jsonPath("$.content[0].isCompleted").value(true))
                .andExpect(jsonPath("$.content[0].created").exists())
                .andExpect(jsonPath("$.content[0].description").exists());
    }

    @Test
    void getTasks_whenCompetedTrue_returnCompletedTasks() throws Exception {
        mockMvc.perform(get("/tasks")
                        .param("completed", "true")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.numberOfElements").value(3))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Implement user registration"))
                .andExpect(jsonPath("$.content[0].isCompleted").value(true))
                .andExpect(jsonPath("$.content[0].created").exists())
                .andExpect(jsonPath("$.content[0].description").exists());
    }

    @Test
    void getTasks_whenCompetedFalse_returnUnCompletedTasks() throws Exception {
        mockMvc.perform(get("/tasks")
                        .param("completed", "false")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.numberOfElements").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(5))
                .andExpect(jsonPath("$.content[0].title").value("Optimize image upload"))
                .andExpect(jsonPath("$.content[0].isCompleted").value(false))
                .andExpect(jsonPath("$.content[0].created").exists())
                .andExpect(jsonPath("$.content[0].description").exists());
    }

    @Test
    void getTasks_whenTodayTrue_returnTodayCreatedTasks() throws Exception {
        mockMvc.perform(get("/tasks")
                        .param("today", "true")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.numberOfElements").value(3))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Implement user registration"))
                .andExpect(jsonPath("$.content[0].isCompleted").value(true))
                .andExpect(jsonPath("$.content[0].created").exists())
                .andExpect(jsonPath("$.content[0].description").exists());
    }

    @Test
    void getTasks_whenCompletedAndTodayProvided_returnFilteredTasks() throws Exception {
        mockMvc.perform(get("/tasks")
                        .param("completed", "true")
                        .param("today", "true")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.numberOfElements").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Implement user registration"))
                .andExpect(jsonPath("$.content[0].isCompleted").value(true))
                .andExpect(jsonPath("$.content[0].created").exists())
                .andExpect(jsonPath("$.content[0].description").exists());
    }

    @Test
    void getTasks_whenPageRequested_returnsCorrectPageMetadata() throws Exception {
        mockMvc.perform(get("/tasks")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.numberOfElements").value(2))
                .andExpect(jsonPath("$.last").value(false))
                .andExpect(jsonPath("$.first").value(false));
    }

    @Test
    void getTasks_whenCustomSortRequested_returnsTasksInCorrectOrder() throws Exception {
        mockMvc.perform(get("/tasks")
                        .param("sort", "id,asc")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[4].id").value(5));
    }

    @Test
    @WithMockUser(id = 99, username = "EmptyUser")
    void getTasks_whenUserHasNoTasks_returnsEmptyPageWithOkStatus() throws Exception {
        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.empty").value(true));
    }

    @Test
    void createTask_whenValidRequestData_returnTaskResponse() throws Exception {
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "NEW-TASK title",
                                    "description": "NEW-TASK description"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.title").value("NEW-TASK title"))
                .andExpect(jsonPath("$.description").value("NEW-TASK description"))
                .andExpect(jsonPath("$.isCompleted").value(false))
                .andExpect(jsonPath("$.created").exists())
                .andExpect(jsonPath("$.updated").exists());
    }

    @Test
    void createTask_whenInvalidRequestData_returnErrorResponse() throws Exception {
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "",
                                    "description": "NEW-TASK description"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }


    @Test
    void updateTask_whenValidRequestData_returnTaskResponse() throws Exception {
        mockMvc.perform(put("/tasks/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "UPDATED title",
                                    "description": "UPDATED description"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("UPDATED title"))
                .andExpect(jsonPath("$.description").value("UPDATED description"))
                .andExpect(jsonPath("$.isCompleted").exists())
                .andExpect(jsonPath("$.created").exists())
                .andExpect(jsonPath("$.updated").exists());
    }

    @Test
    void updateTask_whenInvalidRequestData_returnErrorResponse() throws Exception {
        mockMvc.perform(put("/tasks/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "",
                                    "description": "UPDATED description"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void updateTask_whenUserIsNotOwner_forbidden() throws Exception {
        mockMvc.perform(put("/tasks/{id}", 1000)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "UPDATED title",
                                    "description": "UPDATED description"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void toggleCompleted_whenTaskExists_returnTaskResponse() throws Exception {
        mockMvc.perform(patch("/tasks/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.isCompleted").value(false))
                .andExpect(jsonPath("$.created").exists())
                .andExpect(jsonPath("$.updated").exists());
    }

    @Test
    void toggleCompleted_whenUserIsNotOwner_forbidder() throws Exception {
        mockMvc.perform(patch("/tasks/{id}", 1000))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteTask_whenTaskExists_ok() throws Exception {
        mockMvc.perform(delete("/tasks/{id}", 1))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTask_whenUserIsNotOwner_forbidden() throws Exception {
        mockMvc.perform(delete("/tasks/{id}", 1000))
                .andExpect(status().isForbidden());
    }
}
