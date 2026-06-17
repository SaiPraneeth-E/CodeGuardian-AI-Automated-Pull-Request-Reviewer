package com.codeguardian.infrastructure.security;

import com.codeguardian.domain.model.User;
import com.codeguardian.usecases.port.out.UserRepositoryPort;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Custom Success Handler executing post-login business logic for GitHub OAuth authentication.
 * Securely extracts and encrypts the OAuth token and syncs the user profile into PostgreSQL.
 */
@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    private final UserRepositoryPort userRepositoryPort;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final TokenEncryptionService encryptionService;
    private final String dashboardUrl;

    public OAuth2LoginSuccessHandler(UserRepositoryPort userRepositoryPort,
                                     OAuth2AuthorizedClientService authorizedClientService,
                                     TokenEncryptionService encryptionService,
                                     @Value("${codeguardian.dashboard-url:http://localhost:3000}") String dashboardUrl) {
        this.userRepositoryPort = userRepositoryPort;
        this.authorizedClientService = authorizedClientService;
        this.encryptionService = encryptionService;
        this.dashboardUrl = dashboardUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = oauthToken.getPrincipal();

        // 1. Get access token from authorized client service
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName()
        );
        
        if (client == null || client.getAccessToken() == null) {
            log.error("Failed to load OAuth2 Authorized Client details.");
            response.sendRedirect("/login?error=oauth_failed");
            return;
        }
        
        String accessToken = client.getAccessToken().getTokenValue();

        // 2. Extract profile fields
        Number githubUserIdNum = oauth2User.getAttribute("id");
        Long githubUserId = githubUserIdNum != null ? githubUserIdNum.longValue() : null;
        String login = oauth2User.getAttribute("login");
        String email = oauth2User.getAttribute("email");
        String avatarUrl = oauth2User.getAttribute("avatar_url");

        if (githubUserId == null || login == null) {
            log.error("Failed to extract essential GitHub user profile parameters.");
            response.sendRedirect("/login?error=invalid_profile");
            return;
        }

        log.info("OAuth2 Login Succeeded for GitHub user: {} (ID: {})", login, githubUserId);

        // 3. Encrypt GitHub Access Token for secure DB storage
        String encryptedToken = encryptionService.encrypt(accessToken);

        // 4. Create or Update user registration record
        Optional<User> existingUserOpt = userRepositoryPort.findByGithubUserId(githubUserId);
        User user;
        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get().toBuilder()
                    .login(login)
                    .email(email)
                    .avatarUrl(avatarUrl)
                    .oauthTokenEncrypted(encryptedToken)
                    .updatedAt(Instant.now())
                    .build();
            log.debug("Updating existing user record.");
        } else {
            user = User.builder()
                    .id(UUID.randomUUID())
                    .githubUserId(githubUserId)
                    .login(login)
                    .email(email)
                    .avatarUrl(avatarUrl)
                    .oauthTokenEncrypted(encryptedToken)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            log.debug("Creating new user record.");
        }

        userRepositoryPort.save(user);

        // 5. Clear context attributes and redirect to dashboard URL
        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, dashboardUrl);
    }
}
