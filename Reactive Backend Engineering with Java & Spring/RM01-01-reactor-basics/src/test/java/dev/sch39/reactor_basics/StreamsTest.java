package dev.sch39.reactor_basics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class StreamsTest {

    private final Streams streams = new Streams();

    @Test
    void concatKeepsSourceOrderMergeFollowsEmissionOrderZipPairsByIndex() {
        StepVerifier.withVirtualTime(() -> streams.concatenated().collectList())
                .thenAwait(Duration.ofSeconds(2))
                .assertNext(list -> {
                    System.out.println("concat=" + list);
                    assertEquals(List.of("A", "B", "C", "1", "2", "3"), list, "concat");
                })
                .verifyComplete();

        StepVerifier.withVirtualTime(() -> streams.merged().collectList())
                .thenAwait(Duration.ofSeconds(2))
                .assertNext(list -> {
                    System.out.println("merge=" + list);
                    assertEquals(List.of("A", "1", "B", "2", "C", "3"), list, "merge");
                })
                .verifyComplete();

        StepVerifier.withVirtualTime(() -> streams.zipped().collectList())
                .thenAwait(Duration.ofSeconds(2))
                .assertNext(list -> {
                    System.out.println("zip=" + list);
                    assertEquals(List.of("A1", "B2", "C3"), list, "zip");
                })
                .verifyComplete();
    }
}