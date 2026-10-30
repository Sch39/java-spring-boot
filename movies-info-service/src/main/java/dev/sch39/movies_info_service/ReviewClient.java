package dev.sch39.movies_info_service;

import org.springframework.grpc.client.ImportGrpcClients;
import org.springframework.stereotype.Component;

import dev.sch39.movies_info_service.grpc.ListReviewsRequest;
import dev.sch39.movies_info_service.grpc.ReviewReply;
import dev.sch39.movies_info_service.grpc.ReviewServiceGrpc;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import reactor.core.publisher.Flux;

@Component
@ImportGrpcClients(target = "review", types = ReviewServiceGrpc.ReviewServiceStub.class)
public class ReviewClient {

    private final ReviewServiceGrpc.ReviewServiceStub stub;

    public ReviewClient(ReviewServiceGrpc.ReviewServiceStub stub) {
        this.stub = stub;
    }

    public Flux<ReviewReply> reviews(long movieInfoId) {
        var request = ListReviewsRequest.newBuilder().setMovieInfoId(movieInfoId).build();
        return Flux.create(
                sink -> stub.streamReviews(request, new ClientResponseObserver<ListReviewsRequest, ReviewReply>() {
                    @Override
                    public void beforeStart(ClientCallStreamObserver<ListReviewsRequest> call) {
                        sink.onCancel(() -> call.cancel("subscriber cancelled", null));
                    }

                    @Override
                    public void onNext(ReviewReply reply) {
                        sink.next(reply);
                    }

                    @Override
                    public void onError(Throwable error) {
                        sink.error(error);
                    }

                    @Override
                    public void onCompleted() {
                        sink.complete();
                    }
                }));
    }
}