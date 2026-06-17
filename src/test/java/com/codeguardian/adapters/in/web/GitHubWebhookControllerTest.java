package com.codeguardian.adapters.in.web;

import com.codeguardian.domain.model.PullRequest;
import com.codeguardian.infrastructure.security.GitHubSignatureVerifier;
import com.codeguardian.usecases.port.in.ProcessWebhookUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@WebMvcTest(GitHubWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
class GitHubWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GitHubSignatureVerifier signatureVerifier;

    @MockBean
    private ProcessWebhookUseCase processWebhookUseCase;

    @Test
    void handleWebhook_Success() throws Exception {
        String payload = """
                {
                  "action": "opened",
                  "number": 12,
                  "pull_request": {
                    "id": 987654321,
                    "number": 12,
                    "title": "Fix issue #45",
                    "state": "open",
                    "head": { "sha": "abcdef123" },
                    "base": { "sha": "base12345" }
                  },
                  "repository": {
                    "name": "my-repo",
                    "owner": { "login": "my-org" }
                  }
                }
                """;
        String dummySignature = "sha256=dummysignaturehash";

        Mockito.when(signatureVerifier.verifySignature(any(String.class), eq(dummySignature))).thenReturn(true);

        PullRequest mockPrResult = PullRequest.builder()
                .id(UUID.randomUUID())
                .githubPrId(987654321L)
                .prNumber(12)
                .repositoryName("my-repo")
                .owner("my-org")
                .title("Fix issue #45")
                .state("open")
                .headSha("abcdef123")
                .baseSha("base12345")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Mockito.when(processWebhookUseCase.execute(any(PullRequest.class))).thenReturn(mockPrResult);

        mockMvc.perform(post("/github/webhook")
                        .header("X-Hub-Signature-256", dummySignature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook received and processed."));
    }

    @Test
    void handleWebhook_Unauthorized_WhenSignatureInvalid() throws Exception {
        String payload = "{}";
        String dummySignature = "sha256=invalidsignature";

        Mockito.when(signatureVerifier.verifySignature(any(String.class), eq(dummySignature))).thenReturn(false);

        mockMvc.perform(post("/github/webhook")
                        .header("X-Hub-Signature-256", dummySignature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid GitHub webhook signature."));
    }

    @Test
    void handleWebhook_Ignored_WhenNotPullRequestEvent() throws Exception {
        String payload = """
                {
                  "action": "created",
                  "issue": {
                    "id": 12345
                  },
                  "repository": {
                    "name": "my-repo",
                    "owner": { "login": "my-org" }
                  }
                }
                """;
        String dummySignature = "sha256=dummysignaturehash";

        Mockito.when(signatureVerifier.verifySignature(any(String.class), eq(dummySignature))).thenReturn(true);

        mockMvc.perform(post("/github/webhook")
                        .header("X-Hub-Signature-256", dummySignature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string("Event ignored (Not a PR event)."));
    }
}
