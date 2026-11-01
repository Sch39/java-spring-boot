package dev.sch39.notificationservice;

public record OrderCreated(String orderId, String customerId, long amount) {
}