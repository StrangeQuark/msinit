package com.example.vaultservice.servicetests;

import com.example.vaultservice.environment.Environment;
import com.example.vaultservice.environment.EnvironmentRepository;
import com.example.vaultservice.service.Service;
import com.example.vaultservice.service.ServiceRepository;
import com.example.vaultservice.serviceuser.ServiceUser;// Integration line: Auth
import com.example.vaultservice.serviceuser.ServiceUserRepository;// Integration line: Auth
import com.example.vaultservice.serviceuser.ServiceUserRole;// Integration line: Auth
import com.example.vaultservice.utility.AuthUtility;// Integration line: Auth
import com.example.vaultservice.utility.JwtUtility;// Integration line: Auth
import com.example.vaultservice.variable.Variable;
import com.example.vaultservice.variable.VariableRepository;
import com.example.vaultservice.vault.VaultService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;// Integration line: Auth
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;// Integration line: Auth
import java.util.UUID;// Integration line: Auth
import static org.mockito.Mockito.when;// Integration line: Auth

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
public abstract class BaseServiceTest {
    static {
        System.setProperty("ENCRYPTION_KEY", "AA1A2A8C0E4F76FB3C13F66225AAAC42");
    }

    @Autowired
    public ServiceRepository serviceRepository;
    @Autowired
    public EnvironmentRepository environmentRepository;
    @Autowired
    public VariableRepository variableRepository;
    @Autowired
    public VaultService vaultService;

    public Service testService;
    public Environment testEnvironment;
    public Variable testVariable;
    @Value("${BOOTSTRAP_TOKEN}")
    public String BOOTSTRAP_TOKEN;
    // Integration function start: Auth
    @Value("${AUTH_CICD_TOKEN}")
    public String AUTH_CICD_TOKEN;
    @Autowired
    public ServiceUserRepository serviceUserRepository;
    @MockitoBean
    public JwtUtility jwtUtility;
    @MockitoBean
    public AuthUtility authUtility;
    public UUID testOwnerId = UUID.randomUUID();
    public UUID testUserId = UUID.randomUUID();
    public ServiceUser serviceUser;
    public String testBootstrapService = "testBootstrapService";
    // Integration function end: Auth

    @BeforeEach
    void setup() {
        try {
            testService = new Service("testService");
            testEnvironment = new Environment(testService, "testEnvironment");
            testVariable = new Variable(testEnvironment, "testKey", "testValue");

            serviceRepository.save(testService);
            environmentRepository.save(testEnvironment);
            variableRepository.save(testVariable);
            // Integration function start: Auth
            serviceUser = new ServiceUser(testService, testOwnerId, ServiceUserRole.OWNER);
            serviceUserRepository.save(serviceUser);

            // Mock authUtility functions
            when(jwtUtility.extractId()).thenReturn(testOwnerId.toString());
            when(authUtility.getUserId("testUser")).thenReturn(testUserId.toString());// Integration function end: Auth
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @AfterEach
    void teardown() {
        variableRepository.deleteAll();
        environmentRepository.deleteAll();
        serviceRepository.deleteAll();
    }
}
