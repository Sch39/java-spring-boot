package dev.sch39.orderservice;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
class KafkaTopicConfig {

    @Bean
    NewTopic ordersTopic(@Value("${app.topic.orders}") String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }
}