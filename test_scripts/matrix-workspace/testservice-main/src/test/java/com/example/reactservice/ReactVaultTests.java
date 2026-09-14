


package com.example.reactservice;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "REACTSERVICE_INTEGRATION", matches = "true")
@EnabledIfEnvironmentVariable(named = "VAULTSERVICE_INTEGRATION", matches = "true")
@EnabledIfEnvironmentVariable(named = "AUTHSERVICE_INTEGRATION", matches = "true")
@EnabledIfEnvironmentVariable(named = "EMAILSERVICE_INTEGRATION", matches = "true")
public class ReactVaultTests extends ReactTestsBase {
    private void registerEnableAndLoginDeveloper() {
        reactFunctions.registerAndEnable(page, username, email, password);

        assertTrue(authFunctions.updateRole("DEVELOPER", username, authFunctions.authenticateInitialSuperUser()).ok());

        reactFunctions.navigateToLogin(page);
        reactFunctions.fillAndSubmitLoginForm(page, username, password);
    }


    @Test
    public void ensureToolbarVaultButtonRedirectsToLoginTest() {
        reactFunctions.navigateToLogin(page);

        reactFunctions.clickToolbarVaultButton(page);

        Locator loginDiv = page.locator("id=login-div");

        loginDiv.waitFor(WAIT_FOR_VISIBLE);
        assertTrue(loginDiv.isVisible(), "Login div should be visible after clicking the Vault button on the " +
                "toolbar without logging in");
    }

    @Test
    public void userCreateServiceTest() {
        registerEnableAndLoginDeveloper();
        reactFunctions.navigateToVault(page);

        reactFunctions.fillAndSubmitCreateServiceForm(page, serviceName);

        page.getByText(serviceName).waitFor(WAIT_FOR_ATTACHED);

        Locator serviceSelectOptions = page.locator("#service-select option");
        assertTrue(serviceSelectOptions.allInnerTexts().contains(serviceName),
                "Service name should be present in the dropdown after creation");
    }

    @Test
    public void userCreateEnvironmentTest() {
        registerEnableAndLoginDeveloper();
        reactFunctions.navigateToVault(page);

        reactFunctions.fillAndSubmitCreateServiceForm(page, serviceName);
        reactFunctions.selectService(page, serviceName);

        reactFunctions.fillAndSubmitCreateEnvironmentForm(page, environmentName);

        page.getByText(environmentName).waitFor(WAIT_FOR_ATTACHED);

        Locator environmentSelectOptions = page.locator("#environment-select option");
        assertTrue(environmentSelectOptions.allInnerTexts().contains(environmentName),
                "Environment name should be present in the dropdown after creation");
    }

    @Test
    public void userAddVariableTest() {
        registerEnableAndLoginDeveloper();
        reactFunctions.navigateToVault(page);

        reactFunctions.createServiceEnvironmentAndVariable(page, serviceName, environmentName, testVariableKey, testVariableValue);

        Locator variableKeyInput = page.locator("id=key-" + testVariableKey);
        assertTrue(variableKeyInput.isVisible(), "Variable key input should be visible after variable creation");
    }

    @Test
    public void userUnmaskVariableTest() {
        registerEnableAndLoginDeveloper();
        reactFunctions.navigateToVault(page);

        reactFunctions.createServiceEnvironmentAndVariable(page, serviceName, environmentName, testVariableKey, testVariableValue);
        reactFunctions.unmaskVariable(page, testVariableKey);

        Locator variableValueInput = page.locator("id=value-" + testVariableKey);

        variableValueInput.waitFor(WAIT_FOR_VISIBLE);
        assertFalse(variableValueInput.getAttribute("class").contains("masked-input"), "Variable value should be visible after unmasking");
    }

    @Test
    public void userCopyVariableTest() {
        registerEnableAndLoginDeveloper();
        reactFunctions.navigateToVault(page);

        reactFunctions.createServiceEnvironmentAndVariable(page, serviceName, environmentName, testVariableKey, testVariableValue);

        Locator variableCopyButton = page.locator("id=copy-" + testVariableKey);

        // Spy on clipboard to avoid permission issues
        page.evaluate("""
            () => { 
                window.__copiedText = '';
                if (!navigator.clipboard) {
                    navigator.clipboard = {};
                }
                navigator.clipboard.writeText = text => { 
                    window.__copiedText = text;
                    return Promise.resolve();
                }; 
            }
        """);

        variableCopyButton.click();

        assertEquals(testVariableValue, page.evaluate("() => window.__copiedText"),
                "Clipboard text should match the test variable's value after copying");
    }

    @Test
    public void userDeleteVariableTest() {
        registerEnableAndLoginDeveloper();
        reactFunctions.navigateToVault(page);

        reactFunctions.createServiceEnvironmentAndVariable(page, serviceName, environmentName, testVariableKey, testVariableValue);

        reactFunctions.handleNextAlert(page, true);
        reactFunctions.deleteVariable(page, testVariableKey);

        Locator variableKeyInput = page.locator("id=key-" + testVariableKey);

        variableKeyInput.waitFor(WAIT_FOR_DETACHED);
        assertFalse(variableKeyInput.isVisible(), "Variable key input should not be visible after variable deletion");
    }

    @Test
    public void userUploadEnvFileTest() {
        registerEnableAndLoginDeveloper();
        reactFunctions.navigateToVault(page);

        reactFunctions.createServiceEnvironmentAndVariable(page, serviceName, environmentName, testVariableKey, testVariableValue);

        reactFunctions.uploadEnvFile(page, envFilePath);

        Locator variableKeyInput = page.locator("id=key-testEnvKey");

        variableKeyInput.waitFor(WAIT_FOR_VISIBLE);
        assertTrue(variableKeyInput.isVisible(), "Variable key input should be visible after uploading env file");
    }

    @Test
    public void userDownloadEnvFileTest() {
        registerEnableAndLoginDeveloper();
        reactFunctions.navigateToVault(page);

        reactFunctions.createServiceEnvironmentAndVariable(page, serviceName, environmentName, testVariableKey, testVariableValue);

        Download download = reactFunctions.clickEnvDownloadButton(page);

        assertEquals(serviceName + "." + environmentName + ".env", download.suggestedFilename(), "Download file name should match textFileName");

        String fileContent = reactFunctions.getEnvFileContent(download.path());

        String expectedContent = testVariableKey + "=\"" + testVariableValue + "\"\n";
        assertEquals(expectedContent, fileContent, "Downloaded env file contents should match expected");
    }

    @Test
    public void userDeleteEnvironmentTest() {
        registerEnableAndLoginDeveloper();
        reactFunctions.navigateToVault(page);

        reactFunctions.fillAndSubmitCreateServiceForm(page, serviceName);
        reactFunctions.selectService(page, serviceName);

        reactFunctions.fillAndSubmitCreateEnvironmentForm(page, environmentName);
        reactFunctions.selectEnvironment(page, environmentName);

        reactFunctions.clickServiceManagementIcon(page);

        reactFunctions.handleNextAlert(page, true);
        reactFunctions.clickDeleteEnvironmentButton(page);

        page.getByText(environmentName).waitFor(WAIT_FOR_DETACHED);

        Locator environmentSelectOptions = page.locator("#environment-select option");
        assertFalse(environmentSelectOptions.allInnerTexts().contains(environmentName),
                "Environment name should be present in the dropdown after deletion");
    }

    @Test
    public void userDeleteServiceTest() {
        registerEnableAndLoginDeveloper();
        reactFunctions.navigateToVault(page);

        reactFunctions.fillAndSubmitCreateServiceForm(page, serviceName);
        reactFunctions.selectService(page, serviceName);

        reactFunctions.clickServiceManagementIcon(page);

        reactFunctions.handleNextAlert(page, true);
        reactFunctions.clickDeleteServiceButton(page);

        page.getByText(serviceName).waitFor(WAIT_FOR_DETACHED);

        Locator serviceSelectOptions = page.locator("#service-select option");
        assertFalse(serviceSelectOptions.allInnerTexts().contains(serviceName),
                "Service name should be present in the dropdown after deletion");
    }

    @Test
    public void userAddUserToCollectionTest() {
        String testUsername = "test_" + UUID.randomUUID();
        String testEmail = UUID.randomUUID() + "@testEmail.com";
        String testPassword = "testPassword123!";

        reactFunctions.registerAndEnable(page, testUsername, testEmail, testPassword);
        registerEnableAndLoginDeveloper();
        reactFunctions.navigateToVault(page);

        reactFunctions.fillAndSubmitCreateServiceForm(page, serviceName);
        reactFunctions.selectService(page, serviceName);

        reactFunctions.clickServiceManagementIcon(page);

        reactFunctions.clickManageUsersButton(page);
        reactFunctions.searchForAndSelectUserInUserManagementPopup(page, testUsername);

        Locator addedUsername = page.getByText(testUsername);
        Locator addedEmail = page.getByText(testEmail);

        addedUsername.waitFor(WAIT_FOR_VISIBLE);
        assertTrue(addedUsername.isVisible(), "Added username should be present in the user management popup");

        addedEmail.waitFor(WAIT_FOR_VISIBLE);
        assertTrue(addedEmail.isVisible(), "Added email should be present in the user management popup");

        authFunctions.deleteUser(testUsername, testEmail, testPassword);
    }

    @Test
    public void userAddUserToCollectionAndChangeRoleTest() {
        String testUsername = "test_" + UUID.randomUUID();
        String testEmail = UUID.randomUUID() + "@testEmail.com";
        String testPassword = "testPassword123!";

        reactFunctions.registerAndEnable(page, testUsername, testEmail, testPassword);
        registerEnableAndLoginDeveloper();
        reactFunctions.navigateToVault(page);

        reactFunctions.fillAndSubmitCreateServiceForm(page, serviceName);
        reactFunctions.selectService(page, serviceName);

        reactFunctions.clickServiceManagementIcon(page);

        reactFunctions.clickManageUsersButton(page);
        reactFunctions.searchForAndSelectUserInUserManagementPopup(page, testUsername);

        reactFunctions.changeUserRoleInUserManagementPopup(page, testUsername, "MANAGER");

        Locator newRole = page.locator(".user-row", new Page.LocatorOptions().setHasText(testUsername))
                .getByText("MANAGER");

        newRole.waitFor(WAIT_FOR_VISIBLE);
        assertTrue(newRole.isVisible(), "New role should be present in the user management popup");

        authFunctions.deleteUser(testUsername, testEmail, testPassword);
    }

    @Test
    public void userAddUserToCollectionThenDeleteFromCollectionTest() {
        String testUsername = "test_" + UUID.randomUUID();
        String testEmail = UUID.randomUUID() + "@testEmail.com";
        String testPassword = "testPassword123!";

        reactFunctions.registerAndEnable(page, testUsername, testEmail, testPassword);
        registerEnableAndLoginDeveloper();
        reactFunctions.navigateToVault(page);

        reactFunctions.fillAndSubmitCreateServiceForm(page, serviceName);
        reactFunctions.selectService(page, serviceName);

        reactFunctions.clickServiceManagementIcon(page);

        reactFunctions.clickManageUsersButton(page);
        reactFunctions.searchForAndSelectUserInUserManagementPopup(page, testUsername);

        reactFunctions.handleNextAlert(page, true);
        reactFunctions.deleteUserInUserManagementPopup(page, testUsername);

        Locator testUser = page.getByText(testUsername);

        testUser.waitFor(WAIT_FOR_DETACHED);
        assertFalse(testUser.isVisible(), "Test user should not be present in the user management popup after deletion");

        authFunctions.deleteUser(testUsername, testEmail, testPassword);
    }
}
