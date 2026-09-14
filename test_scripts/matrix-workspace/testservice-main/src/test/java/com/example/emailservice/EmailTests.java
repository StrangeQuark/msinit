

package com.example.emailservice;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.example.authservice.AuthFunctions;
import com.example.utility.ExtentTestWatcher;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ExtentTestWatcher.class)
@EnabledIfEnvironmentVariable(named = "EMAILSERVICE_INTEGRATION", matches = "true")
@EnabledIfEnvironmentVariable(named = "AUTHSERVICE_INTEGRATION", matches = "true")
public class EmailTests {
    private static Playwright playwright;
    private static APIRequestContext apiRequestContext;
    private static EmailFunctions emailFunctions;

    @BeforeAll
    public static void beforeAll() {
        playwright = Playwright.create();
        apiRequestContext = playwright.request().newContext();
        emailFunctions = new EmailFunctions(apiRequestContext);
    }

    @AfterAll
    public static void afterAll() {
        apiRequestContext.dispose();
        playwright.close();
    }

    @Test
    public void healthcheckTest() {
        APIResponse response = emailFunctions.healthcheck();
        assertTrue(response.ok(), "Email service healthcheck failed: " + response.status() + " - " + response.text());
    }


    @Test
    public void unauthenticatedSendEmailTest() {
        APIRequestContext unauthenticatedRequestContext = playwright.request().newContext();
        EmailFunctions unauthenticatedEmailFunctions = new EmailFunctions(unauthenticatedRequestContext);
        APIResponse response = unauthenticatedEmailFunctions.sendEmailWithoutAccess();

        assertEquals(401, response.status());
        unauthenticatedRequestContext.dispose();
    }

    @Test
    public void invalidAccessTokenCannotSendEmailTest() {
        APIResponse response = emailFunctions.sendEmail("recipient@email.com", "sender@email.com",
                "Test email", "Test subject", "invalid-token");

        assertEquals(401, response.status());
    }


    @Test
    public void sendEmailTest() {
        APIResponse response = emailFunctions.sendEmail("recipient@email.com", "sender@email.com",
                "Test email", "Test subject");

        assertTrue(response.ok(), "Send email test failed: " + response.status() + " - " + response.text());
    }

    @Test
    public void superUserCanSendEmailTest() {
        AuthFunctions authFunctions = new AuthFunctions(apiRequestContext);
        String accessToken = authFunctions.authenticateInitialSuperUser();

        APIResponse response = emailFunctions.sendEmail("recipient@email.com", "sender@email.com",
                "Test email", "Test subject", accessToken);

        assertTrue(response.ok(), "SUPER user email request failed: " + response.status() + " - " + response.text());
    }

    @Test
    public void getAllTemplateEmailsTest() {
        APIResponse response = emailFunctions.getAllTemplateEmails();

        assertTrue(response.ok(), "Template list request failed: " + response.status() + " - " + response.text());
        assertTrue(response.text().contains("USER_REGISTER"));
    }

    @Test
    public void normalUserCannotSendEmailTest() {
        AuthFunctions authFunctions = new AuthFunctions(apiRequestContext);
        String username = "test_" + UUID.randomUUID();
        String email = username + "@email.com";
        String password = UUID.randomUUID().toString();
        String accessToken = authFunctions.registerEnableAuthenticateAccess(username, email, password);

        APIResponse response = emailFunctions.sendEmail("recipient@email.com", "sender@email.com",
                "Test email", "Test subject", accessToken);

        assertEquals(403, response.status());

        response = authFunctions.deleteUser(username, email, password);
        assertTrue(response.ok(), "User cleanup failed: " + response.status() + " - " + response.text());
    }

    @Test
    public void getTemplateEmailTest() {
        APIResponse response = emailFunctions.getTemplateEmail("USER_REGISTER");

        assertTrue(response.ok(), "Get template email test failed: " + response.status() + " - " + response.text());
    }

    @Test
    public void sendTemplateEmailTest() {
        APIResponse response = emailFunctions.sendTemplateEmail("recipient@email.com", "sender@email.com",
                true, "USER_REGISTER", Map.of("link", "http://testservice.com"));

        assertTrue(response.ok(), "Send template email test failed: " + response.status() + " - " + response.text());
        JsonObject jsonObject = JsonParser.parseString(response.text()).getAsJsonObject();
        assertFalse(jsonObject.has("token"), "System template response must not include a token");
    }

    @Test
    public void createTemplateEmailTest() {
        String templateName = "TEST_SERVICE_TEMPLATE_" + UUID.randomUUID();
        APIResponse response = emailFunctions.createTemplateEmail("Test template body", "Test template subject",
                templateName, "INVITATION");

        assertTrue(response.ok(), "Create template email test failed: " + response.status() + " - " + response.text());

        response = emailFunctions.deleteTemplateEmail(templateName);
        assertTrue(response.ok(), "Template cleanup failed: " + response.status() + " - " + response.text());
    }

    @Test
    public void sendEmailWithTokenTest() {
        APIResponse response = emailFunctions.sendEmailWithToken("recipient@email.com", "sender@email.com",
                "Test email", "Test subject");
        assertFalse(response.ok(), "Generic email token request should be rejected");
    }

    @Test
    public void sendCustomTemplateEmailWithTokenTest() {
        String templateName = "TEST_SERVICE_TEMPLATE_" + UUID.randomUUID();

        APIResponse response = emailFunctions.createTemplateEmail("Test template body [[confirmationToken]]",
                "Test template subject", templateName, "INVITATION");
        assertTrue(response.ok(), "Custom template creation failed: " + response.status() + " - " + response.text());

        response = emailFunctions.sendTemplateEmail("recipient@email.com", "sender@email.com",
                true, templateName, Map.of());
        assertTrue(response.ok(), "Custom token template send failed: " + response.status() + " - " + response.text());

        JsonObject jsonObject = JsonParser.parseString(response.text()).getAsJsonObject();
        assertTrue(jsonObject.has("token"), "Custom template response should include its token");

        response = emailFunctions.deleteTemplateEmail(templateName);
        assertTrue(response.ok(), "Custom template cleanup failed: " + response.status() + " - " + response.text());
    }

    @Test
    public void updateTemplateEmailTest() {
        String templateName = "TEST_SERVICE_TEMPLATE_" + UUID.randomUUID();

        APIResponse response = emailFunctions.createTemplateEmail("Original body", "Original subject",
                templateName, "INVITATION");
        assertTrue(response.ok(), "Template creation failed: " + response.status() + " - " + response.text());

        response = emailFunctions.updateTemplateEmail("Updated body", "Updated subject", templateName);
        assertTrue(response.ok(), "Template update failed: " + response.status() + " - " + response.text());

        response = emailFunctions.deleteTemplateEmail(templateName);
        assertTrue(response.ok(), "Template cleanup failed: " + response.status() + " - " + response.text());
    }

    @Test
    public void deleteSystemTemplateEmailTest() {
        APIResponse response = emailFunctions.deleteTemplateEmail("USER_REGISTER");

        assertFalse(response.ok(), "System templates must not be deleted");
    }

    @Test
    public void createMultiplePasswordResetTemplatesTest() {
        String firstTemplateName = "PASSWORD_RESET_A_" + UUID.randomUUID();
        String secondTemplateName = "PASSWORD_RESET_B_" + UUID.randomUUID();

        APIResponse response = emailFunctions.createTemplateEmail("Site A reset [[confirmationToken]]",
                "Reset Site A password", firstTemplateName, "PASSWORD_RESET");
        assertTrue(response.ok(), "First reset template creation failed: " + response.status() + " - " + response.text());

        response = emailFunctions.createTemplateEmail("Site B reset [[confirmationToken]]",
                "Reset Site B password", secondTemplateName, "PASSWORD_RESET");
        assertTrue(response.ok(), "Second reset template creation failed: " + response.status() + " - " + response.text());

        emailFunctions.deleteTemplateEmail(firstTemplateName);
        emailFunctions.deleteTemplateEmail(secondTemplateName);
    }
}
