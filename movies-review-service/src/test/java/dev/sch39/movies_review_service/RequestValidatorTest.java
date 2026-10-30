package dev.sch39.movies_review_service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Validation;
import reactor.test.StepVerifier;

class RequestValidatorTest {

    private final RequestValidator validator = new RequestValidator(
            Validation.buildDefaultValidatorFactory().getValidator());

    @Test
    void invalidReviewErrorsWithSortedMessages() {
        StepVerifier.create(validator.validate(new Review(null, null, " ", -1.0)))
                .expectErrorSatisfies(e -> {
                    var ex = (ResponseStatusException) e;
                    System.out.println("status=" + ex.getStatusCode() + " reason=" + ex.getReason());
                    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode(), "status");
                    assertEquals("comment must be present, movieInfoId must be present, rating must not be negative",
                            ex.getReason(), "reason");
                })
                .verify();
    }
}