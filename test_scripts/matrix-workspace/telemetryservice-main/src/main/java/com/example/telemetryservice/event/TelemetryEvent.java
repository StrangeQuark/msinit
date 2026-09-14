package com.example.telemetryservice.event;

import com.example.telemetryservice.utility.EncryptionUtility;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Document(collection = "telemetry_events")
public class TelemetryEvent {
    @Id
    private UUID id = UUID.randomUUID();

    @NotNull
    private String serviceName;

    @NotNull
    private String eventType;

    @NotNull
    private LocalDateTime timestamp;

    private Map<String, Object> metadata;

    public TelemetryEvent() {

    }

    public TelemetryEvent(String serviceName, String eventType, LocalDateTime timestamp) {
        this.serviceName = serviceName;
        this.eventType = eventType;
        this.timestamp = timestamp;
    }

    public TelemetryEvent(String serviceName, String eventType, LocalDateTime timestamp, Map<String, Object> metadata) {
        this(serviceName, eventType, timestamp);
        this.metadata = metadata;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getMetadata() {
        if (metadata == null)
            return null;

        Map<String, Object> decrypted = new HashMap<>();
        for (var entry : metadata.entrySet()) {
            decrypted.put(EncryptionUtility.decrypt(entry.getKey()), EncryptionUtility.decrypt(entry.getValue().toString()));
        }

        return decrypted;
    }

    public void setMetadata(Map<String, Object> metadata) {
        if (metadata == null) {
            this.metadata = null;
            return;
        }

        Map<String, Object> encrypted = new HashMap<>();
        for (var entry : metadata.entrySet()) {
            encrypted.put(EncryptionUtility.encrypt(entry.getKey()), EncryptionUtility.encrypt(entry.getValue().toString()));
        }

        this.metadata = encrypted;
    }
}
