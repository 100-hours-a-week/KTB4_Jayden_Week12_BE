package com.example.spring_rest_api.common.config;

import com.example.spring_rest_api.chat.service.subscriber.ChatMessageRedisSubscriber;
import com.example.spring_rest_api.chat.service.subscriber.ChatUpdateRedisSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisSubscriberConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            ChatMessageRedisSubscriber messageSubscriber,
            ChatUpdateRedisSubscriber updateSubscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        container.addMessageListener(
                messageSubscriber,
                new ChannelTopic("chat.message.v1")
        );
        container.addMessageListener(
                messageSubscriber,
                new ChannelTopic("chat.user-update.v1")
        );

        return container;
    }
}
