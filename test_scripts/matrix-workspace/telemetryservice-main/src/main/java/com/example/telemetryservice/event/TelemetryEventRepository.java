package com.example.telemetryservice.event;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TelemetryEventRepository extends MongoRepository<TelemetryEvent, UUID>, TelemetryEventRepositoryWrapper {

}
