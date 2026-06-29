package com.walking.backend.config;

import com.walking.backend.props.CacheNames;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        RedisSerializer<Object> serializer = GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .allowIfSubType("com.walking.backend")
                        .build())
                .build();

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer)
                );
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(
            RedisCacheConfiguration cacheConfiguration) {
        return builder -> builder
                .withCacheConfiguration(
                        CacheNames.USER_PROFILE,
                        cacheConfiguration.entryTtl(Duration.ofHours(1))
                )
                .withCacheConfiguration(
                        CacheNames.USER_PUBLIC_PROFILE,
                        cacheConfiguration.entryTtl(Duration.ofHours(1))
                )
                .withCacheConfiguration(
                        CacheNames.USER_SHORT_PROFILE,
                        cacheConfiguration.entryTtl(Duration.ofHours(1))
                )
                .withCacheConfiguration(
                        CacheNames.BOARD_INFO,
                        cacheConfiguration.entryTtl(Duration.ofMinutes(30))
                )
                .withCacheConfiguration(
                        CacheNames.BOARD_INFO_SECTION,
                        cacheConfiguration.entryTtl(Duration.ofMinutes(30))
                );
    }
}
