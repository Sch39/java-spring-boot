package dev.sch39.notificationservice;

import static dev.sch39.notificationservice.KafkaTestSupport.await;
import static dev.sch39.notificationservice.KafkaTestSupport.committedTotal;
import static dev.sch39.notificationservice.KafkaTestSupport.json;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationConsumerTest {

    static final String TOPIC = "orders-test-" + UUID.randomUUID();
    static final String GROUP = "notif-" + UUID.randomUUID();

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

    @Autowired
    MockMvc mvc;

    private long lagTotal() throws Exception {
        String body = mvc.perform(get("/consumer/assignment")).andReturn().getResponse().getContentAsString();
        List<Number> lags = JsonPath.read(body, "$.partitions[*].lag");
        return lags.stream().mapToLong(Number::longValue).sum();
    }

    private boolean paused() throws Exception {
        String body = mvc.perform(get("/consumer/assignment")).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.paused");
    }

    @Test
    void listenerReceivesPartitionAndOffset() throws Exception {
        for (String customer : List.of("c1", "c2", "c3", "c1")) {
            template.send(TOPIC, customer, json(UUID.randomUUID().toString(), customer, 100)).get();
        }
        assertTrue(await(() -> store.all().size() >= 4, Duration.ofSeconds(20)),
                "notifications: got " + store.all().size() + ", want >= 4");
        String body = mvc.perform(get("/notifications")).andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> items = JsonPath.read(body, "$[?(@.key == 'c1')]");
        items.forEach(n -> System.out.println(
                "key=" + n.get("key") + " partition=" + n.get("partition") + " offset=" + n.get("offset")
                        + " port=" + n.get("port")));
        Set<Object> c1Partitions = items.stream().map(n -> n.get("partition")).collect(Collectors.toSet());
        assertEquals(2, items.size(), "records for key c1: got " + items.size() + ", want 2");
        assertEquals(1, c1Partitions.size(), "partitions for key c1: got " + c1Partitions + ", want 1");
    }

    @Test
    void offsetIsCommittedAfterAck() throws Exception {
        int before = store.all().size();
        template.send(TOPIC, "c-ack", json(UUID.randomUUID().toString(), "c-ack", 1)).get();
        assertTrue(await(() -> store.all().size() > before, Duration.ofSeconds(20)), "record not consumed");
        long expected = store.all().size();
        assertTrue(await(() -> {
            try {
                return committedTotal(GROUP) == expected;
            } catch (Exception e) {
                return false;
            }
        }, Duration.ofSeconds(20)), "committed: got " + committedTotal(GROUP) + ", want " + expected);
        System.out.println("consumed=" + expected + " committed=" + committedTotal(GROUP));
    }

    @Test
    void pauseBuildsLagAndResumeDrainsIt() throws Exception {
        assertTrue(await(() -> {
            try {
                return lagTotal() == 0;
            } catch (Exception e) {
                return false;
            }
        }, Duration.ofSeconds(20)), "lag not zero before pause");
        mvc.perform(post("/consumer/pause"));
        assertTrue(await(() -> {
            try {
                return paused();
            } catch (Exception e) {
                return false;
            }
        }, Duration.ofSeconds(20)), "container not paused");
        int before = store.all().size();
        for (int i = 0; i < 3; i++) {
            template.send(TOPIC, "c-pause", json(UUID.randomUUID().toString(), "c-pause", i)).get();
        }
        Thread.sleep(3000);
        long lagWhilePaused = lagTotal();
        int consumedWhilePaused = store.all().size() - before;
        System.out.println("paused lag=" + lagWhilePaused + " consumed=" + consumedWhilePaused);
        assertEquals(3, lagWhilePaused, "lag while paused: got " + lagWhilePaused + ", want 3");
        assertEquals(0, consumedWhilePaused, "consumed while paused: got " + consumedWhilePaused + ", want 0");

        mvc.perform(post("/consumer/resume"));
        assertTrue(await(() -> {
            try {
                return lagTotal() == 0 && store.all().size() == before + 3;
            } catch (Exception e) {
                return false;
            }
        }, Duration.ofSeconds(20)), "lag after resume: got " + lagTotal() + ", want 0");
        System.out.println("resumed lag=" + lagTotal() + " consumed=" + (store.all().size() - before));
    }
}