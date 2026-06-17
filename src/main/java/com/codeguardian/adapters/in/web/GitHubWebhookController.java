package com.codeguardian.adapters.in.web;

import com.codeguardian.adapters.in.web.dto.WebhookPayloadDto;
import com.codeguardian.domain.exception.WebhookSecretInvalidException;
import com.codeguardian.domain.model.PullRequest;
import com.codeguardian.infrastructure.security.GitHubSignatureVerifier;
import com.codeguardian.usecases.port.in.ProcessWebhookUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * REST Controller to receive and validate incoming GitHub webhook payloads.
 */
@RestController
@RequestMapping("/github/webhook")
public class GitHubWebhookController {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookController.class);

    private final GitHubSignatureVerifier signatureVerifier;
    private final ProcessWebhookUseCase processWebhookUseCase;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public GitHubWebhookController(GitHubSignatureVerifier signatureVerifier,
                                   ProcessWebhookUseCase processWebhookUseCase,
                                   ObjectMapper objectMapper,
                                   Validator validator) {
        this.signatureVerifier = signatureVerifier;
        this.processWebhookUseCase = processWebhookUseCase;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    /**
     * Endpoint that GitHub invokes when a pull request webhook event is fired.
     *
     * @param signatureHeader X-Hub-Signature-256 header for validation
     * @param rawPayload      raw string payload representing body
     * @return REST response details
     */
    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signatureHeader,
            @RequestBody String rawPayload) {

        log.info("Received a new webhook request from GitHub.");

        // 1. Verify GitHub Signature integrity
        if (signatureHeader != null) {
            boolean isSignatureValid = signatureVerifier.verifySignature(rawPayload, signatureHeader);
            if (!isSignatureValid) {
                log.warn("Request rejected: HMAC signature verification failed.");
                throw new WebhookSecretInvalidException("Invalid GitHub webhook signature.");
            }
            log.debug("GitHub signature verified successfully.");
        } else {
            log.warn("X-Hub-Signature-256 header is missing. Proceeding without signature verification (not recommended for production).");
        }

        // 2. Deserialize webhook JSON body
        WebhookPayloadDto payloadDto;
        try {
            payloadDto = objectMapper.readValue(rawPayload, WebhookPayloadDto.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse GitHub webhook raw JSON payload: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Malformed JSON payload.");
        }

        // 3. Filter out non-PR events
        if (payloadDto.getPullRequest() == null) {
            log.info("Webhook event ignored: Payload does not contain pull request details.");
            return ResponseEntity.ok("Event ignored (Not a PR event).");
        }

        // 4. Validate DTO constraints programmatically
        Set<ConstraintViolation<WebhookPayloadDto>> violations = validator.validate(payloadDto);
        if (!violations.isEmpty()) {
            log.warn("Payload validation failed with {} errors.", violations.size());
            throw new ConstraintViolationException(violations);
        }

        String action = payloadDto.getAction();
        String repositoryName = payloadDto.getRepository().getName();
        Integer prNumber = payloadDto.getPullRequest().getNumber();

        // 5. Log required parameters: repo name, PR number, action
        log.info("GitHub Pull Request Webhook Event Logged:");
        log.info("  - Repository Name: {}", repositoryName);
        log.info("  - PR Number:       {}", prNumber);
        log.info("  - Action:          {}", action);

        // 6. Map and execute domain use case for specified actions (opened, synchronize, reopened)
        if ("opened".equalsIgnoreCase(action) || 
            "synchronize".equalsIgnoreCase(action) || 
            "reopened".equalsIgnoreCase(action)) {
            
            PullRequest prDomain = payloadDto.toDomain();
            PullRequest savedPr = processWebhookUseCase.execute(prDomain);
            log.info("Successfully executed usecase for PR #{}. Internal ID: {}", prNumber, savedPr.getId());
        } else {
            log.info("Action '{}' is logged but ignored for processing.", action);
        }

        // 7. Return HTTP 200 immediately
        return ResponseEntity.ok("Webhook received and processed.");
    }
}
