package dev.sch39.movies_review_service;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;

import dev.sch39.movies_review_service.grpc.AddReviewRequest;
import dev.sch39.movies_review_service.grpc.ListReviewsReply;
import dev.sch39.movies_review_service.grpc.ListReviewsRequest;
import dev.sch39.movies_review_service.grpc.ReviewReply;
import dev.sch39.movies_review_service.grpc.ReviewServiceGrpc;
import io.grpc.stub.StreamObserver;

@Service
public class ReviewGrpcService extends ReviewServiceGrpc.ReviewServiceImplBase {

    private final R2dbcEntityTemplate template;

    public ReviewGrpcService(R2dbcEntityTemplate template) {
        this.template = template;
    }

    @Override
    public void addReview(AddReviewRequest request, StreamObserver<ReviewReply> observer) {
        var review = new Review(null, request.getMovieInfoId(), request.getComment(), request.getRating());
        template.insert(review)
                .map(ReviewGrpcService::toReply)
                .subscribe(observer::onNext, observer::onError, observer::onCompleted);
    }

    @Override
    public void listReviews(ListReviewsRequest request, StreamObserver<ListReviewsReply> observer) {
        template.select(Review.class)
                .matching(query(where("movieInfoId").is(request.getMovieInfoId())))
                .all()
                .map(ReviewGrpcService::toReply)
                .collectList()
                .map(replies -> ListReviewsReply.newBuilder().addAllReviews(replies).build())
                .subscribe(observer::onNext, observer::onError, observer::onCompleted);
    }

    private static ReviewReply toReply(Review review) {
        return ReviewReply.newBuilder()
                .setReviewId(review.reviewId())
                .setMovieInfoId(review.movieInfoId())
                .setComment(review.comment())
                .setRating(review.rating())
                .build();
    }
}