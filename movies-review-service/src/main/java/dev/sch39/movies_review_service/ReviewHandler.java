package dev.sch39.movies_review_service;

import java.time.Duration;

import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitFailureHandler;

@Component
public class ReviewHandler {

    private final ReactiveMongoTemplate mongo;
    private final RequestValidator validator;
    private final Sinks.Many<Review> newReviews = Sinks.many().multicast().onBackpressureBuffer();

    public ReviewHandler(ReactiveMongoTemplate mongo, RequestValidator validator) {
        this.mongo = mongo;
        this.validator = validator;
    }

    public Mono<ServerResponse> addReview(ServerRequest request) {
        return request.bodyToMono(Review.class)
                .switchIfEmpty(
                        Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "body must not be empty")))
                .flatMap(validator::validate)
                .flatMap(mongo::save)
                .doOnNext(saved -> newReviews.emitNext(saved, EmitFailureHandler.busyLooping(Duration.ofMillis(200))))
                .flatMap(saved -> ServerResponse.status(HttpStatus.CREATED).bodyValue(saved));
    }

    public Mono<ServerResponse> streamReviews(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(newReviews.asFlux(), Review.class);
    }
}