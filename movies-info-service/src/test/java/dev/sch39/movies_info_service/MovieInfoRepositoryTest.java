package dev.sch39.movies_info_service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;

import reactor.test.StepVerifier;

@DataMongoTest
class MovieInfoRepositoryTest {

    @Autowired
    private MovieInfoRepository repository;

    @Test
    void saveGeneratesIdOnEmbeddedMongo() {
        StepVerifier.create(repository.save(new MovieInfo(null, "Inception", 2010, List.of("Leonardo DiCaprio"))))
                .assertNext(saved -> {
                    System.out.println("saved=" + saved);
                    assertNotNull(saved.movieInfoId(), "generated id");
                })
                .verifyComplete();
    }
}