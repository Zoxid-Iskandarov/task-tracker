package com.walking.backend.integration.kafka;

import com.walking.backend.domain.dto.kafka.MessageDto;
import com.walking.backend.integration.IntegrationTestBase;
import com.walking.backend.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RequiredArgsConstructor
public class KafkaProducerServiceIT extends IntegrationTestBase {
    private final KafkaProducerService kafkaProducerService;

    private KafkaMessageListenerContainer<String, MessageDto> container;
    private BlockingQueue<ConsumerRecord<String, MessageDto>> results;

    private static final String TOPIC = "EMAIL_SENDING_TASKS";

    @Value("${spring.kafka.consumer.group-id}")
    private final String groupId;
    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private final String offset;
    @Value("${spring.kafka.consumer.properties.spring.json.type.mapping}")
    private final String typeMapping;
    @Value("${spring.kafka.consumer.properties.spring.json.trusted.packages}")
    private final String trustedPackages;

    @BeforeAll
    void startContainer() {
        createTopicIfNotExists();
        startConsumer();
    }

    @BeforeEach
    void setUp() {
        results.clear();
    }

    @Test
    void sendEmail_publishMessageToKafka() throws InterruptedException {
        String key = "1";
        MessageDto expected = new MessageDto("test@gmail.com", "Some Title", "Some Message");

        kafkaProducerService.sendMessageDto(key, expected);

        ConsumerRecord<String, MessageDto> record = results.poll(5, TimeUnit.SECONDS);

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo(key);

        MessageDto actual = record.value();
        assertThat(actual.getEmail()).isEqualTo(expected.getEmail());
        assertThat(actual.getTitle()).isEqualTo(expected.getTitle());
        assertThat(actual.getMessage()).isEqualTo(expected.getMessage());
    }

    @Test
    void sendEmail_shouldHandleSpecialCharacters() throws InterruptedException {
        String body = "Привет! Это тестовое сообщение 🚀";
        MessageDto messageDto = new MessageDto("rus@test.com", "Заголовок", body);

        kafkaProducerService.sendMessageDto("key-utf8", messageDto);

        ConsumerRecord<String, MessageDto> record = results.poll(5, TimeUnit.SECONDS);

        assertThat(record).isNotNull();
        assertThat(record.value().getTitle()).isEqualTo("Заголовок");
        assertThat(record.value().getMessage()).isEqualTo(body);
    }

    @AfterAll
    void afterAll() {
        if (container != null) {
            container.stop();
        }
    }

    private void startConsumer() {
        results = new LinkedBlockingQueue<>();

        var consumerFactory = new DefaultKafkaConsumerFactory<String, MessageDto>(getConsumerConfigs());
        var containerProperties = new ContainerProperties(TOPIC);

        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        container.setupMessageListener((MessageListener<String, MessageDto>) results::add);
        container.start();
    }

    private void createTopicIfNotExists() {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());

        try (AdminClient adminClient = AdminClient.create(props)) {
            adminClient.createTopics(Collections.singletonList(new NewTopic(TOPIC, 1, (short) 1)));
        } catch (Exception ignored) {
        }
    }

    private Map<String, Object> getConsumerConfigs() {
        Map<String, Object> configs = new HashMap<>();

        configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        configs.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offset);

        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configs.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JacksonJsonDeserializer.class);
        configs.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, trustedPackages);
        configs.put(JacksonJsonDeserializer.TYPE_MAPPINGS, typeMapping);

        return configs;
    }
}
