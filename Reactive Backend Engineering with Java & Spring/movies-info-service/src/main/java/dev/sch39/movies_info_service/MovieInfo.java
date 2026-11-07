package dev.sch39.movies_info_service;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Document
public record MovieInfo(
        @Id String movieInfoId,
        @NotBlank(message = "name must be present") String name,
        @NotNull @Positive(message = "year must be a positive number") Integer year,
        @NotEmpty(message = "cast must not be empty") List<String> cast) {
}