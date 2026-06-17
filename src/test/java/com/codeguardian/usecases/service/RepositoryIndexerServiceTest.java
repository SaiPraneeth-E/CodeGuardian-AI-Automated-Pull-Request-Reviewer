package com.codeguardian.usecases.service;

import com.codeguardian.domain.model.CodeSnippet;
import com.codeguardian.usecases.port.out.EmbeddingClientPort;
import com.codeguardian.usecases.port.out.VectorStorePort;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;

class RepositoryIndexerServiceTest {

    private RepositoryIndexerService indexerService;

    @Mock
    private EmbeddingClientPort embeddingClient;

    @Mock
    private VectorStorePort vectorStore;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        indexerService = new RepositoryIndexerService(embeddingClient, vectorStore);
    }

    @Test
    void testIndexRepository_EmptyDirectory() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            indexerService.indexRepository("non-existent-directory-path-1234");
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void testIndexRepository_JavaFilesChunkingAndIngesting(@TempDir Path tempDir) throws IOException {
        // Create dummy java files to chunk
        Path srcDir = Files.createDirectories(tempDir.resolve("src/main/java"));
        Path javaFile1 = srcDir.resolve("TestService.java");

        // Write 40 lines of code to trigger chunking (chunkSize=30, overlap=5)
        List<String> codeLines = new ArrayList<>();
        for (int i = 1; i <= 40; i++) {
            codeLines.add("public class TestService { // line " + i + " }");
        }
        Files.write(javaFile1, codeLines);

        // Stub embedding client response
        Mockito.when(embeddingClient.generateEmbeddings(anyList())).thenAnswer(invocation -> {
            List<?> texts = invocation.getArgument(0);
            List<float[]> mockEmbeddings = new ArrayList<>();
            for (int i = 0; i < texts.size(); i++) {
                mockEmbeddings.add(new float[768]);
            }
            return mockEmbeddings;
        });

        // Run indexer
        indexerService.indexRepository(tempDir.toString());

        // Verify database was wiped first
        Mockito.verify(vectorStore, times(1)).clearAll();

        // 40 lines chunked with size=30, overlap=5:
        // Chunk 1: lines 1-30
        // Chunk 2: lines 26-40
        // Total chunks = 2
        ArgumentCaptor<List<CodeSnippet>> snippetCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(vectorStore, times(1)).upsertSnippets(snippetCaptor.capture(), anyList());

        List<CodeSnippet> capturedSnippets = snippetCaptor.getValue();
        Assertions.assertEquals(2, capturedSnippets.size());

        CodeSnippet c1 = capturedSnippets.get(0);
        Assertions.assertEquals("src/main/java/TestService.java", c1.getFilePath());
        Assertions.assertEquals(1, c1.getStartLine());
        Assertions.assertEquals(30, c1.getEndLine());

        CodeSnippet c2 = capturedSnippets.get(1);
        Assertions.assertEquals("src/main/java/TestService.java", c2.getFilePath());
        Assertions.assertEquals(26, c2.getStartLine());
        Assertions.assertEquals(40, c2.getEndLine());
    }
}
