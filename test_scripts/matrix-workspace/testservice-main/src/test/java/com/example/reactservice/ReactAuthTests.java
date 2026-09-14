


package com.example.reactservice;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "AUTHSERVICE_INTEGRATION", matches = "true")
@EnabledIfEnvironmentVariable(named = "REACTSERVICE_INTEGRATION", matches = "true")
@EnabledIfEnvironmentVariable(named = "EMAILSERVICE_INTEGRATION", matches = "true")
public class ReactAuthTests extends ReactTestsBase {
    @Test
    public void ensureLoginDivLoadingTest() {
        reactFunctions.navigateToLogin(page);

        Locator loginDiv = page.locator("id=login-div");

        loginDiv.waitFor(WAIT_FOR_VISIBLE);
        assertTrue(loginDiv.isVisible(), "Login div should be visible after navigating to login page");
    }

    @Test
    public void ensureRegisterDivLoadingTest() {
        reactFunctions.navigateToRegister(page, email);

        Locator registerDiv = page.locator("id=register-div");

        registerDiv.waitFor(WAIT_FOR_VISIBLE);
        assertTrue(registerDiv.isVisible(), "Register div should be visible after navigating to register page");
    }

    @Test
    public void userRegisterTest() {
        reactFunctions.navigateToRegister(page, email);
        reactFunctions.fillAndSubmitRegisterForm(page, username, email, password);

        Locator requestSuccessTextField = page.locator("id=request-success-text-field");

        requestSuccessTextField.waitFor(WAIT_FOR_VISIBLE);
        assertTrue(requestSuccessTextField.isVisible(), "Request success div should be visible after navigating to register page");
    }

    @Test
    public void userLoginTest() {
        reactFunctions.registerEnableAndLogin(page, username, email, password);

        Locator usernameButton = page.getByText(username);

        usernameButton.waitFor(WAIT_FOR_VISIBLE);
        assertTrue(usernameButton.isVisible(), "The logged in user's button should be visible after logging in");
    }

    @Test
    public void userSessionPersistsAfterReloadTest() {
        reactFunctions.registerEnableAndLogin(page, username, email, password);

        page.reload();

        Locator usernameButton = page.getByText(username);
        usernameButton.waitFor(WAIT_FOR_VISIBLE);
        assertTrue(usernameButton.isVisible(), "The user should remain logged in after reloading");
    }

    @Test
    public void userLogoutPreventsSessionRefreshTest() {
        reactFunctions.registerEnableAndLogin(page, username, email, password);

        page.getByText(username).click();
        page.getByText("Logout").click();

        Locator loginButton = page.getByTestId("loginButton");
        loginButton.waitFor(WAIT_FOR_VISIBLE);

        page.reload();
        loginButton.waitFor(WAIT_FOR_VISIBLE);
        assertTrue(loginButton.isVisible(), "The user should remain logged out after reloading");
    }

    @Test
    public void ensureUserSettingsRedirectsToLoginTest() {
        reactFunctions.navigateToUserSettings(page);

        Locator loginDiv = page.locator("id=login-div");

        loginDiv.waitFor(WAIT_FOR_VISIBLE);
        assertTrue(loginDiv.isVisible(), "Login div should be visible after navigating to login page");
    }

    @Test
    public void userEnsureUserSettingsLoadingTest() {
        reactFunctions.registerEnableAndLogin(page, username, email, password);

        reactFunctions.navigateToUserSettings(page);

        Locator accountSettingsHeader = page.getByText("Account Information");

        accountSettingsHeader.waitFor(WAIT_FOR_VISIBLE);
        assertTrue(accountSettingsHeader.isVisible(), "Account settings header should be visible after navigating to settings page");
    }

    @Test
    public void userUpdateUsernameTest() {
        reactFunctions.registerEnableAndLogin(page, username, email, password);

        reactFunctions.navigateToUserSettings(page);

        username = "test_" + UUID.randomUUID();
        reactFunctions.clickEditAndSubmitUpdateUsername(page, username, password);

        Locator usernameTextElement = page.getByText("Username: " + username);

        usernameTextElement.waitFor(WAIT_FOR_VISIBLE);
        assertEquals("Username: " + username, usernameTextElement.innerText(), "New username should be visible after updating");
    }

    @Test
    public void userUpdateEmailTest() {
        reactFunctions.registerEnableAndLogin(page, username, email, password);

        reactFunctions.navigateToUserSettings(page);

        email = "testEmail_" + UUID.randomUUID() + "@email.com";
        reactFunctions.clickEditAndSubmitUpdateEmail(page, email, password);

        Locator emailTextElement = page.getByText(email);

        emailTextElement.waitFor(WAIT_FOR_VISIBLE);
        assertTrue(emailTextElement.isVisible(), "Email should be visible after updating");
        assertEquals("Email: " + email, emailTextElement.innerText(), "New email should match the email displayed on the page");
    }

    @Test
    public void userUpdatePasswordTest() {
        reactFunctions.registerEnableAndLogin(page, username, email, password);

        reactFunctions.navigateToUserSettings(page);

        String newPassword = "newPassword123!";
        reactFunctions.clickEditAndSubmitUpdatePassword(page, password, newPassword);
        password = newPassword;

        Locator accountSettingsHeader = page.getByText("Account Information");

        accountSettingsHeader.waitFor(WAIT_FOR_VISIBLE);
        assertTrue(accountSettingsHeader.isVisible(), "User should remain logged in after updating their password");
    }

    @Test
    public void deleteUserTest() {
        username = "test_" + UUID.randomUUID();
        email = username + "@testEmail.com";
        password = "testPassword123!";

        reactFunctions.registerEnableAndLogin(page, username, email, password);

        reactFunctions.navigateToUserSettings(page);

        reactFunctions.clickEditAndSubmitDeleteAccount(page, username, password);

        Locator loginDiv = page.locator("id=login-div");

        loginDiv.waitFor(WAIT_FOR_VISIBLE);
        assertTrue(loginDiv.isVisible(), "Login div should be visible after deleting user");
    }

    @Test
    public void superUserAdminPageTest() {
        reactFunctions.navigateToLogin(page);
        reactFunctions.fillAndSubmitLoginForm(page, authUtility.getInitialSuperUsername(), authUtility.getInitialSuperPassword());

        page.navigate(ReactFunctions.REACT_BASE_URL + "/admin");

        Locator administrationHeader = page.getByText("Administration");
        administrationHeader.waitFor(WAIT_FOR_VISIBLE);
        assertTrue(administrationHeader.isVisible(), "SUPER user should be able to view the administration page");
    }

    @Test
    public void ensurePasswordResetDivLoadingTest() {
        reactFunctions.navigateToPasswordReset(page);

        Locator resetPasswordDiv = page.locator("id=request-div");

        resetPasswordDiv.waitFor(WAIT_FOR_VISIBLE);
        assertTrue(resetPasswordDiv.isVisible(), "Reset password div should be visible after navigating to password-reset page");
    }

    @Test
    public void ensureNewPasswordDivLoadingTest() {
        reactFunctions.navigateToNewPassword(page);

        Locator newPasswordDiv = page.locator("id=request-div");

        newPasswordDiv.waitFor(WAIT_FOR_VISIBLE);
        assertTrue(newPasswordDiv.isVisible(), "New password div should be visible after navigating to new-password page");
    }

    @Test
    public void userRegisterEmailTest() {
        reactFunctions.registerAndEnable(page, username, email, password);

        Locator messageDiv = page.locator("id=message-div");

        messageDiv.waitFor(WAIT_FOR_VISIBLE);
        assertTrue(messageDiv.isVisible(), "Success message div should be visible after clicking the register email link");

        reactFunctions.navigateToLogin(page);
        reactFunctions.fillAndSubmitLoginForm(page, username, password);

        Locator usernameButton = page.getByText(username);

        usernameButton.waitFor(WAIT_FOR_VISIBLE);
        assertTrue(usernameButton.isVisible(), "User should be able to log in after confirming registration");
    }

    @Test
    public void userResetPasswordTest() {
        reactFunctions.registerAndEnable(page, username, email, password);

        reactFunctions.navigateToPasswordReset(page);

        reactFunctions.fillAndSubmitPasswordResetForm(page, username);

        reactFunctions.navigateToMailbox(page);

        reactFunctions.clickPasswordResetEmail(page, email);

        password = "newPassword123!";
        reactFunctions.fillAndSubmitNewPasswordForm(page, password);

        Locator successDiv = page.locator("id=request-success-text-field");

        successDiv.waitFor(WAIT_FOR_VISIBLE);
        assertTrue(successDiv.isVisible(), "Success message div should be visible after updating the user's password");
        assertEquals("Your password has been successfully reset", successDiv.innerText(), "The success message div text is incorrect");

        reactFunctions.navigateToLogin(page);
        reactFunctions.fillAndSubmitLoginForm(page, username, password);

        Locator usernameButton = page.getByText(username);

        usernameButton.waitFor(WAIT_FOR_VISIBLE);
        assertTrue(usernameButton.isVisible(), "User should be able to log in with the reset password");
    }

}
