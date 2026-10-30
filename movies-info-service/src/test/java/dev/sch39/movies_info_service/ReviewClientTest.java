package dev.sch39.movies_info_service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.reactive.server.WebTestClient;

import dev.sch39.movies_info_service.grpc.ListReviewsRequest;
import dev.sch39.movies_info_service.grpc.ReviewReply;
import dev.sch39.movies_info_service.grpc.ReviewServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "spring.mongodb.database=movies_info_grpc_test")
@AutoConfigureWebTestClient
@AutoConfigureTestGrpcTransport
class ReviewClientTest {

    @TestConfiguration
    static class FakeReviewServer {
        @Bean
        ReviewServiceGrpc.ReviewServiceImplBase fakeReviewService() {
            return new ReviewServiceGrpc.ReviewServiceImplBase() {
                @Override
                public void streamReviews(ListReviewsRequest request, StreamObserver<ReviewReply> observer) {
                    switch ((int) request.getMovieInfoId()) {
                        case 1 -> {
                            observer.onNext(ReviewReply.newBuilder().setComment("Great").build());
                            observer.onNext(ReviewReply.newBuilder().setComment("Dark").build());
                            observer.onCompleted();
                        }
                        case 2 -> observer.onError(Status.UNAVAILABLE.asRuntimeException());
                        default -> observer.onError(Status.NOT_FOUND.asRuntimeException());
                    }
                }
            };
        }
    }

    private static final String URL = "/v1/movie-info";

    @Autowired
    private ReviewClient client;

    @Autowired
    private MovieInfoRepository repository;

    @Autowired
    private WebTestClient web;

    private String movieId;

    @BeforeEach
    void saveMovie() {
        repository.deleteAll().block();
        movieId = repository.save(new MovieInfo(null, "Batman Begins", 2005, List.of("Christian Bale")))
                .block().movieInfoId();
    }

    @Test
    void clientTurnsServerStreamIntoFluxAndStatusIntoError() {
        var comments = client.reviews(1).map(ReviewReply::getComment).collectList().block();
        System.out.println("comments=" + comments);
        assertEquals(List.of("Great", "Dark"), comments, "streamed comments");

        StepVerifier.create(client.reviews(99))
                .expectErrorSatisfies(error -> {
                    var code = ((StatusRuntimeException) error).getStatus().getCode();
                    System.out.println("errorCode=" + code);
                    assertEquals(Status.Code.NOT_FOUND, code, "status code");
                })
                .verify();
    }

    @Test
    void withReviewsMergesMovieAndReviewsAndTreatsNotFoundAsEmpty() {
        var merged = web.get().uri(URL + "/{id}/with-reviews?reviewMovieId=1", movieId)
                .exchange().expectStatus().isOk()
                .expectBody(MovieInfoReviewService.MovieWithReviews.class).returnResult().getResponseBody();
        var none = web.get().uri(URL + "/{id}/with-reviews?reviewMovieId=99", movieId)
                .exchange().expectStatus().isOk()
                .expectBody(MovieInfoReviewService.MovieWithReviews.class).returnResult().getResponseBody();

        System.out.println("merged=" + merged.movie().name() + " " + merged.reviews() + " none=" + none.reviews());
        assertEquals(List.of("Great", "Dark"), merged.reviews(), "merged reviews");
        assertEquals(List.of(), none.reviews(), "reviews for NOT_FOUND");
    }

    @Test
    void unavailableReviewServiceBecomes503() {
        var body = web.get().uri(URL + "/{id}/with-reviews?reviewMovieId=2", movieId)
                .exchange().expectStatus().isEqualTo(503)
                .expectBody(String.class).returnResult().getResponseBody();

        System.out.println("status=503 body=" + body);
        assertEquals("review service: UNAVAILABLE", body, "error body");
    }
}