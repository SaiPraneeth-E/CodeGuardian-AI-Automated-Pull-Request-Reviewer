package com.codeguardian.adapters.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller exposing REST endpoints for user profile metadata management.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    /**
     * Retrieves the profile metadata of the current authenticated SaaS user.
     *
     * @param principal OAuth2User injected by Spring Security
     * @return User profile details
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("login", principal.getAttribute("login"));
        userProfile.put("email", principal.getAttribute("email"));
        userProfile.put("avatarUrl", principal.getAttribute("avatar_url"));
        userProfile.put("githubId", principal.getAttribute("id"));
        
        return ResponseEntity.ok(userProfile);
    }
}
