package com.example.telemetryservice.servicetests;

import com.example.telemetryservice.event.TelemetryEvent;
import com.example.telemetryservice.event.TelemetryEventRepository;
import com.example.telemetryservice.telemetry.TelemetryService;
import com.example.telemetryservice.utility.JwtUtility;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;

@DataMongoTest
@ActiveProfiles("test")
@Import(TelemetryService.class)
public class TelemetryServiceTest {

    static {
        System.setProperty("ENCRYPTION_KEY", "AA1A2A8C0E4F76FB3C13F66225AAAC42");
    }

    @Autowired
    TelemetryEventRepository telemetryEventRepository;

    @Autowired
    TelemetryService telemetryService;
    @MockitoBean
    JwtUtility jwtUtility;

    TelemetryEvent testEvent;
    final String testServiceName = "test-service";
    final String testEventType = "test-event-type";

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(telemetryService, "authserviceIntegration", true);
        testEvent = new TelemetryEvent(testServiceName, testEventType, LocalDateTime.now());
        telemetryEventRepository.save(testEvent);
    }

    @AfterEach
    void teardown() {
        telemetryEventRepository.deleteAll();
    }

    @Test
    void createEventTest() {
        TelemetryEvent telemetryEvent =
                new TelemetryEvent("spoofed-service", "createTestEvent", LocalDateTime.now().minusYears(1));
        UUID suppliedId = telemetryEvent.getId();

        when(jwtUtility.getServiceName()).thenReturn(testServiceName);
        ResponseEntity<?> response = telemetryService.createEvent(telemetryEvent);

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertTrue(telemetryEventRepository.existsById(telemetryEvent.getId()));
        Assertions.assertNotEquals(suppliedId, telemetryEvent.getId());
        Assertions.assertEquals(testServiceName, telemetryEvent.getServiceName());
        Assertions.assertTrue(telemetryEvent.getTimestamp().isAfter(LocalDateTime.now().minusMinutes(1)));
    }

    @Test
    void createEventWithoutAuthServiceTest() {
        TelemetryEvent telemetryEvent = new TelemetryEvent(testServiceName, "createTestEvent", LocalDateTime.now());

        ReflectionTestUtils.setField(telemetryService, "authserviceIntegration", false);
        ResponseEntity<?> response = telemetryService.createEvent(telemetryEvent);

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertEquals(testServiceName, telemetryEvent.getServiceName());
    }

    @Test
    void getEventsTest() {
        ResponseEntity<?> response = telemetryService.getEvents(testEventType, null, null, null);

        Assertions.assertEquals(200, response.getStatusCode().value());

        List<TelemetryEvent> telemetryEvents = (List<TelemetryEvent>) response.getBody();
        Assertions.assertNotNull(telemetryEvents, "Response body should not be null.");
        Assertions.assertFalse(telemetryEvents.isEmpty(), "Response list should not be empty.");


        boolean containsTestEvent = telemetryEvents.stream()
                // Compare the unique ID or the entire object
                .anyMatch(e -> e.getId().equals(testEvent.getId()));

        Assertions.assertTrue(containsTestEvent, "The retrieved events list must contain the pre-saved testEvent.");
    }

    @Test
    void countEventsTest() {
        ResponseEntity<?> response = telemetryService.countEvents(testServiceName, testEventType, null, null, null);

        Assertions.assertEquals(200, response.getStatusCode().value());

        Map<String, Integer> responseMap = (Map<String, Integer>) response.getBody();
        Assertions.assertEquals(1, responseMap.get("total"));
    }
}
