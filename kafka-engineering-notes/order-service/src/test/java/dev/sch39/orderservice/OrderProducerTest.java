package dev.sch39.orderservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.RecordDeserializationException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OrderProducerTest {

    static final String TOPIC = "orders-test-" + UUID.randomUUID();

    @DynamicPropertySource
    static void topic(DynamicPropertyRegistry registry) {
        registry.add("app.topic.orders", () -> TOPIC);
    }

    @AfterAll
    static void deleteTopic() throws Exception {
        try (AdminClient admin = AdminClient.create(Map.<String, Object>of("bootstrap.servers", "localhost:9092"))) {
            admin.deleteTopics(List.of(TOPIC)).all().get();
        }
    }

    @Autowired
    OrderProducer producer;

    @Autowired
    MockMvc mvc;

    private static KafkaConsumer<String, OrderCreated> consumer(String trustedPackages) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        if (trustedPackages != null) {
            props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, trustedPackages);
        }
        KafkaConsumer<String, OrderCreated> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of(TOPIC));
        return consumer;
    }

    @Test
    void sameKeyGoesToSamePartition() {
        Set<Integer> partitions = java.util.stream.IntStream.range(0, 5)
                .mapToObj(i -> producer.publish(new OrderCreated(UUID.randomUUID().toString(), "c-same", i)).join())
                .map(PublishResult::partition)
                .collect(Collectors.toSet());
        System.out.println("key=c-same partitions=" + partitions);
        assertEquals(1, partitions.size(), "partitions for one key: got " + partitions + ", want 1");
    }

    @Test
    void postOrderReturnsRecordMetadata() throws Exception {
        mvc.perform(post("/orders").contentType("application/json")
                .content("{\"customerId\":\"c-http\",\"amount\":250}"))
                .andDo(r -> System.out.println("POST /orders -> " + r.getResponse().getContentAsString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topic").value(TOPIC))
                .andExpect(jsonPath("$.partition").isNumber())
                .andExpect(jsonPath("$.offset").isNumber());
    }

    @Test
    void bulkWithSameCustomerStaysOnOnePartition() throws Exception {
        String body = mvc.perform(post("/orders/bulk").param("customerId", "c-bulk").param("count", "5"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Integer> partitions = com.jayway.jsonpath.JsonPath.read(body, "$[*].partition");
        System.out.println("POST /orders/bulk partitions=" + partitions);
        assertEquals(5, partitions.size(), "records: got " + partitions.size() + ", want 5");
        assertEquals(1, Set.copyOf(partitions).size(), "partitions: got " + partitions + ", want one distinct value");
    }

    @Test
    void consumerWithDefaultTrustedPackagesFails() {
        producer.publish(new OrderCreated(UUID.randomUUID().toString(), "c-broken", 1)).join();
        try (KafkaConsumer<String, OrderCreated> c = consumer(null)) {
            RecordDeserializationException ex = assertThrows(RecordDeserializationException.class, () -> {
                long end = System.currentTimeMillis() + 15_000;
                while (System.currentTimeMillis() < end) {
                    c.poll(Duration.ofMillis(500));
                }
            });
            System.out.println("error=" + ex.getCause().getMessage());
        }
    }

    @Test
    void consumerWithTrustedPackagesReadsEvent() {
        OrderCreated sent = new OrderCreated(UUID.randomUUID().toString(), "c-fixed", 42);
        producer.publish(sent).join();
        try (KafkaConsumer<String, OrderCreated> c = consumer("dev.sch39.orderservice")) {
            long end = System.currentTimeMillis() + 15_000;
            OrderCreated got = null;
            while (got == null && System.currentTimeMillis() < end) {
                for (ConsumerRecord<String, OrderCreated> r : c.poll(Duration.ofMillis(500))) {
                    if (r.value().orderId().equals(sent.orderId())) {
                        got = r.value();
                        System.out.println("key=" + r.key() + " value=" + got);
                    }
                }
            }
            assertNotNull(got, "event not consumed: got null, want " + sent);
            assertEquals(sent, got, "event: got " + got + ", want " + sent);
        }
    }
}
