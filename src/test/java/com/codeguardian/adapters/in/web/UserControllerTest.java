package com.codeguardian.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for the User Profile endpoint.
 * Verifies security authorization rules and authentication parsing.
 */
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getCurrentUser_Unauthenticated_RedirectsToLogin() throws Exception {
        // Without authentication, accessing /api/users/me should redirect to GitHub/OAuth2 login page
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void getCurrentUser_Authenticated_ReturnsProfileDetails() throws Exception {
        // With mock OAuth2 authentication, accessing /api/users/me should succeed and return properties
        mockMvc.perform(get("/api/users/me")
                        .with(oauth2Login()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .attributes(attrs -> {
                                    attrs.put("id", 12345L);
                                    attrs.put("login", "octocat");
                                    attrs.put("email", "octocat@github.com");
                                    attrs.put("avatar_url", "https://github.com/images/error/octocat_happy.gif");
                                })
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("octocat"))
                .andExpect(jsonPath("$.email").value("octocat@github.com"))
                .andExpect(jsonPath("$.avatarUrl").value("https://github.com/images/error/octocat_happy.gif"))
                .andExpect(jsonPath("$.githubId").value(12345));
    }
}
