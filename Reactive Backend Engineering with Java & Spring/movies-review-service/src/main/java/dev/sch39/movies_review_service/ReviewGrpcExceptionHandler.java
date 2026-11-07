package dev.sch39.movies_review_service;

import java.util.NoSuchElementException;

import org.springframework.grpc.server.exception.GrpcExceptionHandler;
import org.springframework.stereotype.Component;

import io.grpc.Status;
import io.grpc.StatusException;

@Component
public class ReviewGrpcExceptionHandler implements GrpcExceptionHandler {

    @Override
    public StatusException handleException(Throwable exception) {
        if (exception instanceof NoSuchElementException) {
            return Status.NOT_FOUND.withDescription(exception.getMessage()).asException();
        }
        return null;
    }
}