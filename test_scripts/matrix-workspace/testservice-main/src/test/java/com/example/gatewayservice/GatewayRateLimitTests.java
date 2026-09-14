

package com.example.gatewayservice;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.RequestOptions;
import com.example.utility.EnvUtility;
import com.example.utility.ExtentTestWatcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(ExtentTestWatcher.class)
@Tag("rate-limit")
@EnabledIfEnvironmentVariable(named = "AUTHSERVICE_INTEGRATION", matches = "true")
@EnabledIfEnvironmentVariable(named = "GATEWAYSERVICE_INTEGRATION", matches = "true")
public class GatewayRateLimitTests {
    private static final String GATEWAY_BASE_URL = EnvUtility.getEnvVar("GATEWAY_BASE_URL");
    private static Playwright playwright;
    private static APIRequestContext apiRequestContext;

    @BeforeAll
    public static void beforeAll() {
        playwright = Playwright.create();
        apiRequestContext = playwright.request().newContext();
    }

    @AfterAll
    public static void afterAll() {
        apiRequestContext.dispose();
        playwright.close();
    }

    @Test
    public void authTrafficRateLimitTest() {
        assertRateLimit("/api/auth/access", 80);
    }

    private void assertRateLimit(String endpoint, int attempts) {
        APIResponse response = null;

        for(int i = 0; i < attempts; i++) {
            response = apiRequestContext.post(GATEWAY_BASE_URL + endpoint,
                    RequestOptions.create().setData(new HashMap<>()));
        }

        assertEquals(429, response.status(), endpoint + " should return 429 after reaching the rate limit");
    }
}
