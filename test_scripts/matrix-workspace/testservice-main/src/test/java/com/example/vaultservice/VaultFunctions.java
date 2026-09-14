

package com.example.vaultservice;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.FormData;
import com.microsoft.playwright.options.RequestOptions;
import com.example.authservice.AuthFunctions;
import com.example.utility.AuthUtility;
import com.example.utility.EnvUtility;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VaultFunctions {
    APIRequestContext apiRequestContext;
    AuthFunctions authFunctions;
    AuthUtility authUtility;

    public String testUsername;
    public String testEmail;
    public String testPassword;

    public static final String VAULT_BASE_URL = EnvUtility.getEnvVar("VAULT_BASE_URL");
    public static final String BOOTSTRAP_TOKEN = EnvUtility.getEnvVar("BOOTSTRAP_TOKEN");

    public VaultFunctions(APIRequestContext apiRequestContext) {
        this.apiRequestContext = apiRequestContext;
    }

    public VaultFunctions(APIRequestContext apiRequestContext, AuthFunctions authFunctions) {
        this(apiRequestContext);
        this.authFunctions = authFunctions;
        this.authUtility = new AuthUtility(apiRequestContext);
    }

    public APIResponse healthcheck() {
        return apiRequestContext.get(VAULT_BASE_URL + "/health");
    }

    public APIResponse createService(String testServiceName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("serviceName", testServiceName);
        return apiRequestContext.post(VAULT_BASE_URL + "/create-service", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse createEnvironment(String testServiceName, String testEnvironmentName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("serviceName", testServiceName);
        requestBody.put("environmentName", testEnvironmentName);
        return apiRequestContext.post(VAULT_BASE_URL + "/create-environment", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse getService(String testServiceName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("serviceName", testServiceName);
        return apiRequestContext.post(VAULT_BASE_URL + "/get-service", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse getEnvironmentsByService(String testServiceName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("serviceName", testServiceName);
        return apiRequestContext.post(VAULT_BASE_URL + "/get-environments-by-service", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse getEnvironment(String testServiceName, String testEnvironmentName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("serviceName", testServiceName);
        requestBody.put("environmentName", testEnvironmentName);
        return apiRequestContext.post(VAULT_BASE_URL + "/get-environment", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse getVariablesByService(String testServiceName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("serviceName", testServiceName);
        return apiRequestContext.post(VAULT_BASE_URL + "/get-variables-by-service", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse getVariablesByEnvironment(String testServiceName, String testEnvironmentName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("serviceName", testServiceName);
        requestBody.put("environmentName", testEnvironmentName);
        return apiRequestContext.post(VAULT_BASE_URL + "/get-variables-by-environment", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse getVariableByName(String testServiceName, String testEnvironmentName, String testVariableName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("serviceName", testServiceName);
        requestBody.put("environmentName", testEnvironmentName);
        requestBody.put("variableName", testVariableName);
        return apiRequestContext.post(VAULT_BASE_URL + "/get-variable-by-name", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse addVariable(String testServiceName, String testEnvironmentName, String key, String value) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("serviceName", testServiceName);
        requestBody.put("environmentName", testEnvironmentName);
        requestBody.put("variable", Map.of("key", key, "value", value));

        return apiRequestContext.post(VAULT_BASE_URL + "/add-variable", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse updateVariable(String testServiceName, String testEnvironmentName, String key, String value) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("serviceName", testServiceName);
        requestBody.put("environmentName", testEnvironmentName);
        requestBody.put("variable", Map.of("key", key, "value", value));

        return apiRequestContext.post(VAULT_BASE_URL + "/update-variable", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse updateVariables(String testServiceName, String testEnvironmentName, String key, String value) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("key", key);
        requestBody.put("value", value);

        List<Map<String, String>> requestList = new ArrayList<>();
        requestList.add(requestBody);
        Map<String, Object> request = new HashMap<>();
        request.put("serviceName", testServiceName);
        request.put("environmentName", testEnvironmentName);
        request.put("variables", requestList);

        return apiRequestContext.post(VAULT_BASE_URL + "/update-variables", RequestOptions.create().setData(request)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse addEnvFile(String testServiceName, String testEnvironmentName, String uploadFileName) {
        try {
            Path filePath = Paths.get(getClass().getClassLoader().getResource("vaultserviceTestFiles/" + uploadFileName).toURI());
            FormData formData = FormData.create().set("file", filePath).set("serviceName", testServiceName).set("environmentName", testEnvironmentName);

            return apiRequestContext.post(VAULT_BASE_URL + "/add-env-file", RequestOptions.create().setMultipart(formData)
                    .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
            );
        } catch (URISyntaxException ex) {
            throw new RuntimeException("Failed to load test file from resources", ex);
        }
    }

    public APIResponse bootstrapEnvFile(String testServiceName, String testEnvironmentName, String uploadFileName) {
        try {
            Path filePath = Paths.get(getClass().getClassLoader().getResource("vaultserviceTestFiles/" + uploadFileName).toURI());
            FormData formData = FormData.create().set("file", filePath).set("serviceName", testServiceName).set("environmentName", testEnvironmentName);

            return apiRequestContext.post(VAULT_BASE_URL + "/bootstrap/add-env", RequestOptions.create()
                    .setMultipart(formData)
                    .setHeader("X-VAULT-BOOTSTRAP-TOKEN", BOOTSTRAP_TOKEN)
            );
        } catch (URISyntaxException ex) {
            throw new RuntimeException("Failed to load test file from resources", ex);
        }
    }

    public APIResponse bootstrapEnvFileWithoutToken(String testServiceName, String testEnvironmentName, String uploadFileName) {
        try {
            Path filePath = Paths.get(getClass().getClassLoader().getResource("vaultserviceTestFiles/" + uploadFileName).toURI());
            FormData formData = FormData.create().set("file", filePath).set("serviceName", testServiceName).set("environmentName", testEnvironmentName);

            return apiRequestContext.post(VAULT_BASE_URL + "/bootstrap/add-env", RequestOptions.create().setMultipart(formData));
        } catch (URISyntaxException ex) {
            throw new RuntimeException("Failed to load test file from resources", ex);
        }
    }

    public APIResponse downloadEnvFile(String testServiceName, String testEnvironmentName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("serviceName", testServiceName);
        requestBody.put("environmentName", testEnvironmentName);
        return apiRequestContext.post(VAULT_BASE_URL + "/download-env-file", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse deleteVariable(String testServiceName, String testEnvironmentName, String testVariableName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("serviceName", testServiceName);
        requestBody.put("environmentName", testEnvironmentName);
        requestBody.put("variableName", testVariableName);
        return apiRequestContext.delete(VAULT_BASE_URL + "/delete-variable", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse deleteEnvironment(String testServiceName, String testEnvironmentName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("serviceName", testServiceName);
        requestBody.put("environmentName", testEnvironmentName);
        return apiRequestContext.delete(VAULT_BASE_URL + "/delete-environment", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse deleteService(String testServiceName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("serviceName", testServiceName);
        return apiRequestContext.delete(VAULT_BASE_URL + "/delete-service", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse deleteService(String testServiceName, String accessToken) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("serviceName", testServiceName);
        return apiRequestContext.delete(VAULT_BASE_URL + "/delete-service", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + accessToken)
        );
    }

    public APIResponse getAllServices() {
        return getAllServices(authUtility.authenticateServiceAccount());
    }

    public APIResponse getAllServices(String accessToken) {
        return apiRequestContext.get(VAULT_BASE_URL + "/get-all-services", RequestOptions.create()
                .setHeader("Authorization", "Bearer " + accessToken)
        );
    }

    public APIResponse getAllServicesWithoutAccess() {
        return apiRequestContext.get(VAULT_BASE_URL + "/get-all-services");
    }

    public APIResponse bootstrapUser(String testServiceName, String accessToken) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("serviceName", testServiceName);
        return apiRequestContext.post(VAULT_BASE_URL + "/bootstrap/bootstrap-user", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + accessToken)
                .setHeader("X-VAULT-BOOTSTRAP-TOKEN", BOOTSTRAP_TOKEN)
        );
    }

    public APIResponse getUsersByService(String testServiceName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("serviceName", testServiceName);
        return apiRequestContext.post(VAULT_BASE_URL + "/get-users-by-service", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse getUsersByService(String testServiceName, String accessToken) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("serviceName", testServiceName);
        return apiRequestContext.post(VAULT_BASE_URL + "/get-users-by-service", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + accessToken)
        );
    }

    public APIResponse getUsersByServiceWithoutAccess(String testServiceName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("serviceName", testServiceName);
        return apiRequestContext.post(VAULT_BASE_URL + "/get-users-by-service", RequestOptions.create().setData(requestBody));
    }

    public APIResponse getAllRoles() {
        return apiRequestContext.get(VAULT_BASE_URL + "/get-all-roles", RequestOptions.create()
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse getCurrentUserRole(String testServiceName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("serviceName", testServiceName);
        return apiRequestContext.post(VAULT_BASE_URL + "/get-current-user-role", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse updateUserRole(String testServiceName, String newRole) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("serviceName", testServiceName);
        requestBody.put("username", testUsername);
        requestBody.put("role", newRole);

        return apiRequestContext.post(VAULT_BASE_URL + "/update-user-role", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse addUserToService(String testServiceName) {
        return addUserToService(testServiceName, testUsername, "MAINTAINER", authUtility.authenticateServiceAccount());
    }

    public APIResponse addUserToService(String testServiceName, String username, String role, String accessToken) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("serviceName", testServiceName);
        requestBody.put("username", username);
        requestBody.put("role", role);

        return apiRequestContext.post(VAULT_BASE_URL + "/add-user-to-service", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + accessToken)
        );
    }

    public APIResponse deleteUserFromService(String testServiceName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("serviceName", testServiceName);
        requestBody.put("username", testUsername);

        return apiRequestContext.post(VAULT_BASE_URL + "/delete-user-from-service", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse deleteUserFromAllServices() {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("username", testUsername);

        return apiRequestContext.post(VAULT_BASE_URL + "/delete-user-from-all-services", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }
}
