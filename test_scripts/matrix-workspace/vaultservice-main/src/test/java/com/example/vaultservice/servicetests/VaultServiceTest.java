package com.example.vaultservice.servicetests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.vaultservice.environment.Environment;
import com.example.vaultservice.service.Service;
import com.example.vaultservice.service.ServiceResponse;
import com.example.vaultservice.environment.EnvironmentResponse;
import com.example.vaultservice.serviceuser.ServiceUser;// Integration line: Auth
import com.example.vaultservice.serviceuser.ServiceUserRequest;// Integration line: Auth
import com.example.vaultservice.serviceuser.ServiceUserRole;// Integration line: Auth
import com.example.vaultservice.serviceuser.ServiceUserResponse; // Integration line: Auth
import com.example.vaultservice.variable.Variable;
import com.example.vaultservice.variable.VariableRequest;
import com.example.vaultservice.variable.VariableResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;// Integration line: Auth
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;// Integration line: Auth
import org.springframework.web.context.request.ServletRequestAttributes;// Integration line: Auth

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays; // Integration line: Auth
import java.util.List;
import java.util.UUID; // Integration line: Auth

import static org.mockito.Mockito.when; // Integration line: Auth

public class VaultServiceTest extends BaseServiceTest {
    @Test
    void createServiceTest() {
        ResponseEntity<?> response = vaultService.createService("testService1");

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertTrue(serviceRepository.findByName("testService1").isPresent());
    }

    @Test
    void createEnvironmentTest() {
        ResponseEntity<?> response = vaultService.createEnvironment(testService.getName(), "testEnvironment1");

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertTrue(environmentRepository.findByNameAndServiceId("testEnvironment1", testService.getId()).isPresent());
    }

    @Test
    void getServiceTest() {
        ResponseEntity<?> response = vaultService.getService(testService.getName());
        ServiceResponse serviceResponse = (ServiceResponse) response.getBody();

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertEquals(testService.getName(), serviceResponse.getName());
        Assertions.assertTrue(serviceResponse.getEnvironments().contains(testEnvironment.getName()));
    }

    @Test
    void getEnvironmentsByServiceTest() {
        ResponseEntity<?> response = vaultService.getEnvironmentsByService(testService.getName());
        List<String> environments = (List<String>) response.getBody();

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertTrue(environments.contains(testEnvironment.getName()));
    }

    @Test
    void getEnvironmentTest() {
        ResponseEntity<?> response = vaultService.getEnvironment(testService.getName(), testEnvironment.getName());
        EnvironmentResponse environmentResponse = (EnvironmentResponse) response.getBody();

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertEquals(testEnvironment.getName(), environmentResponse.getName());
        Assertions.assertEquals(testVariable.getKey(), environmentResponse.getVariables().get(0).getKey());
    }

    @Test
    void getVariablesByServiceTest() {
        ResponseEntity<?> response = vaultService.getVariablesByService(testService.getName());

        Assertions.assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void getVariablesByEnvironmentTest() {
        ResponseEntity<?> response = vaultService.getVariablesByEnvironment(testService.getName(), testEnvironment.getName());

        Assertions.assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void getVariableByNameTest() {
        ResponseEntity<?> response = vaultService.getVariableByName(testService.getName(), testEnvironment.getName(), "testKey");
        VariableResponse variableResponse = (VariableResponse) response.getBody();

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertEquals(testVariable.getKey(), variableResponse.getKey());
        Assertions.assertEquals(testVariable.getValue(), variableResponse.getValue());
    }

    @Test
    void responseDtosDoNotExposePersistenceFieldsTest() throws Exception {
        ResponseEntity<?> response = vaultService.getEnvironment(testService.getName(), testEnvironment.getName());
        String responseJson = new ObjectMapper().writeValueAsString(response.getBody());

        Assertions.assertFalse(responseJson.contains("id"));
        Assertions.assertFalse(responseJson.contains("environment"));
        Assertions.assertFalse(responseJson.contains("createdAt"));
        Assertions.assertFalse(responseJson.contains("updatedAt"));
        Assertions.assertFalse(responseJson.contains("lastUpdatedBy"));
    }

    @Test
    void addVariableTest() {
        VariableRequest variable = new VariableRequest();
        variable.setKey("testKey1");
        variable.setValue("testValue1");
        ResponseEntity<?> response = vaultService.addVariable(testService.getName(), testEnvironment.getName(), variable);

        Assertions.assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void variableRequestIgnoresPersistenceFieldsTest() throws Exception {
        VariableRequest variableRequest = new ObjectMapper().readValue("""
                {
                    "id": "9d11e4c4-7284-4b16-a7cd-5912ce374c28",
                    "environment": {"id": "4f8a3c63-5ed3-4e51-9b39-780663e84bbb"},
                    "createdAt": "2026-01-01T00:00:00",
                    "updatedAt": "2026-01-01T00:00:00",
                    "lastUpdatedBy": "1ee36a11-024c-4234-a75a-34489c2fc0be",
                    "key": "testKey1",
                    "value": "testValue1"
                }
                """, VariableRequest.class);

        Assertions.assertEquals("testKey1", variableRequest.getKey());
        Assertions.assertEquals("testValue1", variableRequest.getValue());
    }

    @Test
    void updateVariableTest() {
        ResponseEntity<?> response = vaultService.updateVariable(testService.getName(), testEnvironment.getName(),
                variableRequest(testVariable.getKey(), "newValue"));

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertEquals("newValue", variableRepository.findByEnvironmentIdAndKey(testEnvironment.getId(), testVariable.getKey()).get().getValue());
    }

    @Test
    void updateVariablesTest() {
        List<VariableRequest> vars = new ArrayList<>();
        vars.add(variableRequest(testVariable.getKey(), "newValue"));
        vars.add(variableRequest("skippedKey", "skippedValue"));

        ResponseEntity<?> response = vaultService.updateVariables(testService.getName(), testEnvironment.getName(), vars);

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertEquals("Skipped variables: [skippedKey]", response.getBody().toString());
        Assertions.assertEquals("newValue", variableRepository.findByEnvironmentIdAndKey(testEnvironment.getId(), testVariable.getKey()).get().getValue());
    }

    @Test
    void addEnvFileTest() {
        String fileContent = "FOO=bar\nTEST=val1\n";

        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "test.env", "text/plain", fileContent.getBytes(StandardCharsets.UTF_8)
        );

        ResponseEntity<?> response = vaultService.addEnvFile(testService.getName(), testEnvironment.getName(), mockFile);

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertTrue(response.getBody().toString().contains("Variables added: 2"));
    }

    @Test
    void downloadEnvFileTest() {
        vaultService.addVariable(testService.getName(), testEnvironment.getName(), variableRequest("FOO", "bar"));
        vaultService.addVariable(testService.getName(), testEnvironment.getName(), variableRequest("TEST", "val1"));

        ResponseEntity<?> response = vaultService.downloadEnvFile(testService.getName(), testEnvironment.getName());

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertTrue(response.getHeaders().getContentDisposition().getFilename().endsWith(".env"));

        ByteArrayResource body = (ByteArrayResource) response.getBody();
        String content = new String(body.getByteArray(), StandardCharsets.UTF_8);

        Assertions.assertTrue(content.contains("testKey=\"testValue\""));
        Assertions.assertTrue(content.contains("FOO=\"bar\""));
        Assertions.assertTrue(content.contains("TEST=\"val1\""));
    }

    @Test
    void addEnvFileRejectsDuplicateKeysTest() {
        String fileContent = "FOO=bar\nFOO=val1\n";

        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "test.env", "text/plain", fileContent.getBytes(StandardCharsets.UTF_8)
        );

        ResponseEntity<?> response = vaultService.addEnvFile(testService.getName(), testEnvironment.getName(), mockFile);

        Assertions.assertEquals(400, response.getStatusCode().value());
        Assertions.assertTrue(variableRepository.findByEnvironmentIdAndKey(testEnvironment.getId(), "FOO").isEmpty());
    }

    @Test
    void deleteVariableTest() {
        ResponseEntity<?> response = vaultService.deleteVariable(testService.getName(), testEnvironment.getName(), "testKey");

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertTrue(variableRepository.findByEnvironmentIdAndKey(testEnvironment.getId(), testVariable.getKey()).isEmpty());
    }

    @Test
    void deleteEnvironmentTest() {
        ResponseEntity<?> response = vaultService.deleteEnvironment(testService.getName(), testEnvironment.getName());

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertTrue(environmentRepository.findByNameAndServiceId(testEnvironment.getName(), testService.getId()).isEmpty());
    }

    @Test
    void deleteServiceTest() {
        ResponseEntity<?> response = vaultService.deleteService(testService.getName());

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertTrue(serviceRepository.findByName(testService.getName()).isEmpty());
    }

    @Test
    void bootstrapEnvFileTest() {
        String fileContent = "FOO=bar\nTEST=val1\n";

        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "test.env", "text/plain", fileContent.getBytes(StandardCharsets.UTF_8)
        );

        ResponseEntity<?> response = vaultService.bootstrapEnvFile(testBootstrapService, testEnvironment.getName(), mockFile, BOOTSTRAP_TOKEN);

        String responseBody = response.getBody().toString();

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertEquals("Env file successfully bootstrapped", responseBody);
    }

    @Test
    void bootstrapEnvFileRejectsInvalidTokenTest() {
        String fileContent = "FOO=bar\nTEST=val1\n";

        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "test.env", "text/plain", fileContent.getBytes(StandardCharsets.UTF_8)
        );

        ResponseEntity<?> response = vaultService.bootstrapEnvFile(testBootstrapService, testEnvironment.getName(), mockFile, "invalidBootstrapToken");

        Assertions.assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void bootstrapEnvFileRejectsDuplicateKeysTest() {
        String serviceName = "duplicateBootstrapService";
        String fileContent = "FOO=bar\nFOO=val1\n";

        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "test.env", "text/plain", fileContent.getBytes(StandardCharsets.UTF_8)
        );

        ResponseEntity<?> response = vaultService.bootstrapEnvFile(
                serviceName, testEnvironment.getName(), mockFile, BOOTSTRAP_TOKEN
        );

        Assertions.assertEquals(400, response.getStatusCode().value());
        Assertions.assertTrue(serviceRepository.findByName(serviceName).isEmpty());
    }

    @Test
    void bootstrapEnvFileAddsEnvironmentToExistingServiceTest() {
        String fileContent = "FOO=bar\nTEST=val1\n";

        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "test.env", "text/plain", fileContent.getBytes(StandardCharsets.UTF_8)
        );

        ResponseEntity<?> response = vaultService.bootstrapEnvFile(testBootstrapService, testEnvironment.getName(), mockFile, BOOTSTRAP_TOKEN);
        Assertions.assertEquals(200, response.getStatusCode().value());

        response = vaultService.bootstrapEnvFile(testBootstrapService, "testBootstrapEnvironment", mockFile, BOOTSTRAP_TOKEN);

        Assertions.assertEquals(200, response.getStatusCode().value());
    }
    // Integration function start: Auth
    @Test
    void getAllServicesTest() {
        ResponseEntity<?> response = vaultService.getAllServices();

        List<String> services = (List<String>) response.getBody();

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertTrue(services.contains(testService.getName()));
    }

    @Test
    void getUsersByServiceTest() {
        ResponseEntity<?> response = vaultService.getUsersByService(testService.getName());

        List<ServiceUserResponse> users = (List<ServiceUserResponse>) response.getBody();

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertTrue(users.stream().anyMatch(u -> u.getUserId().equals(serviceUser.getUserId())));
    }

    @Test
    void getUsersByServiceAllowsManagerTest() {
        serviceUserRepository.save(new ServiceUser(testService, testUserId, ServiceUserRole.MANAGER));
        when(jwtUtility.extractId()).thenReturn(testUserId.toString());

        ResponseEntity<?> response = vaultService.getUsersByService(testService.getName());

        Assertions.assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void getUsersByServiceRejectsNonMemberTest() {
        when(jwtUtility.extractId()).thenReturn(UUID.randomUUID().toString());

        ResponseEntity<?> response = vaultService.getUsersByService(testService.getName());

        Assertions.assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void getAllRolesTest() {
        ResponseEntity<?> response = vaultService.getAllRoles();
        List<ServiceUserRole> roles = Arrays.asList(((ServiceUserRole[]) response.getBody()));

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertTrue(roles.contains(ServiceUserRole.OWNER));
        Assertions.assertTrue(roles.contains(ServiceUserRole.MANAGER));
        Assertions.assertTrue(roles.contains(ServiceUserRole.MAINTAINER));
    }

    @Test
    void getCurrentUserRoleTest() {
        ResponseEntity<?> response = vaultService.getCurrentUserRole(testService.getName());

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertEquals("OWNER", response.getBody().toString());
    }

    @Test
    void updateUserRoleTest() {
        serviceUserRepository.save(new ServiceUser(testService, testUserId, ServiceUserRole.MAINTAINER));

        Assertions.assertTrue(serviceUserRepository.findByUserIdAndServiceId(testUserId, testService.getId()).isPresent());

        ServiceUserRequest serviceUserRequest = new ServiceUserRequest();
        serviceUserRequest.setServiceName(testService.getName());
        serviceUserRequest.setUsername("testUser");
        serviceUserRequest.setRole(ServiceUserRole.OWNER);

        ResponseEntity<?> response = vaultService.updateUserRole(serviceUserRequest);

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertEquals(ServiceUserRole.OWNER, serviceUserRepository.findByUserIdAndServiceId(testUserId, testService.getId()).get().getRole());
    }

    @Test
    void addUserToServiceTest() {
        ServiceUserRequest serviceUserRequest = new ServiceUserRequest();
        serviceUserRequest.setServiceName(testService.getName());
        serviceUserRequest.setUsername("testUser");

        ResponseEntity<?> response = vaultService.addUserToService(serviceUserRequest);

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertTrue(serviceUserRepository.findByUserIdAndServiceId(testUserId, testService.getId()).isPresent());
    }

    @Test
    void deleteUserFromServiceTest() {
        serviceUserRepository.save(new ServiceUser(testService, testUserId, ServiceUserRole.MAINTAINER));

        Assertions.assertTrue(serviceUserRepository.findByUserIdAndServiceId(testUserId, testService.getId()).isPresent());

        ServiceUserRequest serviceUserRequest = new ServiceUserRequest();
        serviceUserRequest.setServiceName(testService.getName());
        serviceUserRequest.setUsername("testUser");

        ResponseEntity<?> response = vaultService.deleteUserFromService(serviceUserRequest);

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertTrue(serviceUserRepository.findByUserIdAndServiceId(testUserId, testService.getId()).isEmpty());
    }

    @Test
    void deleteUserFromAllServicesTest() {
        serviceUserRepository.save(new ServiceUser(testService, testUserId, ServiceUserRole.MAINTAINER));

        Assertions.assertTrue(serviceUserRepository.findByUserIdAndServiceId(testUserId, testService.getId()).isPresent());

        ServiceUserRequest serviceUserRequest = new ServiceUserRequest();
        serviceUserRequest.setServiceName(testService.getName());
        serviceUserRequest.setUsername("testUser");

        ResponseEntity<?> response = vaultService.deleteUserFromAllServices(serviceUserRequest);

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertTrue(serviceUserRepository.findByUserIdAndServiceId(testUserId, testService.getId()).isEmpty());
    }

    @Test
    void authServiceCanDeleteUserFromAllServicesTest() {
        serviceUserRepository.save(new ServiceUser(testService, testUserId, ServiceUserRole.MAINTAINER));
        when(jwtUtility.isAuthService()).thenReturn(true);

        ServiceUserRequest serviceUserRequest = new ServiceUserRequest();
        serviceUserRequest.setUsername("testUser");

        ResponseEntity<?> response = vaultService.deleteUserFromAllServices(serviceUserRequest);

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertTrue(serviceUserRepository.findByUserIdAndServiceId(testUserId, testService.getId()).isEmpty());
    }

    @Test
    void deleteUserFromAllServicesDoesNotPartiallyDeleteTest() {
        Service protectedService = new Service("protectedService_" + UUID.randomUUID());
        serviceRepository.save(protectedService);

        serviceUserRepository.save(new ServiceUser(
                protectedService,
                testOwnerId,
                ServiceUserRole.OWNER
        ));
        serviceUserRepository.save(new ServiceUser(
                protectedService,
                UUID.randomUUID(),
                ServiceUserRole.MAINTAINER
        ));

        when(authUtility.getUserId("testUser")).thenReturn(testOwnerId.toString());

        ServiceUserRequest serviceUserRequest = new ServiceUserRequest();
        serviceUserRequest.setUsername("testUser");

        ResponseEntity<?> response = vaultService.deleteUserFromAllServices(serviceUserRequest);

        Assertions.assertEquals(400, response.getStatusCode().value());
        Assertions.assertTrue(serviceRepository.findByName(testService.getName()).isPresent());
        Assertions.assertTrue(serviceUserRepository
                .findByUserIdAndServiceId(testOwnerId, testService.getId())
                .isPresent());
        Assertions.assertTrue(serviceRepository.findByName(protectedService.getName()).isPresent());
        Assertions.assertTrue(serviceUserRepository
                .findByUserIdAndServiceId(testOwnerId, protectedService.getId())
                .isPresent());
    }

    @Test
    void bootstrapUserTest() {
        String fileContent = "FOO=bar\nTEST=val1\n";

        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "test.env", "text/plain", fileContent.getBytes(StandardCharsets.UTF_8)
        );

        ResponseEntity<?> response = vaultService.bootstrapEnvFile(testBootstrapService, testEnvironment.getName(), mockFile, BOOTSTRAP_TOKEN);
        Assertions.assertEquals(200, response.getStatusCode().value());

        response = vaultService.bootstrapUser(testBootstrapService, BOOTSTRAP_TOKEN);

        Assertions.assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void cicdGetTest() {
        Environment cicdEnvironment = setupCicdEnvironment();
        Variable cicdVariable = new Variable(cicdEnvironment, "testKey", "value=$test\"line\nnext");
        variableRepository.save(cicdVariable);

        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader("X-CICD-TOKEN", AUTH_CICD_TOKEN);

        ServletRequestAttributes attrs = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(attrs);

        ResponseEntity<?> response = vaultService.cicdGet("authservice", cicdEnvironment.getName());

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertEquals("testKey=\"value=$test\\\"line\\nnext\"\n", response.getBody());
    }

    @Test
    void cicdGetInvalidVariableTest() {
        Environment cicdEnvironment = setupCicdEnvironment();
        Variable cicdVariable = new Variable(cicdEnvironment, "TEST=KEY", "testValue");
        variableRepository.save(cicdVariable);

        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader("X-CICD-TOKEN", AUTH_CICD_TOKEN);

        ServletRequestAttributes attrs = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(attrs);

        ResponseEntity<?> response = vaultService.cicdGet("authservice", cicdEnvironment.getName());

        Assertions.assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void cicdGetWrongServiceTokenTest() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader("X-CICD-TOKEN", AUTH_CICD_TOKEN);

        ServletRequestAttributes attrs = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(attrs);

        ResponseEntity<?> response = vaultService.cicdGet("emailservice", testEnvironment.getName());

        Assertions.assertEquals(403, response.getStatusCode().value());
    }

    private Environment setupCicdEnvironment() {
        Service cicdService = new Service("authservice");
        serviceRepository.save(cicdService);

        Environment cicdEnvironment = new Environment(cicdService, "cicdEnvironment");
        return environmentRepository.save(cicdEnvironment);
    }

    private VariableRequest variableRequest(String key, String value) {
        VariableRequest variableRequest = new VariableRequest();
        variableRequest.setKey(key);
        variableRequest.setValue(value);
        return variableRequest;
    }// Integration function end: Auth
}
