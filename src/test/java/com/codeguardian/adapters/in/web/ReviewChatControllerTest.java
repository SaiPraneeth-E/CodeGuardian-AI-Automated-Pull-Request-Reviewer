package com.codeguardian.adapters.in.web;

import com.codeguardian.usecases.service.ReviewChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for interactive chat endpoints.
 */
@WebMvcTest(ReviewChatController.class)
class ReviewChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewChatService chatService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void chatAboutReview_Authenticated_Success() throws Exception {
        UUID reviewId = UUID.randomUUID();
        ReviewChatController.ChatRequest request = new ReviewChatController.ChatRequest();
        request.setMessage("Why did you flag this variable?");

        when(chatService.chatAboutReview(eq(reviewId), eq("Why did you flag this variable?")))
                .thenReturn("It is a hardcoded credential.");

        mockMvc.perform(post("/api/reviews/" + reviewId + "/chat")
                        .with(oauth2Login().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("It is a hardcoded credential."));
    }

    @Test
    void chatAboutReview_EmptyMessage_ReturnsBadRequest() throws Exception {
        UUID reviewId = UUID.randomUUID();
        ReviewChatController.ChatRequest request = new ReviewChatController.ChatRequest();
        request.setMessage("   ");

        mockMvc.perform(post("/api/reviews/" + reviewId + "/chat")
                        .with(oauth2Login().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chatAboutReview_Unauthenticated_RedirectsToLogin() throws Exception {
        UUID reviewId = UUID.randomUUID();
        ReviewChatController.ChatRequest request = new ReviewChatController.ChatRequest();
        request.setMessage("Why?");

        mockMvc.perform(post("/api/reviews/" + reviewId + "/chat")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is3xxRedirection());
    }
}
