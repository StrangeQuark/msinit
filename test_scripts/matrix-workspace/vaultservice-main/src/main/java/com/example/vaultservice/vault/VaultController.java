package com.example.vaultservice.vault;

import com.example.vaultservice.serviceuser.ServiceUserRequest;// Integration line: Auth
import com.example.vaultservice.variable.Variable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/vault")
public class VaultController {

    @Autowired
    private VaultService vaultService;

    @PostMapping("/create-service")
    public ResponseEntity<?> createService(@RequestBody VaultRequest vaultRequest) {
        return vaultService.createService(vaultRequest.getServiceName());
    }

    @PostMapping("/create-environment")
    public ResponseEntity<?> createEnvironment(@RequestBody VaultRequest vaultRequest) {
        return vaultService.createEnvironment(vaultRequest.getServiceName(), vaultRequest.getEnvironmentName());
    }

    @PostMapping("/get-service")
    public ResponseEntity<?> getService(@RequestBody VaultRequest vaultRequest) {
        return vaultService.getService(vaultRequest.getServiceName());
    }

    @PostMapping("/get-environments-by-service")
    public ResponseEntity<?> getEnvironmentsByService(@RequestBody VaultRequest vaultRequest) {
        return vaultService.getEnvironmentsByService(vaultRequest.getServiceName());
    }

    @PostMapping("/get-environment")
    public ResponseEntity<?> getEnvironment(@RequestBody VaultRequest vaultRequest) {
        return vaultService.getEnvironment(vaultRequest.getServiceName(), vaultRequest.getEnvironmentName());
    }

    @PostMapping("/get-variables-by-service")
    public ResponseEntity<?> getVariablesByService(@RequestBody VaultRequest vaultRequest) {
        return vaultService.getVariablesByService(vaultRequest.getServiceName());
    }

    @PostMapping("/get-variables-by-environment")
    public ResponseEntity<?> getVariablesByEnvironment(@RequestBody VaultRequest vaultRequest) {
        return vaultService.getVariablesByEnvironment(vaultRequest.getServiceName(), vaultRequest.getEnvironmentName());
    }

    @PostMapping("/get-variable-by-name")
    public ResponseEntity<?> getVariableByName(@RequestBody VaultRequest vaultRequest) {
        return vaultService.getVariableByName(vaultRequest.getServiceName(), vaultRequest.getEnvironmentName(), vaultRequest.getVariableName());
    }

    @PostMapping("/add-variable")
    public ResponseEntity<?> addVariable(@RequestBody VaultRequest vaultRequest) {
        return vaultService.addVariable(vaultRequest.getServiceName(), vaultRequest.getEnvironmentName(), vaultRequest.getVariable());
    }

    @PostMapping("/update-variable")
    public ResponseEntity<?> updateVariable(@RequestBody VaultRequest vaultRequest) {
        return vaultService.updateVariable(vaultRequest.getServiceName(), vaultRequest.getEnvironmentName(), vaultRequest.getVariable());
    }

    @PostMapping("/update-variables")
    public ResponseEntity<?> updateVariables(@RequestBody VaultRequest vaultRequest) {
        return vaultService.updateVariables(vaultRequest.getServiceName(), vaultRequest.getEnvironmentName(), vaultRequest.getVariables());
    }

    @PostMapping("/add-env-file")
    public ResponseEntity<?> addEnvFile(
            @RequestParam String serviceName,
            @RequestParam String environmentName,
            @RequestParam("file") MultipartFile file) {
        return vaultService.addEnvFile(serviceName, environmentName, file);
    }

    @PostMapping("/download-env-file")
    public ResponseEntity<?> downloadEnvFile(@RequestBody VaultRequest vaultRequest) {
        return vaultService.downloadEnvFile(vaultRequest.getServiceName(), vaultRequest.getEnvironmentName());
    }

    @DeleteMapping("/delete-variable")
    public ResponseEntity<?> deleteVariable(@RequestBody VaultRequest vaultRequest) {
        return vaultService.deleteVariable(vaultRequest.getServiceName(), vaultRequest.getEnvironmentName(), vaultRequest.getVariableName());
    }

    @DeleteMapping("/delete-environment")
    public ResponseEntity<?> deleteEnvironment(@RequestBody VaultRequest vaultRequest) {
        return vaultService.deleteEnvironment(vaultRequest.getServiceName(), vaultRequest.getEnvironmentName());
    }

    @DeleteMapping("/delete-service")
    public ResponseEntity<?> deleteService(@RequestBody VaultRequest vaultRequest) {
        return vaultService.deleteService(vaultRequest.getServiceName());
    }

    @GetMapping("/get-all-services")
    public ResponseEntity<?> getAllServices() {
        return vaultService.getAllServices();
    }

    @PostMapping("/bootstrap/add-env")
    public ResponseEntity<?> bootstrapEnvFile(
            @RequestParam String serviceName,
            @RequestParam String environmentName,
            @RequestHeader("X-VAULT-BOOTSTRAP-TOKEN") String bootstrapToken,
            @RequestParam("file") MultipartFile file) {
        return vaultService.bootstrapEnvFile(serviceName, environmentName, file, bootstrapToken);
    }
    // Integration function start: Auth
    @PostMapping("/get-users-by-service")
    public ResponseEntity<?> getUsersByService(@RequestBody VaultRequest vaultRequest) {
        return vaultService.getUsersByService(vaultRequest.getServiceName());
    }

    @GetMapping("/get-all-roles")
    public ResponseEntity<?> getAllRoles() {
        return vaultService.getAllRoles();
    }

    @PostMapping("/get-current-user-role")
    public ResponseEntity<?> getCurrentUserRole(@RequestBody VaultRequest vaultRequest) {
        return vaultService.getCurrentUserRole(vaultRequest.getServiceName());
    }

    @PostMapping("/update-user-role")
    public ResponseEntity<?> updateUserRole(@RequestBody ServiceUserRequest serviceUserRequest) {
        return vaultService.updateUserRole(serviceUserRequest);
    }

    @PostMapping("/add-user-to-service")
    public ResponseEntity<?> addUserToService(@RequestBody ServiceUserRequest serviceUserRequest) {
        return vaultService.addUserToService(serviceUserRequest);
    }

    @PostMapping("/delete-user-from-service")
    public ResponseEntity<?> deleteUserFromService(@RequestBody ServiceUserRequest serviceUserRequest) {
        return vaultService.deleteUserFromService(serviceUserRequest);
    }

    @PostMapping("/delete-user-from-all-services")
    public ResponseEntity<?> deleteUserFromAllServices(@RequestBody ServiceUserRequest serviceUserRequest) {
        return vaultService.deleteUserFromAllServices(serviceUserRequest);
    }

    @PostMapping("/bootstrap/bootstrap-user")
    public ResponseEntity<?> bootstrapUser(
            @RequestBody VaultRequest vaultRequest,
            @RequestHeader("X-VAULT-BOOTSTRAP-TOKEN") String bootstrapToken) {
        return vaultService.bootstrapUser(vaultRequest.getServiceName(), bootstrapToken);
    }

    @PostMapping("/cicd")
    public ResponseEntity<?> cicdGet(@RequestBody VaultRequest vaultRequest) {
        return vaultService.cicdGet(vaultRequest.getServiceName(), vaultRequest.getEnvironmentName());
    }// Integration function end: Auth
}
