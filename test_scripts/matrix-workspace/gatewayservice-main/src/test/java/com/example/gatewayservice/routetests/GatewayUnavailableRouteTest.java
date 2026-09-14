package com.example.gatewayservice.routetests;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "service.auth.url=http://localhost:1"
)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
public class GatewayUnavailableRouteTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void unavailableAuthRouteReturnsInternalServerError() {
        webTestClient.get()
                .uri("/api/auth/health")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
