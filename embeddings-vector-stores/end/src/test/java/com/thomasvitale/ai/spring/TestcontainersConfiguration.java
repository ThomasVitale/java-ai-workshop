package com.thomasvitale.ai.spring;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {
    
    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> pgvectorContainer() {
        return new PostgreSQLContainer<>("pgvector/pgvector:pg17");
    }
    
}
