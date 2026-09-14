package com.example.telemetryservice.repositorytests;

import com.example.telemetryservice.event.TelemetryEvent;
import com.example.telemetryservice.event.TelemetryEventRepository;
import com.example.telemetryservice.event.TelemetryEventRepositoryImpl;
import com.example.telemetryservice.event.MongoIndexInitializer;
import com.example.telemetryservice.telemetry.TelemetryService;
import com.example.telemetryservice.utility.JwtUtility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@DataMongoTest
@ActiveProfiles("test")
@TestPropertySource(properties = "telemetry.retention.days=30")
@Import({TelemetryService.class, MongoIndexInitializer.class})
public class TelemetryEventRepositoryImplTest {
    static {
        System.setProperty("ENCRYPTION_KEY", "AA1A2A8C0E4F76FB3C13F66225AAAC42");
    }

    @Autowired
    TelemetryEventRepository telemetryEventRepository;

    @Autowired
    TelemetryEventRepositoryImpl telemetryEventRepositoryImpl;

    @Autowired
    MongoTemplate mongoTemplate;
    @MockitoBean
    private JwtUtility jwtUtility;

    TelemetryEvent testEvent;
    final String testServiceName = "test-service";
    final String testEventType = "test-event-type";

    @BeforeEach
    void setup() {
        testEvent = new TelemetryEvent(testServiceName, testEventType, LocalDateTime.now());
        telemetryEventRepository.save(testEvent);
    }

    @AfterEach
    void teardown() {
        telemetryEventRepository.deleteAll();
    }

    @Test
    void getEventsByTypeTest() {
        List<TelemetryEvent> eventsList = telemetryEventRepositoryImpl.getEventsByEventType(testEventType, null, null, null);

        Assertions.assertEquals(1, eventsList.size());
        Assertions.assertEquals(testEvent.getId(), eventsList.getFirst().getId());
    }

    @Test
    void countEventsTest() {
        Map<String, Integer> eventsMap = telemetryEventRepositoryImpl.countEvents(testServiceName, testEventType, null, null, null);

        Assertions.assertEquals(1, eventsMap.entrySet().size());
        Assertions.assertEquals(1, eventsMap.get("total"));
    }

    @Test
    void telemetryIndexesAreCreatedTest() {
        List<IndexInfo> indexes = mongoTemplate.indexOps(TelemetryEvent.class).getIndexInfo();

        Assertions.assertTrue(indexes.stream().anyMatch(index ->
                index.getName().equals("event_type_timestamp_index")));
        Assertions.assertTrue(indexes.stream().anyMatch(index ->
                index.getName().equals("service_timestamp_index")));
        Assertions.assertTrue(indexes.stream().anyMatch(index ->
                index.getName().equals("service_event_type_timestamp_index")));

        IndexInfo timestampIndex = indexes.stream()
                .filter(index -> index.getName().equals("timestamp_index"))
                .findFirst()
                .orElseThrow();

        Assertions.assertEquals(Duration.ofDays(30), timestampIndex.getExpireAfter().orElse(null));
    }
}
