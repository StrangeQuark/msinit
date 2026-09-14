

package com.example.reactservice;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.example.authservice.AuthFunctions;
import com.example.utility.AuthUtility;
import com.example.utility.ExtentTestWatcher;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(ExtentTestWatcher.class)
public class ReactTestsBase {
    public Playwright playwright;
    public Page page;
    public Browser browser;
    public BrowserContext context;
    public ReactFunctions reactFunctions;
    public final Locator.WaitForOptions WAIT_FOR_VISIBLE = new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE);
    public final Locator.WaitForOptions WAIT_FOR_DETACHED = new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED);
    public final Locator.WaitForOptions WAIT_FOR_ATTACHED = new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED);

    public static APIRequestContext apiRequestContext;
    public AuthFunctions authFunctions;
    public String username;
    public String email;
    public String password;
    public AuthUtility authUtility;
    public String collectionName;
    public String textFileName;
    public Path textFilePath;
    public String audioFileName;
    public Path audioFilePath;
    public String imageFileName;
    public Path imageFilePath;
    public String videoFileName;
    public Path videoFilePath;
    public String serviceName;
    public String environmentName;
    public String testVariableKey;
    public String testVariableValue;
    public String envFileName;
    public Path envFilePath;


    @BeforeAll
    public void beforeAll() throws URISyntaxException {
        playwright = Playwright.create();
        apiRequestContext = playwright.request().newContext();
        authFunctions = new AuthFunctions(apiRequestContext);
        reactFunctions = new ReactFunctions(authFunctions);
        authUtility = new AuthUtility(apiRequestContext);
        textFileName = "testUploadFile.txt";
        textFilePath = Paths.get(getClass().getClassLoader().getResource("fileserviceTestFiles/" + textFileName).toURI());
        audioFileName = "testAudioFile.mp3";
        audioFilePath = Paths.get(getClass().getClassLoader().getResource("fileserviceTestFiles/" + audioFileName).toURI());
        imageFileName = "testImageFile.png";
        imageFilePath = Paths.get(getClass().getClassLoader().getResource("fileserviceTestFiles/" + imageFileName).toURI());
        videoFileName = "testVideoFile.webm";
        videoFilePath = Paths.get(getClass().getClassLoader().getResource("fileserviceTestFiles/" + videoFileName).toURI());


        envFileName = "testEnvFile.env";
        envFilePath = Paths.get(getClass().getClassLoader().getResource("vaultserviceTestFiles/" + envFileName).toURI());

    }

    @BeforeEach
    public void beforeEach(TestInfo testInfo) {
        browser = playwright.webkit().launch(new BrowserType.LaunchOptions().setHeadless(true));
        context = browser.newContext();
        page = context.newPage();

        page.onRequestFailed(req -> {
            if (!req.url().contains("http://maildev:1080/socket.io/"))
                System.out.println("Request failed: " + req.url());
        });


        if(testInfo.getTestMethod().get().getName().startsWith("user")) {
            username = "test_" + UUID.randomUUID();
            email = username + "@testEmail.com";
            password = "testPassword123!";
        }
        collectionName = "collection_" + UUID.randomUUID();
        serviceName = "service_" + UUID.randomUUID();
        environmentName = "environment_" + UUID.randomUUID();
        testVariableKey = "testKey_" + UUID.randomUUID().toString().replace("-", "_");
        testVariableValue = "testValue_" + UUID.randomUUID();

    }

    @AfterEach
    public void afterEach(TestInfo testInfo) {
        try {

            if(testInfo.getTestMethod().get().getName().startsWith("user")) {
                authFunctions.deleteUser(username, email, password);
                assertFalse(authFunctions.getUserId(username, authUtility.authenticateServiceAccount()).ok(), "User cleanup failed in React service register test");
            }
        } finally {
            if(page != null)
                page.close();
            if(context != null)
                context.close();
            if(browser != null)
                browser.close();
        }
    }

    @AfterAll
    public void afterAll() {
        apiRequestContext.dispose();
        playwright.close();
    }
}
