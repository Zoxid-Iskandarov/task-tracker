package com.walking.backend.integration;

import com.redis.testcontainers.RedisContainer;
import com.walking.backend.security.authentication.TokenService;
import com.walking.backend.service.KafkaProducerService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Sql(scripts = "classpath:sql/data.sql")
@Transactional
public abstract class IntegrationTestBase {
    private static final String POSTGRES_IMAGE_NAME = "postgres:16-alpine";
    private static final String REDIS_IMAGE_NAME = "redis:8-alpine";
    private static final String KAFKA_IMAGE_NAME = "apache/kafka:4.1.1";

    private static final DockerImageName MINIO_IMAGE = DockerImageName
            .parse("quay.io/minio/minio:RELEASE.2025-09-07T16-13-09Z")
            .asCompatibleSubstituteFor("minio/minio");

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE_NAME);

    @Container
    @ServiceConnection
    private static final RedisContainer redis = new RedisContainer(REDIS_IMAGE_NAME);

    @Container
    @ServiceConnection
    protected static final KafkaContainer kafka = new KafkaContainer(KAFKA_IMAGE_NAME);

    @Container
    private static final MinIOContainer minio = new MinIOContainer(MINIO_IMAGE);

    static {
        Startables.deepStart(postgres, redis, kafka, minio).join();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.producer.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("app.minio.endpoint", minio::getS3URL);
        registry.add("app.minio.access-key", minio::getUserName);
        registry.add("app.minio.secret-key", minio::getPassword);
    }

    @MockitoSpyBean
    private KafkaProducerService kafkaProducerService;

    @MockitoSpyBean
    private TokenService tokenService;
}
