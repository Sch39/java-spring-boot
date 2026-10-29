package dev.sch39.reactor_basics;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
public class ThreadController {

    @GetMapping("/thread")
    public Mono<String> eventLoop() {
        return Mono.fromSupplier(() -> Thread.currentThread().getName());
    }

    @GetMapping("/thread/blocking")
    public Mono<String> offloaded() {
        return Mono.fromCallable(() -> {
            Thread.sleep(50);
            return Thread.currentThread().getName();
        }).subscribeOn(Schedulers.boundedElastic());
    }
}