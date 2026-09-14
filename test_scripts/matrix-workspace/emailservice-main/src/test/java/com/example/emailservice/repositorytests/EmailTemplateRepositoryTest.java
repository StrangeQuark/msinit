package com.example.emailservice.repositorytests;

import com.example.emailservice.template.EmailTemplate;
import com.example.emailservice.template.EmailTemplateRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

public class EmailTemplateRepositoryTest extends BaseRepositoryTest {
    @Autowired
    private TestEntityManager testEntityManager;
    @Autowired
    private EmailTemplateRepository emailTemplateRepository;

    String testTemplateName = "TEST_TEMPLATE";
    String testTemplateSubject = "TEST SUBJECT";
    String testTemplateBody = "THIS IS A TEST TEMPLATE BODY";

    @BeforeEach
    void setup() {
        EmailTemplate testEmailTemplate = new EmailTemplate(testTemplateName, testTemplateSubject, testTemplateBody);

        testEntityManager.persistAndFlush(testEmailTemplate);
    }

    @Test
    void findByNameTest() {
        Optional<EmailTemplate> emailTemplate = emailTemplateRepository.findByName(testTemplateName);

        Assertions.assertTrue(emailTemplate.isPresent());
        Assertions.assertEquals(testTemplateName, emailTemplate.get().getName());
        Assertions.assertEquals(testTemplateSubject, emailTemplate.get().getSubject());
        Assertions.assertEquals(testTemplateBody, emailTemplate.get().getBody());
    }

    @Test
    void nameIsUniqueTest() {
        Assertions.assertThrows(DataIntegrityViolationException.class,
                () -> emailTemplateRepository.saveAndFlush(
                        new EmailTemplate(testTemplateName, "Duplicate subject", "Duplicate body")
                ));
    }
}
