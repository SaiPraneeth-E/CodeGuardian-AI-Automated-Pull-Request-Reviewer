package com.codeguardian.infrastructure.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration declaring Qdrant gRPC client connections.
 */
@Configuration
public class QdrantConfig {

    @Value("${codeguardian.qdrant.host:localhost}")
    private String host;

    @Value("${codeguardian.qdrant.port:6334}")
    private int port;

    @Bean
    public QdrantClient qdrantClient() {
        // Connects via gRPC to Qdrant vector database
        return new QdrantClient(
                QdrantGrpcClient.newBuilder(host, port, false).build()
        );
    }
}
