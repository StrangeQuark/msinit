package com.example.telemetryservice.event;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TelemetryEventRepositoryImpl implements TelemetryEventRepositoryWrapper {
    @Autowired
    private MongoTemplate mongoTemplate;

    private static final String COLLECTION = "telemetry_events";

    @Override
    public List<TelemetryEvent> getEventsByEventType(String eventType, String numberOfEvents, LocalDateTime start, LocalDateTime end) {
        Criteria criteria = new Criteria();
        if (eventType != null)
            criteria.and("eventType").is(eventType);
        if (start != null && end != null) {
            criteria.and("timestamp").gte(start).lte(end);
        } else if (start != null) {
            criteria.and("timestamp").gte(start);
        } else if (end != null) {
            criteria.and("timestamp").lte(end);
        }

        Query query = new Query(criteria);
        query.with(Sort.by(Sort.Direction.DESC, "timestamp"));

        if (numberOfEvents != null)
            query.limit(Integer.parseInt(numberOfEvents));

        return mongoTemplate.find(query, TelemetryEvent.class, COLLECTION);
    }

    @Override
    public Map<String, Integer> countEvents(String serviceName, String eventType, String interval, LocalDateTime start, LocalDateTime end) {
        List<AggregationOperation> ops = new ArrayList<>();

        Criteria criteria = new Criteria();

        if(serviceName != null)
            criteria.and("serviceName").is(serviceName);
        if(eventType != null)
            criteria.and("eventType").is(eventType);

        if (start != null && end != null) {
            criteria.and("timestamp").gte(start).lte(end);
        } else if (start != null) {
            criteria.and("timestamp").gte(start);
        } else if (end != null) {
            criteria.and("timestamp").lte(end);
        }

        ops.add(Aggregation.match(criteria));

        if(interval != null) {
            String dateFormat = switch (interval.toLowerCase()) {
                case "minute" -> "%Y-%m-%d %H:%M";
                case "hour" -> "%Y-%m-%d %H:00";
                case "day" -> "%Y-%m-%d";
                case "week" -> "%Y-%U";
                case "month" -> "%Y-%m";
                default -> throw new IllegalArgumentException("Invalid interval: " + interval);
            };

            ops.add(Aggregation.project()
                    .andExpression("{$dateToString: { format: '" + dateFormat + "', date: '$timestamp' }}")
                    .as("bucket"));
        }

        ops.add(Aggregation.group("bucket").count().as("count"));

        ops.add(Aggregation.sort(Sort.by(Sort.Direction.ASC, "_id")));

        Aggregation agg = Aggregation.newAggregation(ops);

        AggregationResults<Document> results =
                mongoTemplate.aggregate(agg, "telemetry_events", Document.class);

        Map<String, Integer> output = new LinkedHashMap<>();
        for (Document doc : results.getMappedResults()) {
            if (interval != null) {
                String bucket = doc.getString("_id");
                output.put(bucket, doc.getInteger("count"));
            } else {
                output.put("total", doc.getInteger("count"));
            }
        }

        return output;
    }
}
