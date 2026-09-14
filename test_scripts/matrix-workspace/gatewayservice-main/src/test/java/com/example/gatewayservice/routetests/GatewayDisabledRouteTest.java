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
        properties = "emailservice.integration=false"
)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
public class GatewayDisabledRouteTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void disabledEmailRouteReturnsNotFound() {
        webTestClient.get()
                .uri("/api/email/health")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
    }
}
