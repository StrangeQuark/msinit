

package com.example.vaultservice;

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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(ExtentTestWatcher.class)
@EnabledIfEnvironmentVariable(named = "VAULTSERVICE_INTEGRATION", matches = "true")
@EnabledIfEnvironmentVariable(named = "AUTHSERVICE_INTEGRATION", matches = "true")
@EnabledIfEnvironmentVariable(named = "EMAILSERVICE_INTEGRATION", matches = "true")
public class VaultTests {
    private static Playwright playwright;
    private static APIRequestContext apiRequestContext;
    private static VaultFunctions vaultFunctions;
    private static AuthFunctions authFunctions;

    private String testServiceName;
    private String testEnvironmentName;
    private String testVariableName;
    private String testVariableValue;

    private final String ENV_TEST_FILE = "testEnvFile.env";

    @BeforeAll
    public static void beforeAll() {
        playwright = Playwright.create();
        apiRequestContext = playwright.request().newContext();
        authFunctions = new AuthFunctions(apiRequestContext);
        vaultFunctions = new VaultFunctions(apiRequestContext
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
                testInfo.getTestMethod().get().getName().equals("createServiceTest") ||
                testInfo.getTestMethod().get().getName().equals("createEnvironmentTest")) {
            return;
        }
        testServiceName = "testService_" + UUID.randomUUID();
        testEnvironmentName = "testEnvironment_" + UUID.randomUUID();
        testVariableName = "testVariable_" + UUID.randomUUID().toString().replace("-", "_");
        testVariableValue = "testValue_" + UUID.randomUUID();

        APIResponse response = vaultFunctions.createService(testServiceName);
        assertTrue(response.ok(), "Create service setup failed: " + response.status() + " - " + response.text());

        response = vaultFunctions.createEnvironment(testServiceName, testEnvironmentName);
        assertTrue(response.ok(), "Create environment setup failed: " + response.status() + " - " + response.text());

        if(!testInfo.getTestMethod().get().getName().equals("addVariableTest") &&
                !testInfo.getTestMethod().get().getName().equals("downloadEnvFileTest")) {
            response = vaultFunctions.addVariable(testServiceName, testEnvironmentName, testVariableName, testVariableValue);
            assertTrue(response.ok(), "Add variable setup failed: " + response.status() + " - " + response.text());
        }
    }

    @AfterEach
    public void afterEach(TestInfo testInfo) {
        if(testInfo.getTestMethod().get().getName().equals("healthcheckTest") ||
                testInfo.getTestMethod().get().getName().equals("deleteServiceTest")) {
            return;
        }

        APIResponse response = vaultFunctions.deleteService(testServiceName);
        if(!response.ok()) {
            System.err.println("Cleanup failed for " + testServiceName + ": " + response.status() + " - " + response.text());
        }
    }

    @Test
    public void healthcheckTest() {
        APIResponse response = vaultFunctions.healthcheck();
        assertTrue(response.ok(), "Vault service healthcheck failed: " + response.status() + " - " + response.text());
    }


    @Test
    public void unauthenticatedGetAllServicesTest() {
        APIRequestContext unauthenticatedRequestContext = playwright.request().newContext();
        VaultFunctions unauthenticatedVaultFunctions = new VaultFunctions(unauthenticatedRequestContext);
        APIResponse response = unauthenticatedVaultFunctions.getAllServicesWithoutAccess();

        assertEquals(401, response.status());
        unauthenticatedRequestContext.dispose();
    }

    @Test
    public void invalidAccessTokenCannotGetAllServicesTest() {
        APIResponse response = vaultFunctions.getAllServices("invalid-token");

        assertEquals(401, response.status());
    }

    @Test
    public void emailServiceAccountCannotGetAllServicesTest() {
        String accessToken = authFunctions.extractJwt(authFunctions.serviceAccountAuthenticate("email"));

        APIResponse response = vaultFunctions.getAllServices(accessToken);

        assertEquals(403, response.status());
    }

    @Test
    public void normalUserCannotGetAllServicesTest() {
        String username = "test_" + UUID.randomUUID();
        String email = username + "@email.com";
        String password = UUID.randomUUID().toString();
        String accessToken = authFunctions.registerEnableAuthenticateAccess(username, email, password);

        APIResponse response = vaultFunctions.getAllServices(accessToken);
        assertEquals(403, response.status());

        response = authFunctions.deleteUser(username, email, password);
        assertTrue(response.ok(), "User cleanup failed: " + response.status() + " - " + response.text());
    }

    @Test
    public void unauthenticatedGetUsersByServiceTest() {
        APIRequestContext unauthenticatedRequestContext = playwright.request().newContext();
        VaultFunctions unauthenticatedVaultFunctions = new VaultFunctions(unauthenticatedRequestContext);
        APIResponse response = unauthenticatedVaultFunctions.getUsersByServiceWithoutAccess("testService_" + UUID.randomUUID());

        assertEquals(401, response.status());
        unauthenticatedRequestContext.dispose();
    }


    @Test
    public void createServiceTest() {
        testServiceName = "testService_" + UUID.randomUUID();

        APIResponse response = vaultFunctions.createService(testServiceName);
        assertTrue(response.ok(), "Create service test failed: " + response.status() + " - " + response.text());
    }

    @Test
    public void serviceAndEnvironmentNamesSupportReservedCharactersTest() {
        String serviceName = "test service/" + UUID.randomUUID();
        String environmentName = "test environment[" + UUID.randomUUID() + "]";

        APIResponse response = vaultFunctions.createService(serviceName);
        assertTrue(response.ok(), "Create service with reserved characters failed: " + response.status() + " - " + response.text());

        response = vaultFunctions.createEnvironment(serviceName, environmentName);
        assertTrue(response.ok(), "Create environment with reserved characters failed: " + response.status() + " - " + response.text());

        response = vaultFunctions.deleteService(serviceName);
        assertTrue(response.ok(), "Delete service with reserved characters failed: " + response.status() + " - " + response.text());
    }

    @Test
    public void createEnvironmentTest() {
        testServiceName = "testService_" + UUID.randomUUID();
        testEnvironmentName = "testEnvironment_" + UUID.randomUUID();

        APIResponse response = vaultFunctions.createService(testServiceName);
        assertTrue(response.ok(), "Create service in create environment test failed: " + response.status() + " - " + response.text());

        response = vaultFunctions.createEnvironment(testServiceName, testEnvironmentName);
        assertTrue(response.ok(), "Create environment test failed: " + response.status() + " - " + response.text());
    }

    @Test
    public void bootstrapEnvFileTest() {
        String bootstrapEnvironmentName = "testBootstrapEnvironment_" + UUID.randomUUID();

        APIResponse response = vaultFunctions.bootstrapEnvFile(testServiceName, bootstrapEnvironmentName, ENV_TEST_FILE);
        assertTrue(response.ok(), "Bootstrap env file test failed: " + response.status() + " - " + response.text());
    }

    @Test
    public void bootstrapEnvFileWithoutTokenTest() {
        String bootstrapServiceName = "testBootstrapService_" + UUID.randomUUID();
        String bootstrapEnvironmentName = "testBootstrapEnvironment_" + UUID.randomUUID();

        APIResponse response = vaultFunctions.bootstrapEnvFileWithoutToken(bootstrapServiceName, bootstrapEnvironmentName, ENV_TEST_FILE);

        assertEquals(400, response.status());
    }

    @Test
    public void getServiceTest() {
        APIResponse response = vaultFunctions.getService(testServiceName);
        assertTrue(response.ok(), "Get service test failed: " + response.status() + " - " + response.text());
    }

    @Test
    public void getEnvironmentsByServiceTest() {
        APIResponse response = vaultFunctions.getEnvironmentsByService(testServiceName);
        assertTrue(response.ok(), "Get environments by service test failed: " + response.status() + " - " + response.text());

        JsonArray jsonArray = JsonParser.parseString(response.text()).getAsJsonArray();
        assertEquals(1, jsonArray.size(), "Get environments by service return size failed");

        assertTrue(jsonArray.toString().contains(testEnvironmentName), "Get environments by service return failed");
    }

    @Test
    public void getEnvironmentTest() {
        APIResponse response = vaultFunctions.getEnvironment(testServiceName, testEnvironmentName);
        assertTrue(response.ok(), "Get environment test failed: " + response.status() + " - " + response.text());

        JsonObject jsonObject = JsonParser.parseString(response.text()).getAsJsonObject();
        assertEquals(testEnvironmentName, jsonObject.get("name").getAsString());
    }

    @Test
    public void getVariablesByServiceTest() {
        APIResponse response = vaultFunctions.getVariablesByService(testServiceName);
        assertTrue(response.ok(), "Get variables by service test failed: " + response.status() + " - " + response.text());

        JsonArray jsonArray = JsonParser.parseString(response.text()).getAsJsonArray();
        assertEquals(1, jsonArray.size(), "Get variables by service return size failed");

        JsonObject jsonObject = jsonArray.get(0).getAsJsonObject();
        assertEquals(testVariableName, jsonObject.get("key").getAsString(), "Get variables by service return variable key test failed");
        assertEquals(testVariableValue, jsonObject.get("value").getAsString(), "Get variables by service return variable value test failed");
    }

    @Test
    public void getVariablesByEnvironmentTest() {
        APIResponse response = vaultFunctions.getVariablesByEnvironment(testServiceName, testEnvironmentName);
        assertTrue(response.ok(), "Get variables by environment test failed: " + response.status() + " - " + response.text());

        JsonArray jsonArray = JsonParser.parseString(response.text()).getAsJsonArray();
        assertEquals(1, jsonArray.size(), "Get variables by environment return size failed");

        JsonObject jsonObject = jsonArray.get(0).getAsJsonObject();
        assertEquals(testVariableName, jsonObject.get("key").getAsString(), "Get variables by environment return variable key test failed");
        assertEquals(testVariableValue, jsonObject.get("value").getAsString(), "Get variables by environment return variable value test failed");
    }

    @Test
    public void getVariableByNameTest() {
        APIResponse response = vaultFunctions.getVariableByName(testServiceName, testEnvironmentName, testVariableName);
        assertTrue(response.ok(), "Get variable by name test failed: " + response.status() + " - " + response.text());

        JsonObject jsonObject = JsonParser.parseString(response.text()).getAsJsonObject();
        assertEquals(testVariableName, jsonObject.get("key").getAsString(), "Get variable by name return variable key test failed");
        assertEquals(testVariableValue, jsonObject.get("value").getAsString(), "Get variable by name return variable value test failed");
    }

    @Test
    public void addVariableTest() {
        APIResponse response = vaultFunctions.addVariable(testServiceName, testEnvironmentName, testVariableName, testVariableValue);
        assertTrue(response.ok(), "Add variable test failed: " + response.status() + " - " + response.text());

        response = vaultFunctions.getVariableByName(testServiceName, testEnvironmentName, testVariableName);
        assertTrue(response.ok(), "Get variable by name in add variable test failed: " + response.status() + " - " + response.text());

        JsonObject jsonObject = JsonParser.parseString(response.text()).getAsJsonObject();
        assertEquals(testVariableName, jsonObject.get("key").getAsString(), "Get variable by name in add variable return variable key test failed");
        assertEquals(testVariableValue, jsonObject.get("value").getAsString(), "Get variable by name in add variable return variable value test failed");
    }

    @Test
    public void updateVariableTest() {
        testVariableValue = "updatedTestVariableValue";

        APIResponse response = vaultFunctions.updateVariable(testServiceName, testEnvironmentName, testVariableName, testVariableValue);
        assertTrue(response.ok(), "Update variable test failed: " + response.status() + " - " + response.text());

        response = vaultFunctions.getVariableByName(testServiceName, testEnvironmentName, testVariableName);
        assertTrue(response.ok(), "Get variable by name in update variable test failed: " + response.status() + " - " + response.text());

        JsonObject jsonObject = JsonParser.parseString(response.text()).getAsJsonObject();
        assertEquals(testVariableName, jsonObject.get("key").getAsString(), "Get variable by name in update variable return variable key test failed");
        assertEquals(testVariableValue, jsonObject.get("value").getAsString(), "Get variable by name in update variable return variable value test failed");
    }

    @Test
    public void addEnvFileTest() {
        APIResponse response = vaultFunctions.addEnvFile(testServiceName, testEnvironmentName, ENV_TEST_FILE);
        assertTrue(response.ok(), "Add env file test failed: " + response.status() + " - " + response.text());

        response = vaultFunctions.getVariablesByEnvironment(testServiceName, testEnvironmentName);
        assertTrue(response.ok(), "Get variables by environment in add env file test failed: " + response.status() + " - " + response.text());

        JsonArray jsonArray = JsonParser.parseString(response.text()).getAsJsonArray();
        assertEquals(2, jsonArray.size(), "Get variables by environment in add env file return size failed");

        String returnString = jsonArray.toString();
        assertTrue(returnString.contains("testEnvKey"), "Add env file return testEnvVariable key test failed");
        assertTrue(returnString.contains("testEnvValue"), "Add env file return testEnvVariable value test failed");
    }

    @Test
    public void downloadEnvFileTest() {
        APIResponse response = vaultFunctions.addEnvFile(testServiceName, testEnvironmentName, ENV_TEST_FILE);
        assertTrue(response.ok(), "File upload step in download env file test failed: " + response.status() + " - " + response.text());

        response = vaultFunctions.downloadEnvFile(testServiceName, testEnvironmentName);
        assertTrue(response.ok(), "Env file download test failed: " + response.status() + " - " + response.text());

        try {
            Path filePath = Paths.get(getClass().getClassLoader().getResource("vaultserviceTestFiles/" + ENV_TEST_FILE).toURI());
            assertTrue(Files.readString(filePath).contains(response.text()), "Download env file contents test failed: " + response.status() + " - " + response.text());
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load test env file from resources", ex);
        }
    }

    @Test
    public void deleteVariableTest() {
        APIResponse response = vaultFunctions.deleteVariable(testServiceName, testEnvironmentName, testVariableName);
        assertTrue(response.ok(), "Delete variable test failed: " + response.status() + " - " + response.text());

        response = vaultFunctions.getVariablesByEnvironment(testServiceName, testEnvironmentName);
        assertTrue(response.ok(), "Get variables by environment in delete variable test failed: " + response.status() + " - " + response.text());

        JsonArray jsonArray = JsonParser.parseString(response.text()).getAsJsonArray();
        assertEquals(0, jsonArray.size(), "Get variables by environment in delete variable return size failed");
    }

    @Test
    public void deleteEnvironmentTest() {
        APIResponse response = vaultFunctions.deleteEnvironment(testServiceName, testEnvironmentName);
        assertTrue(response.ok(), "Delete environment test failed: " + response.status() + " - " + response.text());

        response = vaultFunctions.getEnvironmentsByService(testServiceName);
        assertTrue(response.ok(), "Get environments by service in delete environment test failed: " + response.status() + " - " + response.text());

        JsonArray jsonArray = JsonParser.parseString(response.text()).getAsJsonArray();
        assertEquals(0, jsonArray.size(), "Get environments by service in delete environment return size failed");
    }

    @Test
    public void deleteServiceTest() {
        APIResponse response = vaultFunctions.deleteService(testServiceName);
        assertTrue(response.ok(), "Delete service test failed: " + response.status() + " - " + response.text());

        response = vaultFunctions.getService(testServiceName);
        assertFalse(response.ok(), "Get service in delete service test failed: " + response.status() + " - " + response.text());
    }

    @Test
    public void getAllServicesTest() {
        APIResponse response = vaultFunctions.getAllServices();
        assertTrue(response.ok(), "Get all services test failed: " + response.status() + " - " + response.text());

        JsonArray jsonArray = JsonParser.parseString(response.text()).getAsJsonArray();
        assertEquals(1, jsonArray.size(), "Get all services return size failed");

        assertTrue(jsonArray.toString().contains(testServiceName), "Get all services return failed");
    }

    @Test
    public void getUsersByServiceTest() {
        APIResponse response = vaultFunctions.getUsersByService(testServiceName);
        assertTrue(response.ok(), "Get users by service test failed: " + response.status() + " - " + response.text());

        JsonArray jsonArray = JsonParser.parseString(response.text()).getAsJsonArray();
        assertEquals(1, jsonArray.size(), "Get users by service return size test failed: " + response.status() + " - " + response.text());
    }

    @Test
    public void getUsersByServiceAccessTest() {
        String managerUsername = "testManager_" + UUID.randomUUID();
        String managerEmail = managerUsername + "@email.com";
        String managerPassword = UUID.randomUUID().toString();
        String managerToken = authFunctions.registerEnableAuthenticateAccess(managerUsername, managerEmail, managerPassword);

        APIResponse response = authFunctions.updateRole("DEVELOPER", managerUsername, authFunctions.authenticateInitialSuperUser());
        assertTrue(response.ok(), "Promote manager to developer failed: " + response.status() + " - " + response.text());
        authFunctions.authenticate(managerUsername, managerPassword);
        managerToken = authFunctions.extractJwt(authFunctions.serveAccessToken());

        String nonMemberUsername = "testNonMember_" + UUID.randomUUID();
        String nonMemberEmail = nonMemberUsername + "@email.com";
        String nonMemberPassword = UUID.randomUUID().toString();
        String nonMemberToken = authFunctions.registerEnableAuthenticateAccess(nonMemberUsername, nonMemberEmail, nonMemberPassword);

        String ownerToken = authFunctions.extractJwt(authFunctions.serviceAccountAuthenticate("test"));
        response = vaultFunctions.addUserToService(testServiceName, managerUsername, "MANAGER", ownerToken);
        assertTrue(response.ok(), "Add manager to service failed: " + response.status() + " - " + response.text());

        response = vaultFunctions.getUsersByService(testServiceName, managerToken);
        assertTrue(response.ok(), "Manager should be able to get service users: " + response.status() + " - " + response.text());
        assertFalse(response.text().contains("\"id\""), "Service user response should not contain a membership id");

        response = vaultFunctions.getUsersByService(testServiceName, nonMemberToken);
        assertFalse(response.ok(), "Non-member should not be able to get service users");

        authFunctions.deleteUser(managerUsername, managerEmail, managerPassword);
        authFunctions.deleteUser(nonMemberUsername, nonMemberEmail, nonMemberPassword);
    }

    @Test
    public void getAllRolesTest() {
        APIResponse response = vaultFunctions.getAllRoles();
        assertTrue(response.ok(), "Get all roles test failed: " + response.status() + " - " + response.text());

        JsonArray jsonArray = JsonParser.parseString(response.text()).getAsJsonArray();
        assertEquals(3, jsonArray.size(), "Get all roles return size test failed: " + response.status() + " - " + response.text());

        String returnString = jsonArray.toString();
        assertTrue(returnString.contains("OWNER"), "Vault service should contain OWNER role");
        assertTrue(returnString.contains("MANAGER"), "Vault service should contain MANAGER role");
        assertTrue(returnString.contains("MAINTAINER"), "Vault service should contain MAINTAINER role");
    }

    @Test
    public void getCurrentUserRoleTest() {
        APIResponse response = vaultFunctions.getCurrentUserRole(testServiceName);
        assertTrue(response.ok(), "Get current user role test failed: " + response.status() + " - " + response.text());
        assertEquals("OWNER", response.text().replace("\"", ""));
    }

    @Test
    public void updateUserRoleTest() {
        vaultFunctions.testUsername = "test_" + UUID.randomUUID();
        vaultFunctions.testEmail = vaultFunctions.testUsername + "@email.com";
        vaultFunctions.testPassword = UUID.randomUUID().toString();

        authFunctions.registerEnableAuthenticateAccess(vaultFunctions.testUsername, vaultFunctions.testEmail, vaultFunctions.testPassword);

        APIResponse response = vaultFunctions.addUserToService(testServiceName);
        assertTrue(response.ok(), "Add user to service in update user role test failed: " + response.status() + " - " + response.text());

        response = vaultFunctions.updateUserRole(testServiceName, "MANAGER");
        assertTrue(response.ok(), "Update user role test failed: " + response.status() + " - " + response.text());

        response = vaultFunctions.getUsersByService(testServiceName);
        assertTrue(response.ok(), "Get users by service in update user role test failed: " + response.status() + " - " + response.text());

        JsonArray jsonArray = JsonParser.parseString(response.text()).getAsJsonArray();
        assertTrue(jsonArray.toString().contains("MANAGER"), "Users list should contain newly update MANAGER role in update user role test");

        authFunctions.deleteUser(vaultFunctions.testUsername, vaultFunctions.testEmail, vaultFunctions.testPassword);
        assertTrue(response.ok(), "Delete user in update user role test failed: " + response.status() + " - " + response.text());
    }

    @Test
    public void addUserToCollectionTest() {
        vaultFunctions.testUsername = "test_" + UUID.randomUUID();
        vaultFunctions.testEmail = vaultFunctions.testUsername + "@email.com";
        vaultFunctions.testPassword = UUID.randomUUID().toString();

        authFunctions.registerEnableAuthenticateAccess(vaultFunctions.testUsername, vaultFunctions.testEmail, vaultFunctions.testPassword);

        APIResponse response = vaultFunctions.addUserToService(testServiceName);
        assertTrue(response.ok(), "Add user to service test failed: " + response.status() + " - " + response.text());

        response = vaultFunctions.getUsersByService(testServiceName);
        assertTrue(response.ok(), "Get users by service in add user to service test failed: " + response.status() + " - " + response.text());

        JsonArray jsonArray = JsonParser.parseString(response.text()).getAsJsonArray();
        assertEquals(2, jsonArray.size(), "Add user to service return size test failed: " + response.status() + " - " + response.text());

        authFunctions.deleteUser(vaultFunctions.testUsername, vaultFunctions.testEmail, vaultFunctions.testPassword);
        assertTrue(response.ok(), "Delete user in add user to service test failed: " + response.status() + " - " + response.text());
    }

    @Test
    public void deleteUserFromCollectionTest() {
        vaultFunctions.testUsername = "test_" + UUID.randomUUID();
        vaultFunctions.testEmail = vaultFunctions.testUsername + "@email.com";
        vaultFunctions.testPassword = UUID.randomUUID().toString();

        authFunctions.registerEnableAuthenticateAccess(vaultFunctions.testUsername, vaultFunctions.testEmail, vaultFunctions.testPassword);

        APIResponse response = vaultFunctions.addUserToService(testServiceName);
        assertTrue(response.ok(), "Add user to service in delete user from service test failed: " + response.status() + " - " + response.text());

        response = vaultFunctions.getUsersByService(testServiceName);
        assertTrue(response.ok(), "Get users by service in delete user from service test failed: " + response.status() + " - " + response.text());

        JsonArray jsonArray = JsonParser.parseString(response.text()).getAsJsonArray();
        assertEquals(2, jsonArray.size(), "Add user to service in delete user from service return size test failed: " + response.status() + " - " + response.text());

        response = vaultFunctions.deleteUserFromService(testServiceName);
        assertTrue(response.ok(), "Delete user from service test failed: " + response.status() + " - " + response.text());

        response = vaultFunctions.getUsersByService(testServiceName);
        assertTrue(response.ok(), "Second get users by service in delete user from service test failed: " + response.status() + " - " + response.text());

        jsonArray = JsonParser.parseString(response.text()).getAsJsonArray();
        assertEquals(1, jsonArray.size(), "Delete user from service return size test failed: " + response.status() + " - " + response.text());

        authFunctions.deleteUser(vaultFunctions.testUsername, vaultFunctions.testEmail, vaultFunctions.testPassword);
        assertTrue(response.ok(), "Delete user in delete user from service test failed: " + response.status() + " - " + response.text());
    }

    @Test
    public void deleteUserFromAllCollectionsTest() {
        vaultFunctions.testUsername = "test_" + UUID.randomUUID();
        vaultFunctions.testEmail = vaultFunctions.testUsername + "@email.com";
        vaultFunctions.testPassword = UUID.randomUUID().toString();

        authFunctions.registerEnableAuthenticateAccess(vaultFunctions.testUsername, vaultFunctions.testEmail, vaultFunctions.testPassword);

        APIResponse response = vaultFunctions.addUserToService(testServiceName);
        assertTrue(response.ok(), "Add user to service in delete user from all services test failed: " + response.status() + " - " + response.text());

        response = vaultFunctions.getUsersByService(testServiceName);
        assertTrue(response.ok(), "Get users by service in delete user from all services test failed: " + response.status() + " - " + response.text());

        JsonArray jsonArray = JsonParser.parseString(response.text()).getAsJsonArray();
        assertEquals(2, jsonArray.size(), "Add user to service in delete user from all services return size test failed: " + response.status() + " - " + response.text());

        response = vaultFunctions.deleteUserFromAllServices();
        assertTrue(response.ok(), "Delete user from all services test failed: " + response.status() + " - " + response.text());

        response = vaultFunctions.getUsersByService(testServiceName);
        assertTrue(response.ok(), "Second get users by service in delete user from all services test failed: " + response.status() + " - " + response.text());

        jsonArray = JsonParser.parseString(response.text()).getAsJsonArray();
        assertEquals(1, jsonArray.size(), "Delete user from all services return size test failed: " + response.status() + " - " + response.text());

        authFunctions.deleteUser(vaultFunctions.testUsername, vaultFunctions.testEmail, vaultFunctions.testPassword);
        assertTrue(response.ok(), "Delete user in delete user from all services test failed: " + response.status() + " - " + response.text());
    } 
    
    @Test
    public void bootstrapEnvFileAndUserTest() {
        String bootstrapServiceName = "testBootstrapService_" + UUID.randomUUID();
        String bootstrapEnvironmentName = "testBootstrapEnvironment_" + UUID.randomUUID();
        String bootstrapUsername = "testBootstrapUser_" + UUID.randomUUID();
        String bootstrapEmail = bootstrapUsername + "@email.com";
        String bootstrapPassword = UUID.randomUUID().toString();

        APIResponse response = vaultFunctions.bootstrapEnvFile(bootstrapServiceName, bootstrapEnvironmentName, ENV_TEST_FILE);
        assertTrue(response.ok(), "Bootstrap env file test failed: " + response.status() + " - " + response.text());

        String accessToken = authFunctions.registerEnableAuthenticateAccess(bootstrapUsername, bootstrapEmail, bootstrapPassword);
        response = authFunctions.updateRole("DEVELOPER", bootstrapUsername, authFunctions.authenticateInitialSuperUser());
        assertTrue(response.ok(), "Bootstrap user promotion failed: " + response.status() + " - " + response.text());
        authFunctions.authenticate(bootstrapUsername, bootstrapPassword);
        accessToken = authFunctions.extractJwt(authFunctions.serveAccessToken());
        response = vaultFunctions.bootstrapUser(bootstrapServiceName, accessToken);
        assertTrue(response.ok(), "Bootstrap user test failed: " + response.status() + " - " + response.text());

        response = vaultFunctions.deleteService(bootstrapServiceName, accessToken);
        assertTrue(response.ok(), "Bootstrap service cleanup failed: " + response.status() + " - " + response.text());

        response = authFunctions.deleteUser(bootstrapUsername, bootstrapEmail, bootstrapPassword);
        assertTrue(response.ok(), "Bootstrap user cleanup failed: " + response.status() + " - " + response.text());
    }
}
