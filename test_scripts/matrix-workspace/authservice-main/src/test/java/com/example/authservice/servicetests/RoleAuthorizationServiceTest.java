package com.example.authservice.servicetests;

import com.example.authservice.authorization.Authorization;
import com.example.authservice.authorization.RoleAuthorizationRequest;
import com.example.authservice.authorization.RoleAuthorizationService;
import com.example.authservice.user.Role;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public class RoleAuthorizationServiceTest extends BaseServiceTest {
    @Autowired
    private RoleAuthorizationService roleAuthorizationService;

    @Test
    void addRoleAuthorizationTest() {
        String authorizationName = "TEST_AUTHORIZATION_" + UUID.randomUUID();
        authorizationRepository.save(new Authorization(authorizationName));
        RoleAuthorizationRequest request = new RoleAuthorizationRequest();
        request.setRole(Role.ADMIN);
        request.setAuthorization(authorizationName);

        ResponseEntity<?> response = roleAuthorizationService.addRoleAuthorization(request);

        Assertions.assertEquals(200, response.getStatusCode().value());
        Authorization authorization = authorizationRepository.findByName(authorizationName).get();
        Assertions.assertTrue(roleAuthorizationRepository.findByRoleAndAuthorization(Role.ADMIN, authorization).isPresent());
    }

    @Test
    void removeRoleAuthorizationTest() {
        String authorizationName = "TEST_AUTHORIZATION_" + UUID.randomUUID();
        authorizationRepository.save(new Authorization(authorizationName));
        RoleAuthorizationRequest request = new RoleAuthorizationRequest();
        request.setRole(Role.ADMIN);
        request.setAuthorization(authorizationName);
        roleAuthorizationService.addRoleAuthorization(request);

        ResponseEntity<?> response = roleAuthorizationService.removeRoleAuthorization(request);

        Assertions.assertEquals(200, response.getStatusCode().value());
        Authorization authorization = authorizationRepository.findByName(authorizationName).get();
        Assertions.assertTrue(roleAuthorizationRepository.findByRoleAndAuthorization(Role.ADMIN, authorization).isEmpty());
    }

    @Test
    void getAllRolesTest() {
        ResponseEntity<?> response = roleAuthorizationService.getAllRoles();

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertArrayEquals(Role.values(), (Role[]) response.getBody());
    }
}
