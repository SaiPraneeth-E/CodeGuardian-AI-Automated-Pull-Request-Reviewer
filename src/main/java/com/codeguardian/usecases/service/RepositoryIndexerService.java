package com.codeguardian.usecases.service;

import com.codeguardian.domain.model.CodeSnippet;
import com.codeguardian.usecases.port.out.EmbeddingClientPort;
import com.codeguardian.usecases.port.out.VectorStorePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service orchestrating codebase file reading, sliding-window chunking, and embedding ingestion.
 */
@Service
public class RepositoryIndexerService {

    private static final Logger log = LoggerFactory.getLogger(RepositoryIndexerService.class);

    private final EmbeddingClientPort embeddingClient;
    private final VectorStorePort vectorStore;

    public RepositoryIndexerService(EmbeddingClientPort embeddingClient, VectorStorePort vectorStore) {
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
    }

    /**
     * Walks the root directory, chunks Java source files, generates embeddings, and indexes them.
     *
     * @param rootPath absolute path to the directory to index
     */
    public void indexRepository(String rootPath) {
        log.info("Starting repository indexing for directory: {}", rootPath);

        File rootDir = new File(rootPath);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            throw new IllegalArgumentException("Invalid repository root directory: " + rootPath);
        }

        // 1. Wipe current vector database index
        vectorStore.clearAll();

        // 2. Scan and extract sliding-window chunks
        List<CodeSnippet> snippets = new ArrayList<>();
        scanDirectory(rootDir, rootDir.getAbsolutePath(), snippets);
        log.info("Scan completed. Total code snippets generated: {}", snippets.size());

        if (snippets.isEmpty()) {
            return;
        }

        // 3. Batch process text embeddings and index them in Qdrant
        int batchSize = 20;
        for (int i = 0; i < snippets.size(); i += batchSize) {
            int end = Math.min(i + batchSize, snippets.size());
            List<CodeSnippet> batchSnippets = snippets.subList(i, end);

            List<String> batchTexts = batchSnippets.stream()
                    .map(CodeSnippet::getContent)
                    .collect(Collectors.toList());

            log.info("Generating embeddings and upserting batch {}/{} (snippets: {})",
                    (i / batchSize) + 1, (int) Math.ceil((double) snippets.size() / batchSize), batchSnippets.size());

            List<float[]> embeddings = embeddingClient.generateEmbeddings(batchTexts);
            vectorStore.upsertSnippets(batchSnippets, embeddings);
        }

        log.info("Repository indexing completed successfully.");
    }

    private void scanDirectory(File directory, String rootAbsPath, List<CodeSnippet> snippets) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                // Exclude common build and version control directories
                String name = file.getName();
                if (name.equals("target") || name.equals(".git") || name.equals(".gemini") ||
                        name.equals(".idea") || name.equals("maven-dist") || name.equals("node_modules")) {
                    continue;
                }
                scanDirectory(file, rootAbsPath, snippets);
            } else if (file.getName().endsWith(".java")) {
                chunkFile(file, rootAbsPath, snippets);
            }
        }
    }

    private void chunkFile(File file, String rootAbsPath, List<CodeSnippet> snippets) {
        // Extract relative path normalization
        String relativePath = file.getAbsolutePath().substring(rootAbsPath.length());
        if (relativePath.startsWith("/") || relativePath.startsWith("\\")) {
            relativePath = relativePath.substring(1);
        }
        relativePath = relativePath.replace("\\", "/");

        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            int totalLines = lines.size();

            // Line-based sliding window:
            // Chunk size = 30 lines, overlap = 5 lines
            int chunkSize = 30;
            int overlap = 5;
            int step = chunkSize - overlap;

            for (int startIdx = 0; startIdx < totalLines; startIdx += step) {
                int endIdx = Math.min(startIdx + chunkSize, totalLines);

                List<String> chunkLines = lines.subList(startIdx, endIdx);
                String content = String.join("\n", chunkLines);

                // startLine is 1-indexed
                CodeSnippet snippet = new CodeSnippet(relativePath, startIdx + 1, endIdx, content);
                snippets.add(snippet);

                // If we reached the end of the file, break the loop
                if (endIdx == totalLines) {
                    break;
                }
            }

        } catch (Exception e) {
            log.error("Failed to read and chunk file {}: {}", file.getAbsolutePath(), e.getMessage());
        }
    }
}
