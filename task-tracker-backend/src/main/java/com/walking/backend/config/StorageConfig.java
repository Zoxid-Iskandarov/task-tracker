package com.walking.backend.config;

import com.walking.backend.props.AppProperties;
import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    @Bean
    public AppProperties.Minio minioProperties(AppProperties appProperties) {
        return appProperties.getMinio();
    }

    @Bean
    public MinioClient minioClient(AppProperties.Minio minioProperties) {
        return MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }
}
