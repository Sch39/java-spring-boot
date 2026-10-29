package dev.sch39.reactor_basics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class FallbacksTest {

    @Test
    void eagerFallbackRunsAtAssemblyTimeDeferredOnlyWhenEmpty() {
        Fallbacks eager = new Fallbacks();
        StepVerifier.create(eager.eager(Mono.just("value")))
                .expectNext("value")
                .verifyComplete();

        Fallbacks lazy = new Fallbacks();
        StepVerifier.create(lazy.lazy(Mono.just("value")))
                .expectNext("value")
                .verifyComplete();
        int withValue = lazy.fallbackCalls();
        StepVerifier.create(lazy.lazy(Mono.empty()))
                .expectNext("default")
                .verifyComplete();

        System.out.println("eager fallbackCalls=" + eager.fallbackCalls());
        System.out.println("lazy withValue=" + withValue + " afterEmpty=" + lazy.fallbackCalls());
        assertEquals(1, eager.fallbackCalls(), "eager fallbackCalls");
        assertEquals(0, withValue, "lazy withValue");
        assertEquals(1, lazy.fallbackCalls(), "lazy afterEmpty");
    }
}