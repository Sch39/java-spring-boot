package dev.sch39.movies_info_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(MovieInfoController.class)
class MovieInfoValidationTest {

    @Autowired
    private WebTestClient client;

    @MockitoBean
    private MovieInfoService service;

    @Test
    void invalidBodyReturns400WithFieldMessages() {
        String body = client.post().uri("/v1/movie-info")
                .bodyValue(new MovieInfo(null, "", -1, List.of()))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class).returnResult().getResponseBody();

        System.out.println("400 body=" + body);
        assertEquals("cast: cast must not be empty, name: name must be present, year: year must be a positive number",
                body, "error body");
        verifyNoInteractions(service);
    }

    @Test
    void unexpectedExceptionReturns500WithoutLeakingDetails() {
        when(service.add(any())).thenThrow(new IllegalStateException("db password=secret"));

        String body = client.post().uri("/v1/movie-info")
                .bodyValue(new MovieInfo(null, "Batman Begins", 2005, List.of("Christian Bale")))
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(String.class).returnResult().getResponseBody();

        System.out.println("500 body=" + body);
        assertEquals("internal error", body, "error body");
    }
}