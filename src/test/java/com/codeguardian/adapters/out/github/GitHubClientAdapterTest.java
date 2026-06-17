package com.codeguardian.adapters.out.github;

import com.codeguardian.domain.exception.GitHubApiException;
import com.codeguardian.domain.model.ReviewComment;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GitHubClientAdapterTest {

    private GitHubClientAdapter adapter;
    private MockRestServiceServer mockServer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();

        adapter = new GitHubClientAdapter(builder.build());

        ReflectionTestUtils.setField(adapter, "token", "my-test-token");
        ReflectionTestUtils.setField(adapter, "apiUrl", "https://api.github.com");
    }

    @Test
    void fetchExistingComments_Success() throws Exception {
        String mockResponse = """
                [
                  {
                    "path": "src/App.java",
                    "line": 15,
                    "body": "Fix null safety.\\n\\n<!-- System: CodeGuardianAI -->"
                  }
                ]
                """;

        mockServer.expect(requestTo("https://api.github.com/repos/my-org/my-repo/pulls/5/comments"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer my-test-token"))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        List<ReviewComment> comments = adapter.fetchExistingComments("my-org", "my-repo", 5);

        assertNotNull(comments);
        assertEquals(1, comments.size());
        assertEquals("src/App.java", comments.get(0).getFilePath());
        assertEquals(15, comments.get(0).getLineNumber());
        assertTrue(comments.get(0).getCommentText().contains("System: CodeGuardianAI"));
        mockServer.verify();
    }

    @Test
    void postReview_Success_WithFiltering() throws Exception {
        // Mock fetch existing comments returning one matched item
        String existingCommentsMockResponse = """
                [
                  {
                    "path": "src/App.java",
                    "line": 15,
                    "body": "Fix null safety.\\n\\n<!-- System: CodeGuardianAI -->"
                  }
                ]
                """;

        // 1. Expected call to fetch existing comments
        mockServer.expect(requestTo("https://api.github.com/repos/my-org/my-repo/pulls/5/comments"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(existingCommentsMockResponse, MediaType.APPLICATION_JSON));

        // 2. Expected call to submit reviews (only non-duplicate, i.e., line 20 comment)
        mockServer.expect(requestTo("https://api.github.com/repos/my-org/my-repo/pulls/5/reviews"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer my-test-token"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.comments.length()").value(1))
                .andExpect(jsonPath("$.comments[0].line").value(20))
                .andExpect(jsonPath("$.comments[0].path").value("src/App.java"))
                .andExpect(jsonPath("$.event").value("APPROVE"))
                .andExpect(jsonPath("$.body").value("Cc: @octocat\n\nPR Review summary\n\n<!-- System: CodeGuardianAI -->"))
                .andRespond(withSuccess());

        List<ReviewComment> generatedComments = List.of(
                new ReviewComment("src/App.java", 15, "BUG", "Fix null safety."), // Duplicate
                new ReviewComment("src/App.java", 20, "SECURITY", "Use secure random.") // New
        );

        adapter.postReview("my-org", "my-repo", 5, "PR Review summary", generatedComments, 85, "octocat");

        mockServer.verify();
    }

    @Test
    void postReview_ApiError_ThrowsGitHubApiException() {
        mockServer.expect(requestTo("https://api.github.com/repos/my-org/my-repo/pulls/5/comments"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        List<ReviewComment> generatedComments = List.of(
                new ReviewComment("src/App.java", 15, "BUG", "Fix null safety.")
        );

        assertThrows(GitHubApiException.class, () -> 
                adapter.postReview("my-org", "my-repo", 5, "PR Review summary", generatedComments, 85, "octocat"));
                
        mockServer.verify();
    }
}
