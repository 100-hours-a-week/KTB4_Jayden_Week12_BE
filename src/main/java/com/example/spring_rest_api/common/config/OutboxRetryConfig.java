package com.example.spring_rest_api.common.config;

import io.lettuce.core.RedisCommandTimeoutException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class OutboxRetryConfig {

    @Bean
    public RetryTemplate redisPublishRetryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(3)
                .exponentialBackoff(
                        200,
                        2.0,
                        1000
                )
                .retryOn(RedisConnectionFailureException.class)
                .retryOn(RedisCommandTimeoutException.class)
                .traversingCauses()
                .build();
    }
}
