package dev.sch39.orderservice;

public record PublishResult(String topic, int partition, long offset) {
}