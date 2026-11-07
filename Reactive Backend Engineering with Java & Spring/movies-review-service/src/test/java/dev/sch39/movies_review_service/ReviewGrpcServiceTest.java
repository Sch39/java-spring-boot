package dev.sch39.movies_review_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.grpc.client.ImportGrpcClients;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import dev.sch39.movies_review_service.grpc.AddReviewRequest;
import dev.sch39.movies_review_service.grpc.ListReviewsRequest;
import dev.sch39.movies_review_service.grpc.ReviewServiceGrpc;

import java.util.ArrayList;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = "spring.sql.init.mode=always")
@Testcontainers
@AutoConfigureTestGrpcTransport
@ImportGrpcClients(types = ReviewServiceGrpc.ReviewServiceBlockingStub.class)
class ReviewGrpcServiceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private ReviewServiceGrpc.ReviewServiceBlockingStub stub;

    @Test
    void addReviewReturnsIdGeneratedByPostgres() {
        var reply = stub.addReview(AddReviewRequest.newBuilder()
                .setMovieInfoId(1L).setComment("Great").setRating(9.0).build());

        System.out.println("reviewId=" + reply.getReviewId() + " comment=" + reply.getComment());
        assertFalse(reply.getReviewId().isBlank(), "generated review id");
        assertEquals("Great", reply.getComment(), "comment");
    }

    @Test
    void listReviewsReturnsOnlyRequestedMovie() {
        stub.addReview(AddReviewRequest.newBuilder().setMovieInfoId(42L).setComment("A").setRating(5.0).build());
        stub.addReview(AddReviewRequest.newBuilder().setMovieInfoId(42L).setComment("B").setRating(6.0).build());
        stub.addReview(AddReviewRequest.newBuilder().setMovieInfoId(43L).setComment("C").setRating(7.0).build());

        var reply = stub.listReviews(ListReviewsRequest.newBuilder().setMovieInfoId(42L).build());

        System.out.println("movie42 count=" + reply.getReviewsCount());
        assertEquals(2, reply.getReviewsCount(), "reviews for movie 42");
    }

    @Test
    void streamReviewsDeliversRowsAndMapsMissingMovieToNotFound() {
        stub.addReview(AddReviewRequest.newBuilder().setMovieInfoId(77L).setComment("A").setRating(5.0).build());
        stub.addReview(AddReviewRequest.newBuilder().setMovieInfoId(77L).setComment("B").setRating(6.0).build());

        var streamed = new ArrayList<String>();
        stub.streamReviews(ListReviewsRequest.newBuilder().setMovieInfoId(77L).build())
                .forEachRemaining(reply -> streamed.add(reply.getComment()));
        var error = assertThrows(StatusRuntimeException.class,
                () -> stub.streamReviews(ListReviewsRequest.newBuilder().setMovieInfoId(999L).build()).hasNext());

        System.out.println("streamed=" + streamed.size() + " missing=" + error.getStatus().getCode());
        assertEquals(2, streamed.size(), "streamed reviews");
        assertEquals(Status.Code.NOT_FOUND, error.getStatus().getCode(), "status code");
    }
}