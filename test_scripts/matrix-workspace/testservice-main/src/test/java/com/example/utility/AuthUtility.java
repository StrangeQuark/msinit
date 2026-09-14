

package com.example.utility;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import com.example.authservice.AuthFunctions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class AuthUtility {
    private final APIRequestContext apiRequestContext;
    private final String SERVICE_SECRET_TEST;
    private final String SERVICE_SECRET_AUTH;
    private final String SERVICE_SECRET_EMAIL;
    private final String INITIAL_SUPER_USERNAME;
    private final String INITIAL_SUPER_PASSWORD;

    public AuthUtility(APIRequestContext apiRequestContext) {
        this.apiRequestContext = apiRequestContext;

        // Attempt to get the TestService service secret for auth integrations
        SERVICE_SECRET_TEST = EnvUtility.getEnvVar("SERVICE_SECRET_TEST");

        SERVICE_SECRET_AUTH = EnvUtility.getEnvVar("SERVICE_SECRET_AUTH");

        SERVICE_SECRET_EMAIL = EnvUtility.getEnvVar("SERVICE_SECRET_EMAIL");

        try {
            String[] credentials = Files.readString(Path.of(EnvUtility.getEnvVar("INITIAL_SUPER_CREDENTIALS_FILE")))
                    .split("\\n");
            INITIAL_SUPER_USERNAME = credentials[0].replace("Username: ", "");
            INITIAL_SUPER_PASSWORD = credentials[1].replace("Password: ", "");
        } catch (Exception ex) {
            throw new RuntimeException("Unable to read initial SUPER user credentials", ex);
        }
    }

    public String authenticateServiceAccount() {
        return authenticateServiceAccount("test");
    }

    public String authenticateServiceAccount(String clientId) {
        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("clientId", clientId);
            requestBody.put("clientPassword", getServiceSecret(clientId));

            APIResponse response = apiRequestContext.post(AuthFunctions.AUTH_BASE_URL + "/service-account/authenticate",
                    RequestOptions.create().setData(requestBody));

            String res = response.text().replace("\"", "");
            res = res.replace("}", "");

            if(!res.contains("jwtToken"))
                throw new RuntimeException("jwtToken not found in authentication response");

            return res.substring(res.indexOf("jwtToken:") + 9).trim();
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    public String getInitialSuperUsername() {
        return INITIAL_SUPER_USERNAME;
    }

    public String getInitialSuperPassword() {
        return INITIAL_SUPER_PASSWORD;
    }

    public String getServiceSecret(String clientId) {
        if(clientId.equals("auth"))
            return SERVICE_SECRET_AUTH;

        if(clientId.equals("email"))
            return SERVICE_SECRET_EMAIL;

        return SERVICE_SECRET_TEST;
    }
}
