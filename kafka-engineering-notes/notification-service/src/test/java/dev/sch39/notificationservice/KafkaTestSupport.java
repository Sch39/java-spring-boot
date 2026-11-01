package dev.sch39.notificationservice;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;

final class KafkaTestSupport {

    private static final Map<String, Object> CONFIG = Map.of("bootstrap.servers", "localhost:9092");

    private KafkaTestSupport() {
    }

    static void createTopic(String topic, int partitions) throws Exception {
        try (Admin admin = Admin.create(CONFIG)) {
            admin.createTopics(List.of(new NewTopic(topic, partitions, (short) 1))).all().get();
        }
    }

    static void deleteTopic(String topic) throws Exception {
        try (Admin admin = Admin.create(CONFIG)) {
            admin.deleteTopics(List.of(topic)).all().get();
        }
    }

    static long committedTotal(String group) throws Exception {
        try (Admin admin = Admin.create(CONFIG)) {
            return admin.listConsumerGroupOffsets(group).partitionsToOffsetAndMetadata().get()
                    .values().stream().mapToLong(OffsetAndMetadata::offset).sum();
        }
    }

    static boolean await(BooleanSupplier condition, Duration timeout) throws InterruptedException {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(200);
        }
        return condition.getAsBoolean();
    }

    static String json(String orderId, String customerId, long amount) {
        return "{\"orderId\":\"" + orderId + "\",\"customerId\":\"" + customerId + "\",\"amount\":" + amount + "}";
    }
}