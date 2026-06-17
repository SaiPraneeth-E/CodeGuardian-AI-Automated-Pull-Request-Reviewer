package com.codeguardian.usecases.port.out;

import com.codeguardian.domain.model.ReviewReport;
import com.codeguardian.domain.model.ReviewAgentRole;
import java.util.List;

/**
 * Output port for executing AI code reviews via Gemini API.
 */
public interface GeminiClientPort {
    /**
     * Sends the code diff to the Gemini API and parses the response.
     *
     * @param diffContent the raw git diff string
     * @return parsed review report
     */
    ReviewReport generateReview(String diffContent);

    /**
     * Sends the code diff and matching database context to the Gemini API and parses the response.
     *
     * @param diffContent the raw git diff string
     * @param codebaseContext relevant files retrieved from the vector database
     * @return parsed review report
     */
    ReviewReport generateReview(String diffContent, String codebaseContext);

    /**
     * Sends the code diff to the Gemini API using a specific agent role persona and feedback context.
     *
     * @param diffContent the raw git diff string
     * @param codebaseContext relevant files retrieved from the vector database
     * @param feedbackContext historical developer feedback to guide the agent
     * @param role the specific review agent persona
     * @return parsed review report
     */
    ReviewReport generateReview(String diffContent, String codebaseContext, String feedbackContext, ReviewAgentRole role);

    /**
     * Aggregates multiple agent review reports into a single executive summary using the Gemini API.
     *
     * @param reports list of review reports from individual agents
     * @return final summarized text
     */
    String summarizeReviews(List<ReviewReport> reports);

    /**
     * Executes a chat/conversation task with Gemini model.
     *
     * @param systemInstruction instruction for the agent's persona/behavior
     * @param userPrompt user question or context
     * @return response text from Gemini
     */
    String generateContent(String systemInstruction, String userPrompt);
}
