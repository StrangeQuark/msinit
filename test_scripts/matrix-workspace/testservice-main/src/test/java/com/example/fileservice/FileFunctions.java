

package com.example.fileservice;

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
import java.util.HashMap;
import java.util.Map;

public class FileFunctions {
    APIRequestContext apiRequestContext;
    AuthFunctions authFunctions;
    AuthUtility authUtility;

    public String testUsername;
    public String testEmail;
    public String testPassword;

    public static final String FILE_BASE_URL = EnvUtility.getEnvVar("FILE_BASE_URL");
    public static final String GATEWAY_BASE_URL = EnvUtility.getEnvVar("GATEWAY_BASE_URL");

    public FileFunctions(APIRequestContext apiRequestContext) {
        this.apiRequestContext = apiRequestContext;
    }

    public FileFunctions(APIRequestContext apiRequestContext, AuthFunctions authFunctions) {
        this(apiRequestContext);
        this.authFunctions = authFunctions;
        this.authUtility = new AuthUtility(apiRequestContext);
    }

    public APIResponse healthcheck() {
        return apiRequestContext.get(FILE_BASE_URL + "/health");
    }

    public APIResponse createCollection(String testCollectionName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("collectionName", testCollectionName);

        return apiRequestContext.post(FILE_BASE_URL + "/new-collection", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse getAllCollections() {
        return getAllCollections(authUtility.authenticateServiceAccount());
    }

    public APIResponse getAllCollections(String accessToken) {
        return apiRequestContext.get(FILE_BASE_URL + "/get-all-collections", RequestOptions.create()
                .setHeader("Authorization", "Bearer " + accessToken)
        );
    }

    public APIResponse getAllCollectionsWithoutAccess() {
        return apiRequestContext.get(FILE_BASE_URL + "/get-all-collections");
    }

    public APIResponse deleteCollection(String testCollectionName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("collectionName", testCollectionName);

        return apiRequestContext.delete(FILE_BASE_URL + "/delete-collection", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse upload(String testCollectionName, String uploadFileName) {
        try {
            Path filePath = Paths.get(getClass().getClassLoader().getResource("fileserviceTestFiles/" + uploadFileName).toURI());
            FormData formData = FormData.create().set("file", filePath).set("collectionName", testCollectionName);

            return apiRequestContext.post(FILE_BASE_URL + "/upload", RequestOptions.create().setMultipart(formData)
                    .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
            );
        } catch (URISyntaxException ex) {
            throw new RuntimeException("Failed to load test file from resources", ex);
        }
    }

    public APIResponse delete(String testCollectionName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("collectionName", testCollectionName);
        requestBody.put("fileName", "testUploadFile.txt");

        return apiRequestContext.delete(FILE_BASE_URL + "/delete", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse getAllFiles(String testCollectionName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("collectionName", testCollectionName);

        return apiRequestContext.post(FILE_BASE_URL + "/get-all", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse downloadFile(String testCollectionName, String fileName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("collectionName", testCollectionName);
        requestBody.put("fileName", fileName);

        return apiRequestContext.post(FILE_BASE_URL + "/download", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse streamFile(String testCollectionName, String fileName, String range) {
        return apiRequestContext.get(FILE_BASE_URL + "/stream?collectionName=" + testCollectionName + "&fileName=" + fileName, RequestOptions.create()
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
                .setHeader("Range", range)
        );
    }

    public APIResponse streamFileThroughGateway(String testCollectionName, String fileName, String range) {
        return apiRequestContext.get(GATEWAY_BASE_URL + "/api/file/stream?collectionName=" + testCollectionName + "&fileName=" + fileName, RequestOptions.create()
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
                .setHeader("Origin", "http://localhost:6080")
                .setHeader("Range", range)
        );
    }

    public APIResponse preflightThroughGateway(String testCollectionName, String fileName, String origin) {
        return apiRequestContext.fetch(GATEWAY_BASE_URL + "/api/file/stream?collectionName=" + testCollectionName + "&fileName=" + fileName, RequestOptions.create()
                .setMethod("OPTIONS")
                .setHeader("Origin", origin)
                .setHeader("Access-Control-Request-Method", "GET")
                .setHeader("Access-Control-Request-Headers", "authorization,range")
        );
    }


    public APIResponse getCurrentUserRole(String testCollectionName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("collectionName", testCollectionName);

        return apiRequestContext.post(FILE_BASE_URL + "/get-current-user-role", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse getUsersByCollection(String testCollectionName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("collectionName", testCollectionName);

        return apiRequestContext.post(FILE_BASE_URL + "/get-users-by-collection", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse getAllRoles() {
        return apiRequestContext.get(FILE_BASE_URL + "/get-all-roles", RequestOptions.create()
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse addUserToCollection(String testCollectionName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("collectionName", testCollectionName);
        requestBody.put("username", testUsername);
        requestBody.put("role", "READ_WRITE");

        return apiRequestContext.post(FILE_BASE_URL + "/add-user-to-collection", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse updateUserRole(String testCollectionName, String newRole) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("collectionName", testCollectionName);
        requestBody.put("username", testUsername);
        requestBody.put("role", newRole);

        return apiRequestContext.post(FILE_BASE_URL + "/update-user-role", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse deleteUserFromCollection(String testCollectionName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("collectionName", testCollectionName);
        requestBody.put("username", testUsername);

        return apiRequestContext.post(FILE_BASE_URL + "/delete-user-from-collection", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }

    public APIResponse deleteUserFromAllCollections() {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("username", testUsername);

        return apiRequestContext.post(FILE_BASE_URL + "/delete-user-from-all-collections", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount())
        );
    }
}
