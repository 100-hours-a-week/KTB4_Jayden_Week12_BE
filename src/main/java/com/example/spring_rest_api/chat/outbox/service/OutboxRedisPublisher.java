package com.example.spring_rest_api.chat.outbox.service;

import com.example.spring_rest_api.chat.outbox.dto.ClaimedOutbox;
import com.example.spring_rest_api.chat.outbox.dto.PublishResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class OutboxRedisPublisher {
    private final StringRedisTemplate redisTemplate;
    private final RetryTemplate redisPublishRetryTemplate;

    public PublishResult publish(ClaimedOutbox outbox) {
        AtomicInteger attempts = new AtomicInteger();
        try {

            redisPublishRetryTemplate.execute(context -> {
                attempts.incrementAndGet();
                redisTemplate.convertAndSend(outbox.getChannel(), outbox.getPayload());
                return null;
            });

            return PublishResult.success(attempts.get());
        } catch (RuntimeException e) {
            return PublishResult.failure(attempts.get(), e);
        }
    }
}
