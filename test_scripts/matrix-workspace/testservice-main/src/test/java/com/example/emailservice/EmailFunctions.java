

package com.example.emailservice;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import com.example.utility.AuthUtility;
import com.example.authservice.AuthFunctions;
import com.example.utility.EnvUtility;

import java.util.HashMap;
import java.util.Map;

public class EmailFunctions {
    APIRequestContext apiRequestContext;
    AuthUtility authUtility;

    public static final String EMAIL_BASE_URL = EnvUtility.getEnvVar("EMAIL_BASE_URL");

    public EmailFunctions(APIRequestContext apiRequestContext) {
        this.apiRequestContext = apiRequestContext;
        this.authUtility = new AuthUtility(apiRequestContext);
    }

    public APIResponse healthcheck() {
        return apiRequestContext.get(EMAIL_BASE_URL + "/health");
    }

    public APIResponse sendEmail(String recipient, String sender, String body, String subject) {
        return sendEmail(recipient, sender, body, subject, authUtility.authenticateServiceAccount("auth"));
    }

    public APIResponse sendEmail(String recipient, String sender, String body, String subject, String accessToken) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("recipient", recipient);
        requestBody.put("sender", sender);
        requestBody.put("body", body);
        requestBody.put("subject", subject);

        return apiRequestContext.post(EMAIL_BASE_URL + "/send-email", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + accessToken)
        );
    }

    public APIResponse sendEmailWithoutAccess() {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("recipient", "recipient@email.com");
        requestBody.put("sender", "sender@email.com");
        requestBody.put("body", "Test email");
        requestBody.put("subject", "Test subject");

        return apiRequestContext.post(EMAIL_BASE_URL + "/send-email", RequestOptions.create().setData(requestBody));
    }

    public APIResponse getTemplateEmail(String templateName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("templateName", templateName);

        return apiRequestContext.post(EMAIL_BASE_URL + "/get-template-email", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + getInitialSuperAccessToken())
        );
    }

    public APIResponse getAllTemplateEmails() {
        return apiRequestContext.get(EMAIL_BASE_URL + "/get-all-template-emails", RequestOptions.create()
                .setHeader("Authorization", "Bearer " + getInitialSuperAccessToken())
        );
    }

    public APIResponse sendTemplateEmail(String recipient, String sender, boolean includeToken, String templateName,
                                         Map<String, String> templateVariables) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("recipient", recipient);
        requestBody.put("sender", sender);
        requestBody.put("includeToken", includeToken);
        requestBody.put("templateName", templateName);
        requestBody.put("templateVariables", templateVariables);

        return apiRequestContext.post(EMAIL_BASE_URL + "/send-template-email", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount("auth"))
        );
    }

    public APIResponse createTemplateEmail(String body, String subject, String templateName, String tokenPurpose) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("body", body);
        requestBody.put("subject", subject);
        requestBody.put("templateName", templateName);
        requestBody.put("tokenPurpose", tokenPurpose);

        return apiRequestContext.post(EMAIL_BASE_URL + "/create-template-email", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + getInitialSuperAccessToken())
        );
    }

    public APIResponse updateTemplateEmail(String body, String subject, String templateName) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("body", body);
        requestBody.put("subject", subject);
        requestBody.put("templateName", templateName);

        return apiRequestContext.put(EMAIL_BASE_URL + "/update-template-email", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + getInitialSuperAccessToken())
        );
    }

    public APIResponse deleteTemplateEmail(String templateName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("templateName", templateName);

        return apiRequestContext.delete(EMAIL_BASE_URL + "/delete-template-email", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + getInitialSuperAccessToken())
        );
    }

    public APIResponse sendEmailWithToken(String recipient, String sender, String body, String subject) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("recipient", recipient);
        requestBody.put("sender", sender);
        requestBody.put("body", body);
        requestBody.put("subject", subject);
        requestBody.put("includeToken", true);

        return apiRequestContext.post(EMAIL_BASE_URL + "/send-email", RequestOptions.create().setData(requestBody)
                .setHeader("Authorization", "Bearer " + authUtility.authenticateServiceAccount("auth"))
        );
    }

    private String getInitialSuperAccessToken() {
        return new AuthFunctions(apiRequestContext).authenticateInitialSuperUser();
    }

}
