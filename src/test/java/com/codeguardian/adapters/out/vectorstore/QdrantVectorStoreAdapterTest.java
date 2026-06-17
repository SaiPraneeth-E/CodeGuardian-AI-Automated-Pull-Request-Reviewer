package com.codeguardian.adapters.out.vectorstore;

import com.codeguardian.domain.model.CodeSnippet;
import com.google.common.util.concurrent.ListenableFuture;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.UpdateResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class QdrantVectorStoreAdapterTest {

    private QdrantVectorStoreAdapter adapter;

    @Mock
    private QdrantClient qdrantClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adapter = new QdrantVectorStoreAdapter(qdrantClient, "test-collection", 768);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testUpsertSnippets_Success() throws ExecutionException, InterruptedException {
        // Stub collectionExistsAsync to return true
        ListenableFuture<Boolean> collectionExistsFuture = Mockito.mock(ListenableFuture.class);
        Mockito.when(collectionExistsFuture.get()).thenReturn(true);
        Mockito.when(qdrantClient.collectionExistsAsync("test-collection")).thenReturn(collectionExistsFuture);

        // Stub upsertAsync to return UpdateResult
        ListenableFuture<UpdateResult> upsertFuture = Mockito.mock(ListenableFuture.class);
        Mockito.when(upsertFuture.get()).thenReturn(Mockito.mock(UpdateResult.class));
        Mockito.when(qdrantClient.upsertAsync(eq("test-collection"), any(List.class))).thenReturn(upsertFuture);

        CodeSnippet snippet = new CodeSnippet("src/App.java", 1, 10, "public class App {}");
        List<float[]> embeddings = List.of(new float[768]);

        adapter.upsertSnippets(List.of(snippet), embeddings);

        // Verify collectionExistsAsync and upsertAsync were called
        Mockito.verify(qdrantClient, Mockito.times(1)).collectionExistsAsync("test-collection");

        ArgumentCaptor<List<PointStruct>> pointsCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(qdrantClient, Mockito.times(1)).upsertAsync(eq("test-collection"), pointsCaptor.capture());

        List<PointStruct> capturedPoints = pointsCaptor.getValue();
        Assertions.assertEquals(1, capturedPoints.size());
        PointStruct point = capturedPoints.get(0);
        Assertions.assertEquals("src/App.java", point.getPayloadMap().get("file_path").getStringValue());
        Assertions.assertEquals(1, point.getPayloadMap().get("start_line").getIntegerValue());
        Assertions.assertEquals(10, point.getPayloadMap().get("end_line").getIntegerValue());
        Assertions.assertEquals("public class App {}", point.getPayloadMap().get("content").getStringValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testClearAll_DeletesCollection() throws ExecutionException, InterruptedException {
        ListenableFuture<Boolean> collectionExistsFuture = Mockito.mock(ListenableFuture.class);
        Mockito.when(collectionExistsFuture.get()).thenReturn(true);
        Mockito.when(qdrantClient.collectionExistsAsync("test-collection")).thenReturn(collectionExistsFuture);

        ListenableFuture<io.qdrant.client.grpc.Collections.CollectionOperationResponse> deleteFuture = Mockito.mock(ListenableFuture.class);
        Mockito.when(deleteFuture.get()).thenReturn(Mockito.mock(io.qdrant.client.grpc.Collections.CollectionOperationResponse.class));
        Mockito.when(qdrantClient.deleteCollectionAsync("test-collection")).thenReturn(deleteFuture);

        adapter.clearAll();

        Mockito.verify(qdrantClient, Mockito.times(1)).collectionExistsAsync("test-collection");
        Mockito.verify(qdrantClient, Mockito.times(1)).deleteCollectionAsync("test-collection");
    }
}
