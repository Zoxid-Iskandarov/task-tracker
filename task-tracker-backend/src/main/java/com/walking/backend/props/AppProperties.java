package com.walking.backend.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Label label = new Label();
    private Task task = new Task();
    private Kafka kafka = new Kafka();
    private Minio minio = new Minio();
    private Security security = new Security();

    @Data
    public static class Security {
        private Jwt jwt = new Jwt();

        @Data
        public static class Jwt {
            private String secret;
            private long accessTokenExpiration;
            private long refreshTokenExpiration;
            private String cookieName;
            private Redis redis = new Redis();

            @Data
            public static class Redis {
                private String refreshTokenPrefix;
                private String userTokenPrefix;
            }
        }
    }

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
        private int batchDeleteSize;
        private Attachment attachment = new Attachment();

        @Data
        public static class Attachment {
            private int presignedUrlExpiration;
            private int maxPerTask;
            private Set<String> allowedContentTypes;
        }
    }
}
