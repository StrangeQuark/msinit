

package com.example.authservice;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import com.example.utility.AuthUtility;
import com.example.utility.EnvUtility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthFunctions {
    public static final String AUTH_BASE_URL = EnvUtility.getEnvVar("AUTH_BASE_URL");

    private final APIRequestContext apiRequestContext;
    private final AuthUtility authUtility;

    public AuthFunctions(APIRequestContext apiRequestContext) {
        this.apiRequestContext = apiRequestContext;
        this.authUtility = new AuthUtility(apiRequestContext);
    }

    public APIResponse healthcheck() {
        return apiRequestContext.get(AUTH_BASE_URL + "/health");
    }

    public APIResponse register(String username, String email, String password) {
        String accessToken = authenticateInitialSuperUser();
        String inviteToken = createInvitationToken(email, accessToken);
        return register(username, email, password, inviteToken);
    }

    public APIResponse registerWithoutInvitation(String username, String email, String password) {
        return register(username, email, password, null);
    }

    public APIResponse register(String username, String email, String password, String inviteToken) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("username", username);
        requestBody.put("email", email);
        requestBody.put("password", password);
        if(inviteToken != null)
            requestBody.put("inviteToken", inviteToken);

        return apiRequestContext.post(AUTH_BASE_URL + "/register", RequestOptions.create().setData(requestBody));
    }

    public APIResponse getInviteOnly() {
        return apiRequestContext.get(AUTH_BASE_URL + "/invitation/invite-only");
    }

    public APIResponse createInvitation(String email, String accessToken) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", email);

        return apiRequestContext.post(AUTH_BASE_URL + "/invitation/create", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + accessToken));
    }

    public APIResponse getAllInvitations(String accessToken) {
        return apiRequestContext.get(AUTH_BASE_URL + "/invitation/get-all", RequestOptions.create()
                .setHeader("Authorization", "Bearer " + accessToken));
    }

    public APIResponse deleteInvitation(String id, String accessToken) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("id", id);

        return apiRequestContext.delete(AUTH_BASE_URL + "/invitation/delete", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + accessToken));
    }

    public APIResponse deleteAllInvitations(String accessToken) {
        return apiRequestContext.delete(AUTH_BASE_URL + "/invitation/delete-all", RequestOptions.create()
                .setHeader("Authorization", "Bearer " + accessToken));
    }

    public String createInvitationToken(String email, String accessToken) {
        APIResponse response = createInvitation(email, accessToken);
        assertTrue(response.ok(), "Invitation creation failed: " + response.status() + " - " + response.text());

        JsonObject jsonObject = JsonParser.parseString(response.text()).getAsJsonObject();
        return jsonObject.get("token").getAsString();
    }

    public APIResponse enableUser(String email) {
        return enableUser(email, "email");
    }

    public APIResponse enableUser(String email, String clientId) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", email);

        return apiRequestContext.post(AUTH_BASE_URL + "/user/enable-user", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount(clientId)));
    }

    public APIResponse enableUserWithAccessToken(String email, String accessToken) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", email);

        return apiRequestContext.post(AUTH_BASE_URL + "/user/enable-user", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + accessToken));
    }

    public APIResponse disableUser(String username, String accessToken) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("username", username);

        return apiRequestContext.post(AUTH_BASE_URL + "/user/disable-user", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + accessToken));
    }

    public APIResponse authenticate(String username, String password) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("username", username);
        requestBody.put("password", password);

        return apiRequestContext.post(AUTH_BASE_URL + "/authenticate", RequestOptions.create().setData(requestBody));
    }

    public APIResponse serveAccessToken() {
        return apiRequestContext.post(AUTH_BASE_URL + "/access");
    }

    public APIResponse logout() {
        return apiRequestContext.post(AUTH_BASE_URL + "/access/logout");
    }

    public APIResponse getUserId(String username, String accessToken) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("username", username);

        return apiRequestContext.post(AUTH_BASE_URL + "/user/get-user-id", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + accessToken));
    }

    public APIResponse searchUsers(String query, String accessToken) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("query", query);

        return apiRequestContext.post(AUTH_BASE_URL + "/user/search-users", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + accessToken));
    }

    public APIResponse getAdminUser(String query, String accessToken) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("query", query);

        return apiRequestContext.post(AUTH_BASE_URL + "/user/get-admin-user", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + accessToken));
    }

    public APIResponse getUserDetailsByIds(List<String> ids, String accessToken) {
        return apiRequestContext.post(AUTH_BASE_URL + "/user/get-user-details-by-ids", RequestOptions.create()
                .setData(ids)
                .setHeader("Authorization", "Bearer " + accessToken));
    }

    public APIResponse deleteUser(String username, String email, String password) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("username", username);
        requestBody.put("password", password);

        enableUser(email);
        authenticate(username, password);
        String accessToken = extractJwt(serveAccessToken());

        return apiRequestContext.post(AUTH_BASE_URL + "/user/delete-user", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + accessToken));
    }

    public APIResponse updatePassword(String password, String newPassword, String accessToken) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("password", password);
        requestBody.put("newPassword", newPassword);

        return apiRequestContext.post(AUTH_BASE_URL + "/user/update-password", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + accessToken));
    }

    public APIResponse updateEmail(String newEmail, String password, String accessToken) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("newEmail", newEmail);
        requestBody.put("password", password);

        return apiRequestContext.post(AUTH_BASE_URL + "/user/update-email", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + accessToken));
    }

    public APIResponse updateUsername(String newUsername, String password, String accessToken) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("newUsername", newUsername);
        requestBody.put("password", password);

        return apiRequestContext.post(AUTH_BASE_URL + "/user/update-username", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + accessToken));
    }

    public APIResponse updateRole(String role, String username, String accessToken) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("newRole", role);
        requestBody.put("username", username);

        return apiRequestContext.post(AUTH_BASE_URL + "/user/update-role", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + accessToken));
    }

    public APIResponse sendPasswordResetEmail(String email, String accessToken) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", email);

        return apiRequestContext.post(AUTH_BASE_URL + "/user/send-password-reset-email", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + accessToken));
    }

    public String authenticateInitialSuperUser() {
        APIResponse response = authenticate(authUtility.getInitialSuperUsername(),
                authUtility.getInitialSuperPassword());

        response = serveAccessToken();

        return extractJwt(response);
    }

    public APIResponse serviceAccountAuthenticate(String clientId) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("clientId", clientId);
        requestBody.put("clientPassword", authUtility.getServiceSecret(clientId));

        return apiRequestContext.post(AUTH_BASE_URL + "/service-account/authenticate", RequestOptions.create().setData(requestBody));
    }

    public APIResponse addAuthorizationsToUser(String username, List<String> auths,  String accessToken) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", username);
        requestBody.put("authorizations", auths);

        return apiRequestContext.post(AUTH_BASE_URL + "/user/add-authorizations-to-user", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + accessToken));
    }

    public APIResponse removeAuthorizations(String username, List<String> auths, String accessToken) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", username);
        requestBody.put("authorizations", auths);

        return apiRequestContext.post(AUTH_BASE_URL + "/user/remove-authorizations", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + accessToken));
    }

    public APIResponse getAuthorizations(String accessToken) {
        return apiRequestContext.get(AUTH_BASE_URL + "/authorization/get-all", RequestOptions.create()
                .setHeader("Authorization", "Bearer " + accessToken));
    }

    public APIResponse addRoleAuthorization(String role, String authorization, String accessToken) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("role", role);
        requestBody.put("authorization", authorization);

        return apiRequestContext.post(AUTH_BASE_URL + "/role-authorization/add", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + accessToken));
    }

    public APIResponse getRoleAuthorizations(String role, String accessToken) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("role", role);

        return apiRequestContext.post(AUTH_BASE_URL + "/role-authorization/get", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + accessToken));
    }

    public APIResponse getAllRoles(String accessToken) {
        return apiRequestContext.get(AUTH_BASE_URL + "/role-authorization/get-all-roles", RequestOptions.create()
                .setHeader("Authorization", "Bearer " + accessToken));
    }

    public APIResponse removeRoleAuthorization(String role, String authorization, String accessToken) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("role", role);
        requestBody.put("authorization", authorization);

        return apiRequestContext.delete(AUTH_BASE_URL + "/role-authorization/remove", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + accessToken));
    }

    public String registerEnableAuthenticateAccess(String username, String email, String password) {
        APIResponse response = register(username, email, password);
        assertTrue(response.ok(), "Registration failed: " + response.status() + " - " + response.text());

        response = enableUser(email);
        assertTrue(response.ok(), "Enablement failed: " + response.status() + " - " + response.text());

        response = authenticate(username, password);
        assertTrue(response.ok(), "Authentication failed: " + response.status() + " - " + response.text());

        response = serveAccessToken();
        assertTrue(response.ok(), "Access token retrieval failed: " + response.status() + " - " + response.text());

        return extractJwt(response);
    }

    public String extractJwt(APIResponse response) {
        if(response.ok()) {
            JsonObject jsonObject = JsonParser.parseString(response.text()).getAsJsonObject();
            return jsonObject.get("jwtToken").getAsString();
        } else {
            throw new RuntimeException("Unable to extract JWT from failed response: " + response.status());
        }
    }
}
