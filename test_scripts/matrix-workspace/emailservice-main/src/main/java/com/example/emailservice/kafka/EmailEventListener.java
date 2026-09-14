package com.example.emailservice.kafka;

import com.example.emailservice.email.EmailRequest;
import com.example.emailservice.email.EmailService;
import com.example.emailservice.utility.JwtUtility;
import com.example.emailservice.utility.TelemetryUtility;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class EmailEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailEventListener.class);

    private final EmailService emailService;
    @Value("${authservice.integration}")
    boolean authserviceIntegration;
    @Autowired
    TelemetryUtility telemetryUtility;
    @Autowired
    JwtUtility jwtUtility;

    public EmailEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = "general-email-events", groupId = "email-group")
    public void generalEmailEvents(ConsumerRecord<String, EmailRequest> record) {
        LOGGER.info("General email event received");
        EmailRequest emailRequest = record.value();
        String userId = null;
        if(authserviceIntegration) {
            String token = getTokenFromKafkaConsumerRecord(record);
            if(!jwtUtility.validateEmailApiAccessToken(token)) {
                LOGGER.error("Invalid JWT token - general email event skipped");
                return;
            }
            userId = jwtUtility.extractId(token);
        }

        Map<String, Object> telemetryMetadata = new HashMap<>();
        if(authserviceIntegration)
            telemetryMetadata.put("userId", userId);

        telemetryUtility.sendTelemetryEvent("email-event-general", telemetryMetadata);

        emailService.sendEmail(emailRequest, false, userId);
    }

    @KafkaListener(topics = "template-email-events", groupId = "email-group")
    public void templateEmailEvents(ConsumerRecord<String, EmailRequest> record) {
        LOGGER.info("Template email event received");
        EmailRequest emailRequest = record.value();
        String userId = null;
        if(authserviceIntegration) {
            String token = getTokenFromKafkaConsumerRecord(record);
            if(!jwtUtility.validateEmailApiAccessToken(token)) {
                LOGGER.error("Invalid JWT token - template email event skipped");
                return;
            }
            userId = jwtUtility.extractId(token);
        }

        Map<String, Object> telemetryMetadata = new HashMap<>();
        if(authserviceIntegration)
            telemetryMetadata.put("userId", userId);

        telemetryUtility.sendTelemetryEvent("email-event-template", telemetryMetadata);

        emailService.sendTemplateEmail(emailRequest, false, userId);
    }
    public String getTokenFromKafkaConsumerRecord(ConsumerRecord<String, ?> record) {
        LOGGER.info("Getting authorization token from Kafka consumer record");

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

        LOGGER.info("Kafka consumer authorization token retrieved");
        return token.substring(7);
    }
}
