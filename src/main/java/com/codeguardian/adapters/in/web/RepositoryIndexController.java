package com.codeguardian.adapters.in.web;

import com.codeguardian.usecases.service.RepositoryIndexerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller exposing REST endpoints to trigger codebase indexing for RAG search.
 */
@RestController
public class RepositoryIndexController {

    private static final Logger log = LoggerFactory.getLogger(RepositoryIndexController.class);
    private final RepositoryIndexerService indexerService;

    public RepositoryIndexController(RepositoryIndexerService indexerService) {
        this.indexerService = indexerService;
    }

    /**
     * Endpoint to index all Java files in the repository.
     *
     * @param path optional custom absolute path to index (defaults to user.dir)
     */
    @PostMapping("/repository/index")
    public ResponseEntity<Map<String, Object>> indexRepository(@RequestParam(value = "path", required = false) String path) {
        String targetPath = (path != null && !path.trim().isEmpty()) ? path : System.getProperty("user.dir");
        log.info("REST request received to index repository path: {}", targetPath);

        indexerService.indexRepository(targetPath);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Repository indexed successfully.",
                "path", targetPath
        ));
    }
}
