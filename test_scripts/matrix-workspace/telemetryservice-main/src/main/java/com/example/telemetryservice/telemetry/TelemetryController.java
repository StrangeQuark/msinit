package com.example.telemetryservice.telemetry;

import com.example.telemetryservice.event.TelemetryEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/telemetry")
public class TelemetryController {
    @Autowired
    TelemetryService telemetryService;

    @PostMapping("/create-event")
    public ResponseEntity<?> createEvent(@RequestBody TelemetryEvent telemetryEvent) {
        return telemetryService.createEvent(telemetryEvent);
    }

    @GetMapping("/get-events")
    public ResponseEntity<?> getEvents(
            @RequestParam String eventType,
            @RequestParam(required = false) String numberOfEvents,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDateTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDateTime) {

        return telemetryService.getEvents(eventType, numberOfEvents, startDateTime, endDateTime);
    }

    @GetMapping("/count-events")
    public ResponseEntity<?> countEvents(
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String interval,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDateTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDateTime) {

        return telemetryService.countEvents(serviceName, eventType, interval, startDateTime, endDateTime);
    }
}
