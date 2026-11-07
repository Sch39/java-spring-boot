package dev.sch39.reactor_basics;

import java.util.concurrent.atomic.AtomicInteger;
import reactor.core.publisher.Mono;

public class Fallbacks {

    private final AtomicInteger fallbackCalls = new AtomicInteger();

    public Mono<String> eager(Mono<String> source) {
        return source.switchIfEmpty(fallback());
    }

    public Mono<String> lazy(Mono<String> source) {
        return source.switchIfEmpty(Mono.defer(this::fallback));
    }

    public int fallbackCalls() {
        return fallbackCalls.get();
    }

    private Mono<String> fallback() {
        fallbackCalls.incrementAndGet();
        return Mono.just("default");
    }
}