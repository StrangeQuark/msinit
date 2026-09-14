

package com.example.fileservice;

import com.google.gson.JsonArray;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ExtentTestWatcher.class)
@EnabledIfEnvironmentVariable(named = "FILESERVICE_INTEGRATION", matches = "true")
@EnabledIfEnvironmentVariable(named = "AUTHSERVICE_INTEGRATION", matches = "true")
@EnabledIfEnvironmentVariable(named = "EMAILSERVICE_INTEGRATION", matches = "true")
public class FileTests {
    private static Playwright playwright;
    private static APIRequestContext apiRequestContext;
    private static FileFunctions fileFunctions;
    private static AuthFunctions authFunctions;

    private String testCollectionName;

    private final String TEXT_TEST_FILE = "testUploadFile.txt";

    @BeforeAll
    public static void beforeAll() {
        playwright = Playwright.create();
        apiRequestContext = playwright.request().newContext();
        authFunctions = new AuthFunctions(apiRequestContext);
        fileFunctions = new FileFunctions(apiRequestContext
                , authFunctions
        );
    }

    @AfterAll
    public static void afterAll() {
        apiRequestContext.dispose();
        playwright.close();
    }

    @BeforeEach
    public void beforeEach(TestInfo testInfo) {
        if(testInfo.getTestMethod().get().getName().equals("healthcheckTest") ||
                testInfo.getTestMethod().get().getName().equals("createCollectionTest")) {
            return;
        }
        testCollectionName = "testCollection_" + UUID.randomUUID();

        APIResponse response = fileFunctions.createCollection(testCollectionName);
        assertTrue(response.ok(), "Create collection setup failed: " + response.status() + " - " + response.text());
    }

    @AfterEach
    public void afterEach(TestInfo testInfo) {
        if(testInfo.getTestMethod().get().getName().equals("healthcheckTest") ||
                testInfo.getTestMethod().get().getName().equals("deleteCollectionTest")) {
            return;
        }

        APIResponse response = fileFunctions.deleteCollection(testCollectionName);
        if(!response.ok()) {
            System.err.println("Cleanup failed for " + testCollectionName + ": " + response.status() + " - " + response.text());
        }
    }

    @Test
    public void healthcheckTest() {
        APIResponse response = fileFunctions.healthcheck();
        assertTrue(response.ok(), "File service healthcheck failed: " + response.status() + " - " + response.text());
    }


    @Test
    public void unauthenticatedGetAllCollectionsTest() {
        APIRequestContext unauthenticatedRequestContext = playwright.request().newContext();
        FileFunctions unauthenticatedFileFunctions = new FileFunctions(unauthenticatedRequestContext);
        APIResponse response = unauthenticatedFileFunctions.getAllCollectionsWithoutAccess();

        assertEquals(401, response.status());
        unauthenticatedRequestContext.dispose();
    }

    @Test
    public void normalUserCanGetAllCollectionsTest() {
        String username = "test_" + UUID.randomUUID();
        String email = username + "@email.com";
        String password = UUID.randomUUID().toString();
        String accessToken = authFunctions.registerEnableAuthenticateAccess(username, email, password);

        APIResponse response = fileFunctions.getAllCollections(accessToken);
        assertTrue(response.ok(), "Normal user should access FileService: " + response.status() + " - " + response.text());

        response = authFunctions.deleteUser(username, email, password);
        assertTrue(response.ok(), "User cleanup failed: " + response.status() + " - " + response.text());
    }

    @Test
    public void invalidAccessTokenCannotGetAllCollectionsTest() {
        APIResponse response = fileFunctions.getAllCollections("invalid-token");

        assertEquals(401, response.status());
    }

    @Test
    public void emailServiceAccountCannotGetAllCollectionsTest() {
        String accessToken = authFunctions.extractJwt(authFunctions.serviceAccountAuthenticate("email"));

        APIResponse response = fileFunctions.getAllCollections(accessToken);

        assertEquals(403, response.status());
    }


    @Test
    public void createCollectionTest() {
        testCollectionName = "testCollection_" + UUID.randomUUID();

        APIResponse response = fileFunctions.createCollection(testCollectionName);
        assertTrue(response.ok(), "Create collection test failed: " + response.status() + " - " + response.text());
    }

    @Test
    public void collectionNameSupportsReservedCharactersTest() {
        String collectionName = "test collection/" + UUID.randomUUID();

        APIResponse response = fileFunctions.createCollection(collectionName);
        assertTrue(response.ok(), "Create collection with reserved characters failed: " + response.status() + " - " + response.text());

        response = fileFunctions.deleteCollection(collectionName);
        assertTrue(response.ok(), "Delete collection with reserved characters failed: " + response.status() + " - " + response.text());
    }

    @Test
    public void getAllCollectionsTest() {
        APIResponse response = fileFunctions.getAllCollections();
        assertTrue(response.ok(), "Get all collections test failed: " + response.status() + " - " + response.text());

        JsonArray jsonArray = JsonParser.parseString(response.text()).getAsJsonArray();
        assertEquals(1, jsonArray.size());

        JsonObject jsonObject = jsonArray.get(0).getAsJsonObject();
        assertEquals(testCollectionName, jsonObject.get("name").getAsString());
    }

    @Test
    public void deleteCollectionTest() {
        APIResponse response = fileFunctions.deleteCollection(testCollectionName);
        assertTrue(response.ok(), "Create collection step in delete collection test failed: " + response.status() + " - " + response.text());
    }

    @Test
    public void uploadTest() {
        APIResponse response = fileFunctions.upload(testCollectionName, TEXT_TEST_FILE);
        assertTrue(response.ok(), "File upload test failed: " + response.status() + " - " + response.text());
    }

    @Test
    public void deleteTest() {
        APIResponse response = fileFunctions.upload(testCollectionName, TEXT_TEST_FILE);
        assertTrue(response.ok(), "File upload step in delete file test failed: " + response.status() + " - " + response.text());

        response = fileFunctions.delete(testCollectionName);
        assertTrue(response.ok(), "File delete test failed: " + response.status() + " - " + response.text());

        response = fileFunctions.getAllFiles(testCollectionName);
        assertTrue(response.ok(), "Get all files step in delete file test failed: " + response.status() + " - " + response.text());

        JsonArray jsonArray = JsonParser.parseString(response.text()).getAsJsonArray();
        assertEquals(0, jsonArray.size(), "Deleted file should not be returned by get all files");
    }

    @Test
    public void getAllFilesTest() {
        APIResponse response = fileFunctions.upload(testCollectionName, TEXT_TEST_FILE);
        assertTrue(response.ok(), "File upload step in get all files test failed: " + response.status() + " - " + response.text());

        response = fileFunctions.getAllFiles(testCollectionName);
        assertTrue(response.ok(), "Get all files test failed: " + response.status() + " - " + response.text());

        JsonArray jsonArray = JsonParser.parseString(response.text()).getAsJsonArray();
        assertEquals(1, jsonArray.size(), "Get all files return size test failed: " + response.status() + " - " + response.text());

        String returnFileName = jsonArray.get(0).getAsString();
        assertEquals(TEXT_TEST_FILE, returnFileName, "Get all files return file name test failed: " + response.status() + " - " + response.text());
    }

    @Test
    public void downloadFileTest() {
        APIResponse response = fileFunctions.upload(testCollectionName, TEXT_TEST_FILE);
        assertTrue(response.ok(), "File upload step in download file test failed: " + response.status() + " - " + response.text());

        response = fileFunctions.downloadFile(testCollectionName, TEXT_TEST_FILE);
        assertTrue(response.ok(), "File download test failed: " + response.status() + " - " + response.text());

        try {
            Path filePath = Paths.get(getClass().getClassLoader().getResource("fileserviceTestFiles/" + TEXT_TEST_FILE).toURI());
            assertEquals(Files.readString(filePath), response.text(), "Download file contents test failed: " + response.status() + " - " + response.text());
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load test file from resources", ex);
        }
    }

    @Test
    public void streamFileTest() {
        APIResponse response = fileFunctions.upload(testCollectionName, TEXT_TEST_FILE);
        assertTrue(response.ok(), "File upload step in stream file test failed: " + response.status() + " - " + response.text());

        response = fileFunctions.streamFile(testCollectionName, TEXT_TEST_FILE, "bytes=0-");
        assertTrue(response.ok(), "File stream test failed: " + response.status() + " - " + response.text());

        try {
            Path filePath = Paths.get(getClass().getClassLoader().getResource("fileserviceTestFiles/" + TEXT_TEST_FILE).toURI());
            assertEquals(Files.readString(filePath), response.text(), "Stream file contents test failed: " + response.status() + " - " + response.text());
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load test file from resources", ex);
        }
    }

    @Test
    public void streamFileRejectsInvalidRangeTest() {
        APIResponse response = fileFunctions.upload(testCollectionName, TEXT_TEST_FILE);
        assertTrue(response.ok(), "File upload setup failed: " + response.status() + " - " + response.text());

        response = fileFunctions.streamFile(testCollectionName, TEXT_TEST_FILE, "bytes=-");

        assertEquals(416, response.status());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "GATEWAYSERVICE_INTEGRATION", matches = "true")
    public void gatewayPassesFileCorsHeadersTest() {
        APIResponse response = fileFunctions.upload(testCollectionName, TEXT_TEST_FILE);
        assertTrue(response.ok(), "File upload setup failed: " + response.status() + " - " + response.text());

        response = fileFunctions.preflightThroughGateway(testCollectionName, TEXT_TEST_FILE, "http://localhost:6080");
        assertTrue(response.ok(), "Gateway CORS preflight failed: " + response.status() + " - " + response.text());
        assertEquals("http://localhost:6080", response.headers().get("access-control-allow-origin"));

        response = fileFunctions.streamFileThroughGateway(testCollectionName, TEXT_TEST_FILE, "bytes=0-");
        assertEquals(206, response.status());
        assertEquals("http://localhost:6080", response.headers().get("access-control-allow-origin"));
        assertTrue(response.headers().get("access-control-expose-headers").contains("Content-Range"));
        assertTrue(response.headers().get("content-range") != null);
        assertEquals("Gateway Service", response.headers().get("x-powered-by"));

        response = fileFunctions.preflightThroughGateway(testCollectionName, TEXT_TEST_FILE, "http://not-allowed.example");
        assertEquals(403, response.status());
    }


    @Test
    public void getCurrentUserRoleTest() {
        APIResponse response = fileFunctions.getCurrentUserRole(testCollectionName);
        assertTrue(response.ok(), "Get current user role test failed: " + response.status() + " - " + response.text());
        assertEquals("OWNER", response.text().replace("\"", ""));
    }

    @Test
    public void getUsersByCollectionTest() {
        APIResponse response = fileFunctions.getUsersByCollection(testCollectionName);
        assertTrue(response.ok(), "Get users by collection test failed: " + response.status() + " - " + response.text());

        JsonArray jsonArray = JsonParser.parseString(response.text()).getAsJsonArray();
        assertEquals(1, jsonArray.size(), "Get users by collection return size test failed: " + response.status() + " - " + response.text());
    }

    @Test
    public void getAllRolesTest() {
        APIResponse response = fileFunctions.getAllRoles();
        assertTrue(response.ok(), "Get all roles test failed: " + response.status() + " - " + response.text());

        JsonArray jsonArray = JsonParser.parseString(response.text()).getAsJsonArray();
        assertEquals(4, jsonArray.size(), "Get all roles return size test failed: " + response.status() + " - " + response.text());

        String returnString = jsonArray.toString();
        assertTrue(returnString.contains("OWNER"), "File service should contain OWNER role");
        assertTrue(returnString.contains("MANAGER"), "File service should contain MANAGER role");
        assertTrue(returnString.contains("READ_WRITE"), "File service should contain READ_WRITE role");
        assertTrue(returnString.contains("READ"), "File service should contain READ role");
    }

    @Test
    public void addUserToCollectionTest() {
        fileFunctions.testUsername = "test_" + UUID.randomUUID();
        fileFunctions.testEmail = fileFunctions.testUsername + "@email.com";
        fileFunctions.testPassword = UUID.randomUUID().toString();

        authFunctions.registerEnableAuthenticateAccess(fileFunctions.testUsername, fileFunctions.testEmail, fileFunctions.testPassword);

        APIResponse response = fileFunctions.addUserToCollection(testCollectionName);
        assertTrue(response.ok(), "Add user to collection test failed: " + response.status() + " - " + response.text());

        response = fileFunctions.getUsersByCollection(testCollectionName);
        assertTrue(response.ok(), "Get users by collection in add user to collection test failed: " + response.status() + " - " + response.text());

        JsonArray jsonArray = JsonParser.parseString(response.text()).getAsJsonArray();
        assertEquals(2, jsonArray.size(), "Add user to collection return size test failed: " + response.status() + " - " + response.text());

        authFunctions.deleteUser(fileFunctions.testUsername, fileFunctions.testEmail, fileFunctions.testPassword);
        assertTrue(response.ok(), "Delete user in add user to collection test failed: " + response.status() + " - " + response.text());
    }

    @Test
    public void updateUserRoleTest() {
        fileFunctions.testUsername = "test_" + UUID.randomUUID();
        fileFunctions.testEmail = fileFunctions.testUsername + "@email.com";
        fileFunctions.testPassword = UUID.randomUUID().toString();

        authFunctions.registerEnableAuthenticateAccess(fileFunctions.testUsername, fileFunctions.testEmail, fileFunctions.testPassword);

        APIResponse response = fileFunctions.addUserToCollection(testCollectionName);
        assertTrue(response.ok(), "Add user to collection in update user role test failed: " + response.status() + " - " + response.text());

        response = fileFunctions.updateUserRole(testCollectionName, "MANAGER");
        assertTrue(response.ok(), "Update user role test failed: " + response.status() + " - " + response.text());

        response = fileFunctions.getUsersByCollection(testCollectionName);
        assertTrue(response.ok(), "Get users by collection in update user role test failed: " + response.status() + " - " + response.text());

        JsonArray jsonArray = JsonParser.parseString(response.text()).getAsJsonArray();
        assertTrue(jsonArray.toString().contains("MANAGER"), "Users list should contain newly update MANAGER role in update user role test");

        authFunctions.deleteUser(fileFunctions.testUsername, fileFunctions.testEmail, fileFunctions.testPassword);
        assertTrue(response.ok(), "Delete user in update user role test failed: " + response.status() + " - " + response.text());
    }

    @Test
    public void deleteUserFromCollectionTest() {
        fileFunctions.testUsername = "test_" + UUID.randomUUID();
        fileFunctions.testEmail = fileFunctions.testUsername + "@email.com";
        fileFunctions.testPassword = UUID.randomUUID().toString();

        authFunctions.registerEnableAuthenticateAccess(fileFunctions.testUsername, fileFunctions.testEmail, fileFunctions.testPassword);

        APIResponse response = fileFunctions.addUserToCollection(testCollectionName);
        assertTrue(response.ok(), "Add user to collection in delete user from collection test failed: " + response.status() + " - " + response.text());

        response = fileFunctions.getUsersByCollection(testCollectionName);
        assertTrue(response.ok(), "Get users by collection in delete user from collection test failed: " + response.status() + " - " + response.text());

        JsonArray jsonArray = JsonParser.parseString(response.text()).getAsJsonArray();
        assertEquals(2, jsonArray.size(), "Add user to collection in delete user from collection return size test failed: " + response.status() + " - " + response.text());

        response = fileFunctions.deleteUserFromCollection(testCollectionName);
        assertTrue(response.ok(), "Delete user from collection test failed: " + response.status() + " - " + response.text());

        response = fileFunctions.getUsersByCollection(testCollectionName);
        assertTrue(response.ok(), "Second get users by collection in delete user from collection test failed: " + response.status() + " - " + response.text());

        jsonArray = JsonParser.parseString(response.text()).getAsJsonArray();
        assertEquals(1, jsonArray.size(), "Delete user from collection return size test failed: " + response.status() + " - " + response.text());

        authFunctions.deleteUser(fileFunctions.testUsername, fileFunctions.testEmail, fileFunctions.testPassword);
        assertTrue(response.ok(), "Delete user in delete user from collection test failed: " + response.status() + " - " + response.text());
    }

    @Test
    public void deleteUserFromAllCollectionsTest() {
        fileFunctions.testUsername = "test_" + UUID.randomUUID();
        fileFunctions.testEmail = fileFunctions.testUsername + "@email.com";
        fileFunctions.testPassword = UUID.randomUUID().toString();

        authFunctions.registerEnableAuthenticateAccess(fileFunctions.testUsername, fileFunctions.testEmail, fileFunctions.testPassword);

        APIResponse response = fileFunctions.addUserToCollection(testCollectionName);
        assertTrue(response.ok(), "Add user to collection in delete user from all collections test failed: " + response.status() + " - " + response.text());

        response = fileFunctions.getUsersByCollection(testCollectionName);
        assertTrue(response.ok(), "Get users by collection in delete user from all collections test failed: " + response.status() + " - " + response.text());

        JsonArray jsonArray = JsonParser.parseString(response.text()).getAsJsonArray();
        assertEquals(2, jsonArray.size(), "Add user to collection in delete user from all collections return size test failed: " + response.status() + " - " + response.text());

        response = fileFunctions.deleteUserFromAllCollections();
        assertTrue(response.ok(), "Delete user from all collections test failed: " + response.status() + " - " + response.text());

        response = fileFunctions.getUsersByCollection(testCollectionName);
        assertTrue(response.ok(), "Second get users by collection in delete user from all collections test failed: " + response.status() + " - " + response.text());

        jsonArray = JsonParser.parseString(response.text()).getAsJsonArray();
        assertEquals(1, jsonArray.size(), "Delete user from all collections return size test failed: " + response.status() + " - " + response.text());

        authFunctions.deleteUser(fileFunctions.testUsername, fileFunctions.testEmail, fileFunctions.testPassword);
        assertTrue(response.ok(), "Delete user in delete user from all collections test failed: " + response.status() + " - " + response.text());
    }
}
