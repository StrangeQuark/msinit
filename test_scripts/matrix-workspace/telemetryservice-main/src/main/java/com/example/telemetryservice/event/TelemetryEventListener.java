package com.example.telemetryservice.event;

import com.example.telemetryservice.utility.JwtUtility;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.stereotype.Service;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Collection;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TelemetryEventListener {
    private final Logger LOGGER = LoggerFactory.getLogger(TelemetryEventListener.class);

    @Autowired
    TelemetryEventRepository telemetryEventRepository;
    @Value("${authservice.integration}")
    boolean authserviceIntegration;
    @Value("${emailservice.integration}")
    boolean emailserviceIntegration;
    @Value("${fileservice.integration}")
    boolean fileserviceIntegration;
    @Value("${vaultservice.integration}")
    boolean vaultserviceIntegration;
    @Autowired
    JwtUtility jwtUtility;

    @Bean
    public Collection<NewTopic> kafkaTopics() {
        Collection<NewTopic> topics = new ArrayList<>();
        topics.add(TopicBuilder.name("general-telemetry-events").partitions(1).replicas(1).build());

        if(authserviceIntegration)
            topics.add(TopicBuilder.name("auth-telemetry-events").partitions(1).replicas(1).build());
        if(emailserviceIntegration)
            topics.add(TopicBuilder.name("email-telemetry-events").partitions(1).replicas(1).build());
        if(fileserviceIntegration)
            topics.add(TopicBuilder.name("file-telemetry-events").partitions(1).replicas(1).build());
        if(vaultserviceIntegration)
            topics.add(TopicBuilder.name("vault-telemetry-events").partitions(1).replicas(1).build());

        return topics;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TelemetryEvent> kafkaListenerContainerFactory(ConsumerFactory<String, TelemetryEvent> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, TelemetryEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(1000L, 2)));

        return factory;
    }

    @KafkaListener(topics = "general-telemetry-events", groupId = "telemetry-group")
    public void generalTelemetryEvents(ConsumerRecord<String, TelemetryEvent> record) {
        LOGGER.info("General telemetry event received");
        saveTelemetryEvent(record);
    }
    @KafkaListener(topics = "auth-telemetry-events", groupId = "telemetry-group", autoStartup = "${authservice.integration}")
    public void authTelemetryEvents(ConsumerRecord<String, TelemetryEvent> record) {
        LOGGER.info("Auth telemetry event received");
        saveTelemetryEvent(record);
    }
    @KafkaListener(topics = "email-telemetry-events", groupId = "telemetry-group", autoStartup = "${emailservice.integration}")
    public void emailTelemetryEvents(ConsumerRecord<String, TelemetryEvent> record) {
        LOGGER.info("Email telemetry event received");
        saveTelemetryEvent(record);
    }
    @KafkaListener(topics = "file-telemetry-events", groupId = "telemetry-group", autoStartup = "${fileservice.integration}")
    public void fileTelemetryEvents(ConsumerRecord<String, TelemetryEvent> record) {
        LOGGER.info("File telemetry event received");
        saveTelemetryEvent(record);
    }
    @KafkaListener(topics = "vault-telemetry-events", groupId = "telemetry-group", autoStartup = "${vaultservice.integration}")
    public void vaultTelemetryEvents(ConsumerRecord<String, TelemetryEvent> record) {
        LOGGER.info("Vault telemetry event received");
        saveTelemetryEvent(record);
    }
    public String getTokenFromKafkaConsumerRecord(ConsumerRecord<String, TelemetryEvent> record) {
        LOGGER.debug("Getting authorization token from Kafka consumer record");

        Header authHeader = record.headers().lastHeader("Authorization");
        if (authHeader == null) {
            LOGGER.error("Missing Authorization header in Kafka message");
            return null;
        }
        String token = new String(authHeader.value());

        if(!token.startsWith("Bearer ")) {
            LOGGER.error("Invalid Authorization header in Kafka message");
            return null;
        }

        LOGGER.debug("Kafka consumer authorization token retrieved");
        return token.substring(7);
    }

    private void saveTelemetryEvent(ConsumerRecord<String, TelemetryEvent> record) {
        TelemetryEvent telemetryEvent = record.value();

        if(authserviceIntegration) {
            String token = getTokenFromKafkaConsumerRecord(record);
            if(!jwtUtility.validateToken(token)) {
                LOGGER.error("Invalid JWT token - telemetry event skipped");
                return;
            }

            try {
                telemetryEvent.setServiceName(jwtUtility.getServiceNameFromToken(token));
            } catch(Exception ex) {
                LOGGER.error("JWT is not a service account token - telemetry event skipped");
                return;
            }
        }

        telemetryEvent.setId(UUID.randomUUID());
        telemetryEvent.setTimestamp(LocalDateTime.now());
        telemetryEventRepository.save(telemetryEvent);
    }
}
