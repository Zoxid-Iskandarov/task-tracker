package com.walking.backend.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Label label = new Label();
    private Task task = new Task();
    private Kafka kafka = new Kafka();
    private Minio minio = new Minio();

    @Data
    public static class Label {
        private int maxPerBoard;
        private int maxPerTask;
    }

    @Data
    public static class Task {
        private double positionStep;
    }

    @Data
    public static class Kafka {
        private Topics topics = new Topics();
        private int partitions;
        private int replicas;

        @Data
        public static class Topics {
            private String emailSending;
            private String userActivity;
        }
    }

    @Data
    public static class Minio {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucketAvatar;
        private String bucketAttachment;
        private Attachment attachment = new Attachment();

        @Data
        public static class Attachment {
            private int presignedUrlExpiration;
            private int maxPerTask;
            private Set<String> allowedContentTypes;
        }
    }
}
