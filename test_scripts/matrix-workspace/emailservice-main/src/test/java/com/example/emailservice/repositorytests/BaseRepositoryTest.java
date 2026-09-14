package com.example.emailservice.repositorytests;

import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
public class BaseRepositoryTest {
    static {
        System.setProperty("ENCRYPTION_KEY", "8C636049C7763F06A35A17E86A542B15");
        System.setProperty("SERVICE_SECRET_EMAIL", "testClientPassword");
    }
}
