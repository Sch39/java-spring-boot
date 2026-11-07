package dev.sch39.reactor_basics;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ThreadControllerTest {

    @Autowired
    private WebTestClient client;

    @Test
    void blockingWorkMovesOffTheNettyEventLoop() {
        String eventLoop = client.get().uri("/thread").exchange()
                .returnResult(String.class).getResponseBody().blockFirst();
        String offloaded = client.get().uri("/thread/blocking").exchange()
                .returnResult(String.class).getResponseBody().blockFirst();

        System.out.println("eventLoop thread=" + eventLoop);
        System.out.println("offloaded thread=" + offloaded);
        assertTrue(eventLoop.startsWith("webflux-http-"), "eventLoop thread " + eventLoop);
        assertTrue(offloaded.startsWith("boundedElastic-"), "offloaded thread " + offloaded);
    }
}