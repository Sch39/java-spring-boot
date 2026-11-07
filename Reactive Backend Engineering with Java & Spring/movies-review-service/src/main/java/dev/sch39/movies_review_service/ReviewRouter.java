package dev.sch39.movies_review_service;

import static org.springframework.web.reactive.function.server.RequestPredicates.path;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class ReviewRouter {

    @Bean
    public RouterFunction<ServerResponse> reviewRoute(ReviewHandler handler) {
        return route()
                .nest(path("/v1/reviews"), builder -> builder
                        .GET("/stream", handler::streamReviews)
                        .POST("", handler::addReview))
                .build();
    }
}