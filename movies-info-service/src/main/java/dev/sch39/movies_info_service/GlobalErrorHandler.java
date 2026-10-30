package dev.sch39.movies_info_service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.util.concurrent.TimeoutException;
import io.grpc.StatusRuntimeException;

@ControllerAdvice
public class GlobalErrorHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<String> handleBindException(WebExchangeBindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .reduce((a, b) -> a + ", " + b)
                .orElse("invalid request");
        return ResponseEntity.badRequest().body(message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("internal error");
    }

    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<String> handleGrpcStatus(StatusRuntimeException ex) {
        var status = switch (ex.getStatus().getCode()) {
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case DEADLINE_EXCEEDED -> HttpStatus.GATEWAY_TIMEOUT;
            default -> HttpStatus.BAD_GATEWAY;
        };
        return ResponseEntity.status(status).body("review service: " + ex.getStatus().getCode());
    }

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<String> handleTimeout(TimeoutException ex) {
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body("review service: timeout");
    }
}