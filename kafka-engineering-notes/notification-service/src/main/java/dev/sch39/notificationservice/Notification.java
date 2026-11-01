package dev.sch39.notificationservice;

public record Notification(String orderId, String customerId, long amount,
        String key, int partition, long offset, int port) {
}