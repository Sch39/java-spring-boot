package dev.sch39.movies_review_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import io.grpc.stub.ServerCallStreamObserver;
import reactor.core.publisher.Flux;

class GrpcReactiveBridgeTest {

    static class FakeObserver extends ServerCallStreamObserver<Integer> {
        final List<Integer> received = new CopyOnWriteArrayList<>();
        volatile boolean ready = true;
        volatile boolean completed;
        int stopAfter = Integer.MAX_VALUE;
        Runnable onReady = () -> {
        };
        Runnable onCancel = () -> {
        };

        @Override
        public void onNext(Integer value) {
            received.add(value);
            if (received.size() == stopAfter) {
                ready = false;
            }
        }

        @Override
        public void onError(Throwable t) {
        }

        @Override
        public void onCompleted() {
            completed = true;
        }

        @Override
        public boolean isReady() {
            return ready;
        }

        @Override
        public void setOnReadyHandler(Runnable onReadyHandler) {
            this.onReady = onReadyHandler;
        }

        @Override
        public void setOnCancelHandler(Runnable onCancelHandler) {
            this.onCancel = onCancelHandler;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public void setCompression(String compression) {
        }

        @Override
        public void disableAutoInboundFlowControl() {
        }

        @Override
        public void request(int count) {
        }

        @Override
        public void setMessageCompression(boolean enable) {
        }
    }

    @Test
    void stopsRequestingWhenNotReadyAndResumesOnReady() {
        var requested = new AtomicLong();
        var observer = new FakeObserver();
        observer.stopAfter = 3;

        GrpcReactiveBridge.stream(Flux.range(1, 10).doOnRequest(requested::addAndGet), observer);
        System.out.println("notReady delivered=" + observer.received.size() + " requested=" + requested.get());
        assertEquals(3, observer.received.size(), "delivered while not ready");
        assertEquals(3, requested.get(), "requested while not ready");

        observer.ready = true;
        observer.onReady.run();
        System.out.println("ready delivered=" + observer.received.size() + " completed=" + observer.completed);
        assertEquals(10, observer.received.size(), "delivered after ready");
        assertTrue(observer.completed, "completed");
    }

    @Test
    void clientCancelDisposesUpstream() throws InterruptedException {
        var upstreamCancelled = new CountDownLatch(1);
        var observer = new FakeObserver();

        GrpcReactiveBridge.stream(
                Flux.interval(Duration.ofMillis(10)).map(Long::intValue).doOnCancel(upstreamCancelled::countDown),
                observer);
        while (observer.received.size() < 2) {
            Thread.sleep(5);
        }
        observer.onCancel.run();
        var cancelled = upstreamCancelled.await(1, TimeUnit.SECONDS);
        var sizeAtCancel = observer.received.size();
        Thread.sleep(100);

        System.out.println(
                "upstreamCancelled=" + cancelled + " afterCancel=" + (observer.received.size() - sizeAtCancel));
        assertTrue(cancelled, "upstream cancelled");
        assertEquals(sizeAtCancel, observer.received.size(), "items after cancel");
    }
}