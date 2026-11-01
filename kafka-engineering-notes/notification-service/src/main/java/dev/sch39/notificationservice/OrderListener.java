package dev.sch39.notificationservice;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
class OrderListener {

    private final NotificationStore store;
    private final int port;
    private final boolean ackEnabled;

    OrderListener(NotificationStore store,
            @Value("${server.port}") int port,
            @Value("${app.ack-enabled}") boolean ackEnabled) {
        this.store = store;
        this.port = port;
        this.ackEnabled = ackEnabled;
    }

    @KafkaListener(id = "${app.listener.id}", topics = "${app.topic.orders}")
    void onOrder(ConsumerRecord<String, OrderCreated> record, Acknowledgment ack) {
        OrderCreated event = record.value();
        store.add(new Notification(event.orderId(), event.customerId(), event.amount(),
                record.key(), record.partition(), record.offset(), port));
        if (ackEnabled) {
            ack.acknowledge();
        }
    }
}