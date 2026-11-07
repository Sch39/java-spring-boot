package dev.sch39.movies_review_service;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.relational.core.mapping.Table;

@Document
@Table("review")
public record Review(
                @Id String reviewId,
                @NotNull(message = "movieInfoId must be present") Long movieInfoId,
                @NotBlank(message = "comment must be present") String comment,
                @NotNull(message = "rating must be present") @DecimalMin(value = "0.0", message = "rating must not be negative") @DecimalMax(value = "10.0", message = "rating must not be greater than 10") Double rating) {
}