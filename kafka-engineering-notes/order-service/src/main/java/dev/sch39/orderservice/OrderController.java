package dev.sch39.orderservice;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
class OrderController {

    record CreateOrderRequest(String customerId, long amount) {
    }

    private final OrderProducer producer;

    OrderController(OrderProducer producer) {
        this.producer = producer;
    }

    @PostMapping
    PublishResult create(@RequestBody CreateOrderRequest request) {
        return producer.publish(newEvent(request.customerId(), request.amount())).join();
    }

    @PostMapping("/bulk")
    List<PublishResult> bulk(@RequestParam String customerId, @RequestParam int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> producer.publish(newEvent(customerId, 100 + i)))
                .toList().stream()
                .map(f -> f.join())
                .toList();
    }

    private static OrderCreated newEvent(String customerId, long amount) {
        return new OrderCreated(UUID.randomUUID().toString(), customerId, amount);
    }
}