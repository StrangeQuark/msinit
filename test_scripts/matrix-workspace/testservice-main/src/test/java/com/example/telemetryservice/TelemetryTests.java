

package com.example.telemetryservice;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.example.authservice.AuthFunctions;
import com.example.utility.ExtentTestWatcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ExtentTestWatcher.class)
@EnabledIfEnvironmentVariable(named = "TELEMETRYSERVICE_INTEGRATION", matches = "true")
@EnabledIfEnvironmentVariable(named = "AUTHSERVICE_INTEGRATION", matches = "true")
@EnabledIfEnvironmentVariable(named = "EMAILSERVICE_INTEGRATION", matches = "true")
public class TelemetryTests {
    private static Playwright playwright;
    private static APIRequestContext apiRequestContext;
    private static TelemetryFunctions telemetryFunctions;

    @BeforeAll
    public static void beforeAll() {
        playwright = Playwright.create();
        apiRequestContext = playwright.request().newContext();
        telemetryFunctions = new TelemetryFunctions(apiRequestContext);
    }

    @AfterAll
    public static void afterAll() {
        apiRequestContext.dispose();
        playwright.close();
    }

    @Test
    public void healthcheckTest() {
        APIResponse response = telemetryFunctions.healthcheck();

        assertTrue(response.ok(), "Telemetry service healthcheck failed: " + response.status() + " - " + response.text());
    }


    @Test
    public void unauthenticatedGetEventsTest() {
        APIRequestContext unauthenticatedRequestContext = playwright.request().newContext();
        TelemetryFunctions unauthenticatedTelemetryFunctions = new TelemetryFunctions(unauthenticatedRequestContext);
        APIResponse response = unauthenticatedTelemetryFunctions.getEventsWithoutAccess();

        assertEquals(401, response.status());
        unauthenticatedRequestContext.dispose();
    }

    @Test
    public void invalidAccessTokenCannotGetEventsTest() {
        APIResponse response = telemetryFunctions.getEvents("invalid-token");

        assertEquals(401, response.status());
    }

    @Test
    public void serviceAccountCannotGetEventsTest() {
        AuthFunctions authFunctions = new AuthFunctions(apiRequestContext);
        String accessToken = authFunctions.extractJwt(authFunctions.serviceAccountAuthenticate("test"));

        APIResponse response = telemetryFunctions.getEvents(accessToken);

        assertEquals(403, response.status());
    }

    @Test
    public void superUserCanGetEventsTest() {
        AuthFunctions authFunctions = new AuthFunctions(apiRequestContext);
        String accessToken = authFunctions.authenticateInitialSuperUser();

        APIResponse response = telemetryFunctions.getEvents(accessToken);

        assertTrue(response.ok(), "SUPER user should access TelemetryService: " +
                response.status() + " - " + response.text());
    }

    @Test
    public void serviceAccountCannotSpoofTelemetryEventTest() {
        AuthFunctions authFunctions = new AuthFunctions(apiRequestContext);
        String serviceAccountToken = authFunctions.extractJwt(authFunctions.serviceAccountAuthenticate("test"));
        String eventType = "test-" + UUID.randomUUID();
        String suppliedId = UUID.randomUUID().toString();
        String suppliedTimestamp = LocalDateTime.now().minusYears(1).toString();

        APIResponse response = telemetryFunctions.createEvent(
                "spoofed-service",
                eventType,
                suppliedId,
                suppliedTimestamp,
                serviceAccountToken
        );

        assertTrue(response.ok(), "Service account event creation failed: " +
                response.status() + " - " + response.text());

        String superUserToken = authFunctions.authenticateInitialSuperUser();
        response = telemetryFunctions.getEvents(eventType, superUserToken);

        assertTrue(response.ok(), "SUPER user could not retrieve telemetry event: " +
                response.status() + " - " + response.text());
        assertTrue(response.text().contains("\"serviceName\":\"test\""));
        assertFalse(response.text().contains("spoofed-service"));
        assertFalse(response.text().contains(suppliedId));
        assertFalse(response.text().contains(suppliedTimestamp));
    }

    @Test
    public void normalUserCannotGetEventsTest() {
        AuthFunctions authFunctions = new AuthFunctions(apiRequestContext);
        String username = "test_" + UUID.randomUUID();
        String email = username + "@email.com";
        String password = UUID.randomUUID().toString();
        String accessToken = authFunctions.registerEnableAuthenticateAccess(username, email, password);

        APIResponse response = telemetryFunctions.getEvents(accessToken);

        assertEquals(403, response.status());

        response = authFunctions.deleteUser(username, email, password);
        assertTrue(response.ok(), "User cleanup failed: " + response.status() + " - " + response.text());
    }

}
