package com.example.telemetryservice.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface TelemetryEventRepositoryWrapper {
    List<TelemetryEvent> getEventsByEventType(String eventType, String numberOfEvents, LocalDateTime start, LocalDateTime end);
    Map<String, Integer> countEvents(String serviceName, String eventType, String interval, LocalDateTime start, LocalDateTime end);
}
