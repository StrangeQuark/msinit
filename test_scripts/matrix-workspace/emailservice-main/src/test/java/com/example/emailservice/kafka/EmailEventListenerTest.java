package com.example.emailservice.kafka;

import com.example.emailservice.email.EmailRequest;
import com.example.emailservice.email.EmailService;
import com.example.emailservice.utility.JwtUtility;
import com.example.emailservice.utility.TelemetryUtility;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class EmailEventListenerTest {
    private EmailEventListener emailEventListener;
    private EmailService emailService;
    private JwtUtility jwtUtility;
    private TelemetryUtility telemetryUtility;

    @BeforeEach
    void setup() {
        emailService = mock(EmailService.class);
        jwtUtility = mock(JwtUtility.class);
        telemetryUtility = mock(TelemetryUtility.class);

        emailEventListener = new EmailEventListener(emailService);
        emailEventListener.jwtUtility = jwtUtility;
        emailEventListener.telemetryUtility = telemetryUtility;
        emailEventListener.authserviceIntegration = true;
    }

    @Test
    void generalEmailEventValidatesKafkaTokenTest() {
        EmailRequest emailRequest = new EmailRequest();
        ConsumerRecord<String, EmailRequest> record = new ConsumerRecord<>("general-email-events", 0, 0, "key", emailRequest);
        record.headers().add("Authorization", "Bearer test-token".getBytes());
        when(jwtUtility.validateEmailApiAccessToken("test-token")).thenReturn(true);
        when(jwtUtility.extractId("test-token")).thenReturn("test-user");

        emailEventListener.generalEmailEvents(record);

        verify(emailService).sendEmail(emailRequest, false, "test-user");
    }

    @Test
    void generalEmailEventWithoutTokenIsSkippedTest() {
        ConsumerRecord<String, EmailRequest> record = new ConsumerRecord<>("general-email-events", 0, 0, "key", new EmailRequest());
        when(jwtUtility.validateEmailApiAccessToken(null)).thenReturn(false);

        emailEventListener.generalEmailEvents(record);

        verifyNoInteractions(emailService, telemetryUtility);
    }

    @Test
    void generalEmailEventWithoutAuthserviceIntegrationTest() {
        emailEventListener.authserviceIntegration = false;
        EmailRequest emailRequest = new EmailRequest();
        ConsumerRecord<String, EmailRequest> record = new ConsumerRecord<>("general-email-events", 0, 0, "key", emailRequest);

        emailEventListener.generalEmailEvents(record);

        verify(emailService).sendEmail(emailRequest, false, null);
    }
}
