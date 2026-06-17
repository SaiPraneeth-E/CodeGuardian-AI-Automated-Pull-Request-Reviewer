package com.codeguardian.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Spring Configuration for the Gemini REST client connectivity.
 */
@Configuration
public class GeminiConfig {

    @Value("${codeguardian.gemini.timeout-ms:30000}")
    private int timeoutMs;

    /**
     * Constructs a Spring RestClient configured with proper HTTP socket read and connection timeouts.
     */
    @Bean
    public RestClient geminiRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
