package dev.sch39.notificationservice;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/consumer")
class ConsumerController {

    record PartitionLag(int partition, long committed, long end, long lag) {
    }

    record Assignment(int port, boolean paused, List<PartitionLag> partitions) {
    }

    private final KafkaListenerEndpointRegistry registry;
    private final KafkaAdmin kafkaAdmin;
    private final String listenerId;
    private final int port;

    ConsumerController(KafkaListenerEndpointRegistry registry, KafkaAdmin kafkaAdmin,
            @Value("${app.listener.id}") String listenerId,
            @Value("${server.port}") int port) {
        this.registry = registry;
        this.kafkaAdmin = kafkaAdmin;
        this.listenerId = listenerId;
        this.port = port;
    }

    @GetMapping("/assignment")
    Assignment assignment() throws Exception {
        MessageListenerContainer container = registry.getListenerContainer(listenerId);
        List<TopicPartition> assigned = container.getAssignedPartitions().stream()
                .sorted(Comparator.comparingInt(TopicPartition::partition))
                .toList();
        return new Assignment(port, container.isContainerPaused(), lag(assigned));
    }

    @PostMapping("/pause")
    void pause() {
        registry.getListenerContainer(listenerId).pause();
    }

    @PostMapping("/resume")
    void resume() {
        registry.getListenerContainer(listenerId).resume();
    }

    private List<PartitionLag> lag(List<TopicPartition> assigned) throws Exception {
        if (assigned.isEmpty()) {
            return List.of();
        }
        try (Admin admin = Admin.create(kafkaAdmin.getConfigurationProperties())) {
            Map<TopicPartition, OffsetAndMetadata> committed = admin.listConsumerGroupOffsets(listenerId)
                    .partitionsToOffsetAndMetadata().get();
            Map<TopicPartition, ListOffsetsResultInfo> end = admin.listOffsets(
                    assigned.stream().collect(Collectors.toMap(Function.identity(), tp -> OffsetSpec.latest())))
                    .all().get();
            return assigned.stream().map(tp -> {
                OffsetAndMetadata c = committed.get(tp);
                long committedOffset = c == null ? 0 : c.offset();
                long endOffset = end.get(tp).offset();
                return new PartitionLag(tp.partition(), committedOffset, endOffset, endOffset - committedOffset);
            }).toList();
        }
    }

}
