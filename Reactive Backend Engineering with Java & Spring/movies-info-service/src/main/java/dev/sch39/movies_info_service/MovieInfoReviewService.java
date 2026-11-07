package dev.sch39.movies_info_service;

import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Service;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class MovieInfoReviewService {

    public record MovieWithReviews(MovieInfo movie, List<String> reviews) {
    }

    private final MovieInfoService movies;
    private final ReviewClient reviews;

    public MovieInfoReviewService(MovieInfoService movies, ReviewClient reviews) {
        this.movies = movies;
        this.reviews = reviews;
    }

    public Mono<MovieWithReviews> findWithReviews(String id, long reviewMovieId) {
        var comments = reviews.reviews(reviewMovieId)
                .timeout(Duration.ofSeconds(2))
                .onErrorResume(MovieInfoReviewService::isNotFound, e -> Flux.empty())
                .map(reply -> reply.getComment())
                .collectList();
        return movies.findById(id).zipWith(comments, MovieWithReviews::new);
    }

    private static boolean isNotFound(Throwable error) {
        return error instanceof StatusRuntimeException e && e.getStatus().getCode() == Status.Code.NOT_FOUND;
    }
}