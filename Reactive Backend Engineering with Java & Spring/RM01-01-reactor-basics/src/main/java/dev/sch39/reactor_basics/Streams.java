package dev.sch39.reactor_basics;

import java.time.Duration;
import reactor.core.publisher.Flux;

public class Streams {

    public Flux<String> letters() {
        return Flux.just("A", "B", "C").delayElements(Duration.ofMillis(100));
    }

    public Flux<String> digits() {
        // mulai 50 ms setelah letters supaya emisi saling menyela
        return Flux.just("1", "2", "3").delayElements(Duration.ofMillis(100))
                .delaySubscription(Duration.ofMillis(50));
    }

    public Flux<String> concatenated() {
        return Flux.concat(letters(), digits());
    }

    public Flux<String> merged() {
        return Flux.merge(letters(), digits());
    }

    public Flux<String> zipped() {
        return letters().zipWith(digits(), (letter, digit) -> letter + digit);
    }
}