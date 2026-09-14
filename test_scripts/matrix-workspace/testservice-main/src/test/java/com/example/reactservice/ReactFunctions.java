

package com.example.reactservice;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.example.authservice.AuthFunctions;
import com.example.utility.EnvUtility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ReactFunctions {
    public static final String REACT_BASE_URL = EnvUtility.getEnvVar("REACT_BASE_URL");
    public static final String MAILDEV_BASE_URL = EnvUtility.getEnvVar("MAILDEV_BASE_URL");
    private final AuthFunctions authFunctions;

    public ReactFunctions(AuthFunctions authFunctions) {
        this.authFunctions = authFunctions;

    }

    public void navigateToHomePage(Page page) {
        page.navigate(REACT_BASE_URL + "/");
    }

    public void handleNextAlert(Page page, boolean accept) {
        page.onceDialog(dialog -> {
            if(accept)
                dialog.accept();
            else
                dialog.dismiss();
        });
    }

    public void navigateToLogin(Page page) {
        page.navigate(REACT_BASE_URL + "/login");
    }

    public void navigateToRegister(Page page) {
        page.navigate(REACT_BASE_URL + "/register");
    }

    public void navigateToRegister(Page page, String email) {
        String accessToken = authFunctions.authenticateInitialSuperUser();
        String inviteToken = authFunctions.createInvitationToken(email, accessToken);
        page.navigate(REACT_BASE_URL + "/register#inviteToken=" + inviteToken);
    }

    public void fillAndSubmitRegisterForm(Page page, String username, String email, String password) {
        page.locator("id=username").fill(username);
        page.locator("id=email").fill(email);
        page.locator("id=password").fill(password);
        page.locator("id=confirm-password").fill(password);

        page.click("id=submit-button");

        // We must wait for the success div to ensure request sent
        page.locator("id=request-success-text-field")
                .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
    }

    public void fillAndSubmitLoginForm(Page page, String username, String password) {
        page.locator("id=username").fill(username);
        page.locator("id=password").fill(password);

        page.click("id=submit-button");

        page.waitForURL(REACT_BASE_URL + "/");
    }

    public void fillAndSubmitPasswordResetForm(Page page, String username) {
        page.locator("id=credentials").fill(username);

        page.click("id=submit-button");

        // We must wait for the success div to ensure request sent
        page.locator("id=request-success-div")
                .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
    }

    public void fillAndSubmitNewPasswordForm(Page page, String newPassword) {
        page.locator("id=password").fill(newPassword);
        page.locator("id=confirm-password").fill(newPassword);

        page.click("id=submit-button");

        // We must wait for the success div to ensure request sent
        page.locator("id=request-success-div")
                .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
    }

    public void registerAndEnable(Page page, String username, String email, String password) {
        navigateToRegister(page, email);
        fillAndSubmitRegisterForm(page, username, email, password);

        navigateToMailbox(page);
        page.getByPlaceholder("Search emails...").fill(email);

        Locator registrationEmail = page.getByTestId("email-list-item")
                .filter(new Locator.FilterOptions().setHasText(email))
                .filter(new Locator.FilterOptions().setHasText("Activate your account"));
        registrationEmail.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        registrationEmail.click();

        FrameLocator emailFrame = page.frameLocator("iframe").first();

        Locator confirmLink = emailFrame.getByText("Confirm registration");
        confirmLink.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

        String confirmUrl = confirmLink.getAttribute("href");
        assertNotNull(confirmUrl, "Confirmation link should have an href attribute");

        page.navigate(confirmUrl);
        page.locator("id=message-div").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

    }

    public void registerEnableAndLogin(Page page, String username, String email, String password) {
        registerAndEnable(page, username, email, password);

        navigateToLogin(page);

        fillAndSubmitLoginForm(page, username, password);
    }

    public void navigateToUserSettings(Page page) {
        page.navigate(REACT_BASE_URL + "/settings");
    }

    public void clickEditAndSubmitUpdateUsername(Page page, String username, String password) {
        page.getByTestId("update-username").click();

        page.locator("id=input-newUsername-0").fill(username);
        page.locator("id=input-password-1").fill(password);

        page.getByText("Save").click();
    }

    public void clickEditAndSubmitUpdateEmail(Page page, String email, String password) {
        page.getByTestId("update-email").click();

        page.locator("id=input-newEmail-0").fill(email);
        page.locator("id=input-password-1").fill(password);

        page.getByText("Save").click();
    }

    public void clickEditAndSubmitUpdatePassword(Page page, String password, String newPassword) {
        page.getByTestId("update-password").click();

        page.locator("id=input-password-0").fill(password);
        page.locator("id=input-newPassword-1").fill(newPassword);

        page.getByText("Save").click();
    }

    public void clickEditAndSubmitDeleteAccount(Page page, String username, String password) {
        page.getByTestId("delete-account-button").click();

        page.locator("id=input-username-0").fill(username);
        page.locator("id=input-password-1").fill(password);

        page.getByText("Save").click();
    }

    public void clickManageUsersButton(Page page) {
        page.getByText("Manage Users").click();
    }

    public void searchForAndSelectUserInUserManagementPopup(Page page, String username) {
        page.locator("id=search-users").fill(username);

        page.getByText("Search").click();

        page.locator("id=search-result").click();
    }

    public void changeUserRoleInUserManagementPopup(Page page, String username, String role) {
        Locator userRow = page.locator(".user-row", new Page.LocatorOptions().setHasText(username));
        userRow.getByTestId("edit-user-role-button").click();

        page.getByTestId("role-select").selectOption(role);

        page.getByText("Save").click();
    }

    public void deleteUserInUserManagementPopup(Page page, String username) {
        Locator userRow = page.locator(".user-row", new Page.LocatorOptions().setHasText(username));
        userRow.getByTestId("delete-user-button").click();
    }


    public void navigateToPasswordReset(Page page) {
        page.navigate(REACT_BASE_URL + "/password-reset");
    }

    public void navigateToNewPassword(Page page) {
        page.navigate(REACT_BASE_URL + "/new-password");
    }

    public void navigateToMailbox(Page page) {
        page.navigate(MAILDEV_BASE_URL);
    }

    public void clickPasswordResetEmail(Page page, String email) {
        page.getByPlaceholder("Search emails...").fill(email);

        Locator resetEmail = page.getByTestId("email-list-item")
                .filter(new Locator.FilterOptions().setHasText(email))
                .filter(new Locator.FilterOptions().setHasText("Reset your password"));
        resetEmail.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        resetEmail.click();

        FrameLocator emailFrame = page.frameLocator("iframe").first();

        Locator resetLink = emailFrame.getByText("Reset password");
        resetLink.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

        String resetUrl = resetLink.getAttribute("href");
        assertNotNull(resetUrl, "Reset link should have an href attribute");

        page.navigate(resetUrl);
        page.locator("id=request-div").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
    }


    public void clickToolbarFilesButton(Page page) {
        page.getByTestId("files-nav-link").click();
    }

    public void navigateToFiles(Page page) {
        page.navigate(REACT_BASE_URL + "/files");
    }

    public void fillAndSubmitCreateCollectionForm(Page page, String collectionName) {
        page.getByText("Create collection").click();

        page.locator("id=input-collectionName-0").fill(collectionName);

        page.getByText("Save").click();
    }

    public void clickCollectionIcon(Page page, String collectionName) {
        page.getByText(collectionName).click();
    }

    public void clickCollectionManagementIcon(Page page) {
        page.getByTestId("cog-icon").click();
    }

    public void clickDeleteCollectionButton(Page page) {
        page.getByText("Delete Collection").click();
    }

    public void clickAndSelectUploadFile(Page page, Path filePath) {
        page.locator("input[type='file']").setInputFiles(filePath);
    }

    public Download clickFileDownloadButton(Page page) {
        return page.waitForDownload(() -> page.locator(".download-btn").click());
    }

    public String getFileContent(Path filePath) {
        try {
            return Files.readString(filePath);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public void clickDeleteFile(Page page) {
        page.getByText("Delete").click();
    }

    public void clickListenToFile(Page page) {
        page.getByText("Listen").click();
    }

    public void clickStreamFile(Page page) {
        page.getByText("Stream").click();
    }

    public void clickViewFile(Page page) {
        page.getByText("View").click();
    }


    public void clickToolbarVaultButton(Page page) {
        page.getByTestId("vault-nav-link").click();
    }

    public void navigateToVault(Page page) {
        page.navigate(REACT_BASE_URL + "/vault");
    }

    public void fillAndSubmitCreateServiceForm(Page page, String serviceName) {
        page.getByText("Create service").click();

        page.locator("id=input-serviceName-0").fill(serviceName);

        page.getByText("Save").click();
    }

    public void selectService(Page page, String serviceName) {
        page.getByText(serviceName).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED));
        page.locator("id=service-select").selectOption(serviceName);
    }

    public void fillAndSubmitCreateEnvironmentForm(Page page, String environmentName) {
        page.getByText("Create environment").click();

        page.locator("id=input-environmentName-0").fill(environmentName);

        page.getByText("Save").click();
    }

    public void selectEnvironment(Page page, String environmentName) {
        page.getByText(environmentName).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED));
        page.locator("id=environment-select").selectOption(environmentName);
    }

    public void fillAndSubmitAddVariableForm(Page page, String key, String value) {
        page.getByText("Add var").click();

        page.locator("id=input-key-0").fill(key);
        page.locator("id=input-value-1").fill(value);

        page.getByText("Save").click();
    }

    public void unmaskVariable(Page page, String key) {
        page.locator("id=unmask-" + key).click();
    }

    public void deleteVariable(Page page, String key) {
        page.locator("id=delete-" + key).click();
    }

    public void uploadEnvFile(Page page, Path filePath) {
        page.locator("input[type='file']").setInputFiles(filePath);
    }

    public Download clickEnvDownloadButton(Page page) {
        return page.waitForDownload(() -> page.locator("id=env-file-download").click());
    }

    public String getEnvFileContent(Path filePath) {
        try {
            return Files.readString(filePath);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public void clickServiceManagementIcon(Page page) {
        page.getByTestId("cog-icon").click();
    }

    public void clickDeleteEnvironmentButton(Page page) {
        page.getByText("Delete Environment").click();
    }

    public void clickDeleteServiceButton(Page page) {
        page.getByText("Delete Service").click();
    }

    public void createServiceEnvironmentAndVariable(Page page, String serviceName, String environmentName,
                                                    String testVariableKey, String testVariableValue) {
        fillAndSubmitCreateServiceForm(page, serviceName);
        selectService(page, serviceName);

        fillAndSubmitCreateEnvironmentForm(page, environmentName);
        selectEnvironment(page, environmentName);

        fillAndSubmitAddVariableForm(page, testVariableKey, testVariableValue);

        Locator variableKeyInput = page.locator("id=key-" + testVariableKey);

        variableKeyInput.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
    }

}
