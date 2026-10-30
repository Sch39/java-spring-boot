package dev.sch39.movies_info_service;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public record MovieInfo(@Id String movieInfoId, String name, Integer year, List<String> cast) {
}