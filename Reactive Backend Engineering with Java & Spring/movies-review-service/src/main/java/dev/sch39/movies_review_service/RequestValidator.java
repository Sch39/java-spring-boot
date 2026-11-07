package dev.sch39.movies_review_service;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import reactor.core.publisher.Mono;

@Component
public class RequestValidator {

    private final Validator validator;

    public RequestValidator(Validator validator) {
        this.validator = validator;
    }

    public <T> Mono<T> validate(T target) {
        var violations = validator.validate(target);
        if (violations.isEmpty()) {
            return Mono.just(target);
        }
        var message = violations.stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .collect(Collectors.joining(", "));
        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, message));
    }
}