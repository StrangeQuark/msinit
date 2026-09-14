package com.example.telemetryservice.event;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

@Component
public class MongoIndexInitializer {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Value("${telemetry.retention.days}")
    private long retentionDays;

    @PostConstruct
    public void createIndexes() {
        if(retentionDays < 0)
            throw new RuntimeException("Telemetry retention days must not be negative");

        IndexOperations indexOperations = mongoTemplate.indexOps(TelemetryEvent.class);

        createTimestampIndex(indexOperations);

        indexOperations.ensureIndex(new Index()
                .on("eventType", Sort.Direction.ASC)
                .on("timestamp", Sort.Direction.DESC)
                .named("event_type_timestamp_index"));

        indexOperations.ensureIndex(new Index()
                .on("serviceName", Sort.Direction.ASC)
                .on("timestamp", Sort.Direction.ASC)
                .named("service_timestamp_index"));

        indexOperations.ensureIndex(new Index()
                .on("serviceName", Sort.Direction.ASC)
                .on("eventType", Sort.Direction.ASC)
                .on("timestamp", Sort.Direction.ASC)
                .named("service_event_type_timestamp_index"));
    }

    private void createTimestampIndex(IndexOperations indexOperations) {
        Duration retention = retentionDays == 0 ? null : Duration.ofDays(retentionDays);

        IndexInfo indexInfo = indexOperations.getIndexInfo().stream()
                .filter(index -> index.getName().equals("timestamp_index"))
                .findFirst()
                .orElse(null);

        if(indexInfo != null && !Objects.equals(indexInfo.getExpireAfter().orElse(null), retention))
            indexOperations.dropIndex("timestamp_index");

        Index index = new Index()
                .on("timestamp", Sort.Direction.ASC)
                .named("timestamp_index");

        if(retention != null)
            index.expire(retention);

        indexOperations.ensureIndex(index);
    }
}
