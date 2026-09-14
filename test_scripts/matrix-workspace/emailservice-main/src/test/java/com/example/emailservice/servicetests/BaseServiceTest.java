package com.example.emailservice.servicetests;

import com.example.emailservice.template.EmailTemplate;
import com.example.emailservice.template.EmailTemplateRepository;
import com.example.emailservice.token.ConfirmationToken;
import com.example.emailservice.token.ConfirmationTokenRepository;
import com.example.emailservice.token.TokenPurpose;
import com.example.emailservice.utility.JwtUtility;
import com.example.emailservice.utility.AuthUtility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.when;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
public abstract class BaseServiceTest {

    static {
        System.setProperty("ENCRYPTION_KEY", "8C636049C7763F06A35A17E86A542B15");
        System.setProperty("SERVICE_SECRET_EMAIL", "testClientPassword");
    }

    @Autowired
    public ConfirmationTokenRepository confirmationTokenRepository;
    @MockitoBean
    public JavaMailSender javaMailSender;
    @Autowired
    public EmailTemplateRepository emailTemplateRepository;
    @MockitoBean
    public JwtUtility jwtUtility;
    @MockitoBean
    public AuthUtility authUtility;

    public UUID token;
    public String testTemplateName = "USER_REGISTER";
    public String testTemplateSubject = "TEST_TEMPLATE_SUBJECT";
    public String testTemplateBody = "TEST TEMPLATE BODY";

    @BeforeEach
    void setup() {
        when(jwtUtility.validateEmailApiAccess()).thenReturn(true);
        when(jwtUtility.validateEmailTemplateManagement()).thenReturn(true);
        token = UUID.randomUUID();
        ConfirmationToken confirmationToken = new ConfirmationToken(token, LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(15), "test@test.com", TokenPurpose.REGISTRATION.name());

        confirmationTokenRepository.save(confirmationToken);
        EmailTemplate emailTemplate = emailTemplateRepository.findByName(testTemplateName)
                .orElse(new EmailTemplate(testTemplateName, testTemplateSubject, testTemplateBody, TokenPurpose.REGISTRATION.name()));
        emailTemplate.setSubject(testTemplateSubject);
        emailTemplate.setBody(testTemplateBody);
        emailTemplateRepository.save(emailTemplate);
    }

    @AfterEach
    void teardown() {
        confirmationTokenRepository.deleteAll();
        confirmationTokenRepository.flush();
        emailTemplateRepository.deleteAll();
        emailTemplateRepository.flush();
    }
}
