package com.walking.backend.integration.controller;

import com.walking.backend.integration.IntegrationTestBase;
import com.walking.backend.integration.annotation.WithMockUser;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RequiredArgsConstructor
public class UserControllerIT extends IntegrationTestBase {
    private final MockMvc mockMvc;

    @Test
    @WithMockUser
    void getUserInfo_whenUserAuthenticated_returnUserResponse() throws Exception {
        mockMvc.perform(get("/user"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                            "id": 1,
                            "username": "Zoxka",
                            "email":"san781617@gmail.com"
                        }
                        """));
    }

    @Test
    void getUserInfo_whenUserNotAuthenticated_returnErrorResponse() throws Exception {
        mockMvc.perform(get("/user"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Access token not transformed"));
    }

    @Test
    void getUserInfo_whenInvalidToken_returnErrorResponse() throws Exception {
        mockMvc.perform(get("/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.access.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.UNAUTHORIZED.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.UNAUTHORIZED.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value("Invalid or malformed token"));
    }
}
