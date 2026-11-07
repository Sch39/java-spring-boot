package dev.sch39.movies_info_service;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class MovieInfoService {

    private final MovieInfoRepository repository;

    public MovieInfoService(MovieInfoRepository repository) {
        this.repository = repository;
    }

    public Mono<MovieInfo> add(MovieInfo movieInfo) {
        return repository.save(movieInfo);
    }

    public Flux<MovieInfo> findAll() {
        return repository.findAll();
    }

    public Mono<MovieInfo> findById(String id) {
        return repository.findById(id);
    }

    public Mono<MovieInfo> update(String id, MovieInfo movieInfo) {
        return repository.findById(id)
                .map(existing -> new MovieInfo(id, movieInfo.name(), movieInfo.year(), movieInfo.cast()))
                .flatMap(repository::save);
    }

    public Mono<Void> delete(String id) {
        return repository.deleteById(id);
    }
}