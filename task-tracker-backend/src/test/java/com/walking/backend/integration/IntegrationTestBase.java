package com.walking.backend.integration;

import com.redis.testcontainers.RedisContainer;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.lifecycle.Startables;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Sql(scripts = "classpath:sql/data.sql")
@Transactional
public abstract class IntegrationTestBase {
    private static final String POSTGRES_IMAGE_NAME = "postgres:16-alpine";
    private static final String REDIS_IMAGE_NAME = "redis:8-alpine";
    private static final String KAFKA_IMAGE_NAME = "apache/kafka:4.1.1";

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE_NAME);

    @Container
    @ServiceConnection
    private static final RedisContainer redis = new RedisContainer(REDIS_IMAGE_NAME);

    @Container
    @ServiceConnection
    protected static final KafkaContainer kafka = new KafkaContainer(KAFKA_IMAGE_NAME);

    static {
        Startables.deepStart(postgres, redis, kafka).join();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.producer.bootstrap-servers", kafka::getBootstrapServers);
    }

    @MockitoSpyBean
    private KafkaProducerService kafkaProducerService;
}
