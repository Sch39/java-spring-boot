package dev.sch39.notificationservice;

import static dev.sch39.notificationservice.KafkaTestSupport.await;
import static dev.sch39.notificationservice.KafkaTestSupport.committedTotal;
import static dev.sch39.notificationservice.KafkaTestSupport.json;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = "app.ack-enabled=false")
class NotificationNoAckTest {

    static final String TOPIC = "orders-noack-" + UUID.randomUUID();
    static final String GROUP = "notif-noack-" + UUID.randomUUID();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("app.topic.orders", () -> TOPIC);
        registry.add("app.listener.id", () -> GROUP);
    }

    @BeforeAll
    static void createTopic() throws Exception {
        KafkaTestSupport.createTopic(TOPIC, 2);
    }

    @AfterAll
    static void deleteTopic() throws Exception {
        KafkaTestSupport.deleteTopic(TOPIC);
    }

    @Autowired
    NotificationStore store;

    @Autowired
    KafkaTemplate<String, String> template;

    @Test
    void offsetStaysUncommittedWithoutAck() throws Exception {
        for (int i = 0; i < 3; i++) {
            template.send(TOPIC, "c-noack", json(UUID.randomUUID().toString(), "c-noack", i)).get();
        }
        assertTrue(await(() -> store.all().size() == 3, Duration.ofSeconds(20)),
                "consumed: got " + store.all().size() + ", want 3");
        Thread.sleep(3000);
        long committed = committedTotal(GROUP);
        System.out.println("consumed=" + store.all().size() + " committed=" + committed);
        assertEquals(0, committed, "committed without ack: got " + committed + ", want 0");
    }
}