package dev.sch39.orderservice;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderProducer {

    private final KafkaTemplate<String, OrderCreated> template;
    private final String topic;

    OrderProducer(KafkaTemplate<String, OrderCreated> template,
            @Value("${app.topic.orders}") String topic) {
        this.template = template;
        this.topic = topic;
    }

    public CompletableFuture<PublishResult> publish(OrderCreated event) {
        return template.send(topic, event.customerId(), event)
                .thenApply(r -> new PublishResult(
                        r.getRecordMetadata().topic(),
                        r.getRecordMetadata().partition(),
                        r.getRecordMetadata().offset()));
    }
}