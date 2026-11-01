package dev.sch39.orderservice;

public record OrderCreated(String orderId, String customerId, long amount) {
}