package com.codeguardian.infrastructure.gemini;

import com.codeguardian.domain.model.ReviewReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiClientTest {

    private GeminiClient geminiClient;
    private MockRestServiceServer mockServer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        
        geminiClient = new GeminiClient(builder.build(), objectMapper);
        
        ReflectionTestUtils.setField(geminiClient, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(geminiClient, "model", "gemini-1.5-flash");
        ReflectionTestUtils.setField(geminiClient, "maxRetries", 3);
        ReflectionTestUtils.setField(geminiClient, "backoffMs", 50L); // Tiny backoff to make tests run quickly
    }

    @Test
    void generateReview_Success() throws Exception {
        String mockInnerResultJson = """
                {
                  "summary": "Review summary description.",
                  "overallScore": 88,
                  "comments": [
                    {
                      "filePath": "src/main/java/Main.java",
                      "lineNumber": 10,
                      "category": "BUG",
                      "commentText": "Fix array out of bounds danger."
                    }
                  ]
                }
                """;

        String apiEnvelope = String.format("""
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": %s
                          }
                        ]
                      },
                      "finishReason": "STOP"
                    }
                  ]
                }
                """, objectMapper.writeValueAsString(mockInnerResultJson));

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=test-api-key"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(apiEnvelope, MediaType.APPLICATION_JSON));

        ReviewReport report = geminiClient.generateReview("git diff contents");

        assertNotNull(report);
        assertEquals("Review summary description.", report.getSummary());
        assertEquals(88, report.getOverallScore());
        assertEquals(1, report.getComments().size());
        assertEquals("src/main/java/Main.java", report.getComments().get(0).getFilePath());
        assertEquals(10, report.getComments().get(0).getLineNumber());
        assertEquals("BUG", report.getComments().get(0).getCategory());
        mockServer.verify();
    }

    @Test
    void generateReview_RetrySuccess() throws Exception {
        String mockInnerResultJson = """
                {
                  "summary": "Succeeded after retry.",
                  "overallScore": 75,
                  "comments": []
                }
                """;

        String apiEnvelope = String.format("""
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": %s
                          }
                        ]
                      }
                    }
                  ]
                }
                """, objectMapper.writeValueAsString(mockInnerResultJson));

        // 1st request fails with 429
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=test-api-key"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        // 2nd request succeeds
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=test-api-key"))
                .andRespond(withSuccess(apiEnvelope, MediaType.APPLICATION_JSON));

        ReviewReport report = geminiClient.generateReview("git diff contents");

        assertNotNull(report);
        assertEquals("Succeeded after retry.", report.getSummary());
        mockServer.verify();
    }

    @Test
    void generateReview_FailureExhausted() {
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=test-api-key"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=test-api-key"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=test-api-key"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(RuntimeException.class, () -> geminiClient.generateReview("git diff contents"));
        mockServer.verify();
    }

    @Test
    void generateEmbedding_Success() throws Exception {
        String mockResponseJson = """
                {
                  "embedding": {
                    "values": [0.1, 0.2, 0.3]
                  }
                }
                """;

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent?key=test-api-key"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(mockResponseJson, MediaType.APPLICATION_JSON));

        float[] vector = geminiClient.generateEmbedding("hello world");

        assertNotNull(vector);
        assertEquals(3, vector.length);
        assertEquals(0.1f, vector[0]);
        assertEquals(0.2f, vector[1]);
        assertEquals(0.3f, vector[2]);
        mockServer.verify();
    }

    @Test
    void generateEmbeddings_Success() throws Exception {
        String mockResponseJson = """
                {
                  "embeddings": [
                    {
                      "values": [0.1, 0.2]
                    },
                    {
                      "values": [0.3, 0.4]
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:batchEmbedContents?key=test-api-key"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(mockResponseJson, MediaType.APPLICATION_JSON));

        List<float[]> vectors = geminiClient.generateEmbeddings(List.of("text1", "text2"));

        assertNotNull(vectors);
        assertEquals(2, vectors.size());
        assertEquals(0.1f, vectors.get(0)[0]);
        assertEquals(0.4f, vectors.get(1)[1]);
        mockServer.verify();
    }

    @Test
    void generateContent_Success() throws Exception {
        String mockResponseJson = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "This is a chat response from Gemini."
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=test-api-key"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(mockResponseJson, MediaType.APPLICATION_JSON));

        String result = geminiClient.generateContent("You are a helpful assistant.", "Hello, Gemini!");

        assertEquals("This is a chat response from Gemini.", result);
        mockServer.verify();
    }
}
