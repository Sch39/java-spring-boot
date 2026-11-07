package dev.sch39.movies_review_service;

import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.Subscription;

import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class GrpcReactiveBridge {

    private GrpcReactiveBridge() {
    }

    public static <T> void respond(Mono<T> source, StreamObserver<T> observer) {
        source.subscribe(observer::onNext, observer::onError, observer::onCompleted);
    }

    public static <T> void stream(Flux<T> source, StreamObserver<T> observer) {
        var call = (ServerCallStreamObserver<T>) observer;
        var inFlight = new AtomicBoolean();

        var subscriber = new BaseSubscriber<T>() {
            @Override
            protected void hookOnSubscribe(Subscription subscription) {
                pump();
            }

            @Override
            protected void hookOnNext(T value) {
                call.onNext(value);
                inFlight.set(false);
                pump();
            }

            @Override
            protected void hookOnComplete() {
                call.onCompleted();
            }

            @Override
            protected void hookOnError(Throwable error) {
                call.onError(error);
            }

            void pump() {
                if (call.isReady() && inFlight.compareAndSet(false, true)) {
                    request(1);
                }
            }
        };

        call.setOnReadyHandler(subscriber::pump);
        call.setOnCancelHandler(subscriber::dispose);
        source.subscribe(subscriber);
    }
}