package com.example.emailservice.utility;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthUtility {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthUtility.class);

    @Value("${SERVICE_SECRET_EMAIL}")
    private String SERVICE_SECRET_EMAIL;
    @Value("${authservice.integration}")
    private boolean authserviceIntegration;

    @Autowired
    private RestTemplate restTemplate;

    public String authenticateServiceAccount() {
        if(!authserviceIntegration)
            return null;

        try {
            LOGGER.debug("Attempting to authenticate service account");

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("clientId", "email");
            requestBody.put("clientPassword", SERVICE_SECRET_EMAIL);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

            String response = restTemplate.postForObject(
                    "http://auth-service:6001/api/auth/service-account/authenticate",
                    requestEntity,
                    String.class
            );

            response = response.replace("\"", "");
            response = response.replace("}", "");

            if(!response.contains("jwtToken"))
                throw new RuntimeException("jwtToken not found in authentication response");

            LOGGER.debug("Service account authentication success");
            return response.substring(response.indexOf("jwtToken:") + 9).trim();
        } catch (RestClientException ex) {
            LOGGER.error("Failed to authenticate service account: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return null;
        }
    }

    public void enableUser(String email) {
        if(!authserviceIntegration)
            return;

        LOGGER.debug("Attempting to enable user");

        try {
            String accessToken = authenticateServiceAccount();

            //Set the headers
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            //Create the request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("email", email);

            //Compile the HttpEntity
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);

            String url = "http://auth-service:6001/api/auth/user/enable-user";

            restTemplate.postForObject(url, requestEntity, String.class);
        } catch (JSONException ex) {
            LOGGER.error("Failed to enable user in auth utility: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
        }
    }

    public void resetPassword(String email, String newPassword) {
        if(!authserviceIntegration)
            return;

        LOGGER.debug("Attempting to reset user password");

        try {
            String accessToken = authenticateServiceAccount();

            //Set the headers
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            //Create the request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("email", email);
            requestBody.put("newPassword", newPassword);

            //Compile the HttpEntity
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);

            String url = "http://auth-service:6001/api/auth/user/reset-password";

            restTemplate.postForObject(url, requestEntity, String.class);
        } catch (JSONException ex) {
            LOGGER.error("Failed to reset user password in auth utility: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
        }
    }
}
