

package com.example.telemetryservice;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import com.example.utility.EnvUtility;

import java.util.HashMap;
import java.util.Map;

public class TelemetryFunctions {
    public static final String TELEMETRY_BASE_URL = EnvUtility.getEnvVar("TELEMETRY_BASE_URL");

    private final APIRequestContext apiRequestContext;

    public TelemetryFunctions(APIRequestContext apiRequestContext) {
        this.apiRequestContext = apiRequestContext;
    }

    public APIResponse healthcheck() {
        return apiRequestContext.get(TELEMETRY_BASE_URL + "/health");
    }

    public APIResponse getEventsWithoutAccess() {
        return apiRequestContext.get(TELEMETRY_BASE_URL + "/get-events?eventType=test");
    }

    public APIResponse getEvents(String accessToken) {
        return getEvents("test", accessToken);
    }

    public APIResponse getEvents(String eventType, String accessToken) {
        return apiRequestContext.get(TELEMETRY_BASE_URL + "/get-events?eventType=" + eventType, RequestOptions.create()
                .setHeader("Authorization", "Bearer " + accessToken)
        );
    }

    public APIResponse createEvent(String serviceName, String eventType, String id, String timestamp, String accessToken) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("serviceName", serviceName);
        requestBody.put("eventType", eventType);
        requestBody.put("id", id);
        requestBody.put("timestamp", timestamp);

        return apiRequestContext.post(TELEMETRY_BASE_URL + "/create-event", RequestOptions.create()
                .setData(requestBody)
                .setHeader("Authorization", "Bearer " + accessToken)
        );
    }
}
