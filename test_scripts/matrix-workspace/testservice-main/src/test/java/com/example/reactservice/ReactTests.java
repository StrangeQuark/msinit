

package com.example.reactservice;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "REACTSERVICE_INTEGRATION", matches = "true")
public class ReactTests extends ReactTestsBase {
    @Test
    public void ensureHomePageLoadingTest() {
        reactFunctions.navigateToHomePage(page);

        Locator homeButton = page.getByTestId("home-nav-link");

        homeButton.waitFor(WAIT_FOR_VISIBLE);
        assertTrue(homeButton.isVisible(), "Home button should be visible in the toolbar after navigating to home page");
    }

    @Test
    public void securityHeadersTest() {
        APIResponse response = apiRequestContext.get(ReactFunctions.REACT_BASE_URL);

        assertEquals(200, response.status());
        assertTrue(response.headers().get("content-security-policy").contains("default-src 'self'"));
        assertEquals("same-origin", response.headers().get("cross-origin-opener-policy"));
        assertEquals("nosniff", response.headers().get("x-content-type-options"));
        assertEquals("DENY", response.headers().get("x-frame-options"));
        assertEquals("strict-origin-when-cross-origin", response.headers().get("referrer-policy"));
        assertEquals("camera=(), geolocation=(), microphone=(), payment=(), usb=()", response.headers().get("permissions-policy"));
    }
}
