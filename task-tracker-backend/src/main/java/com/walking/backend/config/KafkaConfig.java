package com.walking.backend.config;

import com.walking.backend.props.AppProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public AppProperties.Kafka kafkaProperties(AppProperties appProperties) {
        return appProperties.getKafka();
    }

    @Bean
    public NewTopic userActivityTopic(AppProperties.Kafka kafkaProperties) {
        return TopicBuilder.name(kafkaProperties.getTopics().getUserActivity())
                .partitions(kafkaProperties.getPartitions())
                .replicas(kafkaProperties.getReplicas())
                .build();
    }
}
