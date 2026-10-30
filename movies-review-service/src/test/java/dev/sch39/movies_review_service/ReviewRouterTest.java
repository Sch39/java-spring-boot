package dev.sch39.movies_review_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

@WebFluxTest
@Import({ ReviewRouter.class, ReviewHandler.class, RequestValidator.class })
class ReviewRouterTest {

    @Autowired
    private WebTestClient client;

    @MockitoBean
    private ReactiveMongoTemplate mongo;

    @Test
    void invalidBodyIs400AndUnknownRouteIs404() {
        int invalid = client.post().uri("/v1/reviews")
                .bodyValue(new Review(null, null, "", -1.0))
                .exchange()
                .expectBody().returnResult().getStatus().value();
        int unknown = client.get().uri("/v1/unknown")
                .exchange()
                .expectBody().returnResult().getStatus().value();

        System.out.println("invalid=" + invalid + " unknown=" + unknown);
        assertEquals(400, invalid, "invalid body status");
        assertEquals(404, unknown, "unknown route status");
        verifyNoInteractions(mongo);
    }

    @Test
    void streamEmitsReviewsAddedAfterSubscribe() {
        when(mongo.save(any(Review.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        // header SSE baru terkirim saat event pertama ditulis, jadi review dikirim dari
        // thread lain
        Schedulers.boundedElastic().schedule(() -> {
            sleep(500);
            postReview("first");
            postReview("second");
        });

        var stream = client.get().uri("/v1/reviews/stream")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .returnResult(Review.class)
                .getResponseBody();

        StepVerifier.create(stream.take(2))
                .assertNext(r -> {
                    System.out.println("sse1=" + r.comment());
                    assertEquals("first", r.comment(), "first event");
                })
                .assertNext(r -> {
                    System.out.println("sse2=" + r.comment());
                    assertEquals("second", r.comment(), "second event");
                })
                .verifyComplete();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void postReview(String comment) {
        client.post().uri("/v1/reviews")
                .bodyValue(new Review(null, 1L, comment, 7.5))
                .exchange()
                .expectStatus().isCreated();
    }
}