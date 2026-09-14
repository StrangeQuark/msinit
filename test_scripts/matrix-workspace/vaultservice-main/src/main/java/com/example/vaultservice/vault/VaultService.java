package com.example.vaultservice.vault;

import com.example.vaultservice.environment.Environment;
import com.example.vaultservice.environment.EnvironmentResponse;
import com.example.vaultservice.error.ErrorResponse;
import com.example.vaultservice.service.Service;
import com.example.vaultservice.service.ServiceResponse;
import com.example.vaultservice.serviceuser.ServiceUser;// Integration line: Auth
import com.example.vaultservice.serviceuser.ServiceUserRepository;// Integration line: Auth
import com.example.vaultservice.serviceuser.ServiceUserRequest;// Integration line: Auth
import com.example.vaultservice.serviceuser.ServiceUserRole;// Integration line: Auth
import com.example.vaultservice.serviceuser.ServiceUserResponse; // Integration line: Auth
import com.example.vaultservice.utility.AuthUtility;// Integration line: Auth
import com.example.vaultservice.utility.DotenvUtility;
import com.example.vaultservice.utility.JwtUtility;// Integration line: Auth
import com.example.vaultservice.utility.TelemetryUtility;// Integration line: Telemetry
import com.example.vaultservice.variable.Variable;
import com.example.vaultservice.variable.VariableRequest;
import com.example.vaultservice.variable.VariableResponse;
import com.example.vaultservice.environment.EnvironmentRepository;
import com.example.vaultservice.service.ServiceRepository;
import com.example.vaultservice.variable.VariableRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class VaultService {
    private static final Logger LOGGER = LoggerFactory.getLogger(VaultService.class);

    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private EnvironmentRepository environmentRepository;
    @Autowired
    private VariableRepository variableRepository;
    @Autowired
    private DotenvUtility dotenvUtility;
    // Integration function start: Auth
    @Autowired
    private ServiceUserRepository serviceUserRepository;
    @Autowired
    JwtUtility jwtUtility;
    @Autowired
    AuthUtility authUtility;
    @Value("${AUTH_CICD_TOKEN}")
    private String AUTH_CICD_TOKEN; // Integration line: Auth
    @Value("${EMAIL_CICD_TOKEN}")
    private String EMAIL_CICD_TOKEN; // Integration line: Email
    @Value("${FILE_CICD_TOKEN}")
    private String FILE_CICD_TOKEN; // Integration line: File
    @Value("${GATEWAY_CICD_TOKEN}")
    private String GATEWAY_CICD_TOKEN; // Integration line: Gateway
    @Value("${LOGGER_CICD_TOKEN}")
    private String LOGGER_CICD_TOKEN; // Integration line: Logger
    @Value("${REACT_CICD_TOKEN}")
    private String REACT_CICD_TOKEN; // Integration line: React
    @Value("${TELEMETRY_CICD_TOKEN}")
    private String TELEMETRY_CICD_TOKEN; // Integration line: Telemetry
    @Value("${BOOTSTRAP_TOKEN:}")
    private String BOOTSTRAP_TOKEN;
    // Integration function end: Auth
    // Integration function start: Telemetry
    @Autowired
    TelemetryUtility telemetryUtility;
    // Integration function end: Telemetry

    @Transactional
    public ResponseEntity<?> createService(String serviceName) {
        try {
            LOGGER.info("Attempting to create service");

            if(serviceRepository.findByName(serviceName).isPresent()) {
                LOGGER.error("Service creation failed - That service name already exists");
                return ResponseEntity.status(409).body(new ErrorResponse("Service with that name already exists"));
            }

            Service service = new Service();
            service.setName(serviceName);
            service.addUser(new ServiceUser(service, UUID.fromString(jwtUtility.extractId()), ServiceUserRole.OWNER));// Integration line: Auth

            serviceRepository.save(service);
            // Integration function start: Telemetry
            telemetryUtility.sendTelemetryEvent("vault-create-service", Map.of(
                            "userId", jwtUtility.extractId(), // Integration line: Auth
                            "service-id", service.getId(),
                            "service-name", service.getName()
                    )
            ); // Integration function end: Telemetry

            LOGGER.info("New service successfully created");
            return ResponseEntity.ok("New service successfully created");
        } catch (Exception ex) {
            LOGGER.error("Failed to create service: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(400).body(new ErrorResponse(ex.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<?> createEnvironment(String serviceName, String environmentName) {
        try {
            LOGGER.info("Attempting to create environment");

            Service service = serviceRepository.findByName(serviceName)
                    .orElseThrow(() -> new RuntimeException("Service not found"));

            //Integration function start: Auth
            ServiceUser requestingUser = serviceUserRepository.findByUserIdAndServiceId(UUID.fromString(jwtUtility.extractId()), service.getId())
                    .orElseThrow(() -> new RuntimeException("Requesting user does not have access to this service"));

            // Ensure that the request user has the OWNER or MAINTAINER role
            if ((requestingUser.getRole() != ServiceUserRole.OWNER && requestingUser.getRole() != ServiceUserRole.MANAGER)) {
                throw new RuntimeException("Only service users with OWNER or MANAGER roles can create environments");
            }
            //Integration function end: Auth
            if(environmentRepository.findByNameAndServiceId(environmentName, service.getId()).isPresent()) {
                LOGGER.error("Environment creation failed - An environment with that name already exists in this service");
                return ResponseEntity.status(409).body(new ErrorResponse("Environment with that name already exists in this service"));
            }

            Environment environment = new Environment();
            environment.setName(environmentName);
            environment.setService(service);
            environmentRepository.save(environment);
            // Integration function start: Telemetry
            telemetryUtility.sendTelemetryEvent("vault-create-environment", Map.of(
                            "userId", jwtUtility.extractId(), // Integration line: Auth
                            "service-id", service.getId(),
                            "service-name", service.getName(),
                            "environment-id", environment.getId(),
                            "environment-name", environment.getName()
                    )
            ); // Integration function end: Telemetry

            LOGGER.info("New environment successfully created");
            return ResponseEntity.ok(new EnvironmentResponse(environment.getName(), new ArrayList<>()));
        } catch (Exception ex) {
            LOGGER.error("Failed to create environment: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(400).body(new ErrorResponse(ex.getMessage()));
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> getService(String serviceName) {
        try {
            LOGGER.debug("Attempting to get service");

            Service service = serviceRepository.findByName(serviceName)
                    .orElseThrow(() -> new RuntimeException("Service not found"));

            //Integration function start: Auth
            serviceUserRepository.findByUserIdAndServiceId(UUID.fromString(jwtUtility.extractId()), service.getId())
                    .orElseThrow(() -> new RuntimeException("Requesting user does not have access to this service"));
            //Integration function end: Auth
            List<Environment> environments = environmentRepository.findAllByServiceId(service.getId());
            List<String> environmentNames = new ArrayList<>();
            for(Environment environment : environments)
                environmentNames.add(environment.getName());

            return ResponseEntity.ok(new ServiceResponse(service.getName(), environmentNames));
        } catch (Exception ex) {
            LOGGER.error("Failed to get service: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(400).body(new ErrorResponse(ex.getMessage()));
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> getEnvironmentsByService(String serviceName) {
        try {
            LOGGER.debug("Attempting to get environments by service");

            Service service = serviceRepository.findByName(serviceName)
                    .orElseThrow(() -> new RuntimeException("Service not found"));

            //Integration function start: Auth
            serviceUserRepository.findByUserIdAndServiceId(UUID.fromString(jwtUtility.extractId()), service.getId())
                    .orElseThrow(() -> new RuntimeException("Requesting user does not have access to this service"));
            //Integration function end: Auth
            List<Environment> environments = environmentRepository.findAllByServiceId(service.getId());

            List<String> environmentNames = new ArrayList<>();
            for(Environment env : environments)
                environmentNames.add(env.getName());

            LOGGER.debug("Successfully retrieved all environments by service");
            return ResponseEntity.ok(environmentNames);
        } catch (Exception ex) {
            LOGGER.error("Failed to get environments by service: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(400).body(new ErrorResponse(ex.getMessage()));
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> getEnvironment(String serviceName, String environmentName) {
        try {
            LOGGER.debug("Attempting to get environment");

            Service service = serviceRepository.findByName(serviceName)
                    .orElseThrow(() -> new RuntimeException("Service not found"));

            //Integration function start: Auth
            serviceUserRepository.findByUserIdAndServiceId(UUID.fromString(jwtUtility.extractId()), service.getId())
                    .orElseThrow(() -> new RuntimeException("Requesting user does not have access to this service"));
            //Integration function end: Auth
            Environment environment = environmentRepository.findByNameAndServiceId(environmentName, service.getId())
                    .orElseThrow(() -> new RuntimeException("Environment not found"));

            List<VariableResponse> variables = variableRepository.findByEnvironmentId(environment.getId())
                    .stream()
                    .map(VariableResponse::new)
                    .toList();

            return ResponseEntity.ok(new EnvironmentResponse(environment.getName(), variables));
        } catch (Exception ex) {
            LOGGER.error("Failed to get environment: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(400).body(new ErrorResponse(ex.getMessage()));
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> getVariablesByService(String serviceName) {
        try {
            LOGGER.debug("Attempting to get variables by service");

            Service service = serviceRepository.findByName(serviceName)
                    .orElseThrow(() -> new RuntimeException("Service not found"));

            //Integration function start: Auth
            serviceUserRepository.findByUserIdAndServiceId(UUID.fromString(jwtUtility.extractId()), service.getId())
                    .orElseThrow(() -> new RuntimeException("Requesting user does not have access to this service"));
            //Integration function end: Auth
            List<VariableResponse> variables = variableRepository.findByEnvironmentServiceId(service.getId())
                    .stream()
                    .map(VariableResponse::new)
                    .toList();

            return ResponseEntity.ok(variables);
        } catch (Exception ex) {
            LOGGER.error("Failed to get variables by service: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(400).body(new ErrorResponse(ex.getMessage()));
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> getVariablesByEnvironment(String serviceName, String environmentName) {
        try {
            LOGGER.debug("Attempting to get variables by environment");

            Service service = serviceRepository.findByName(serviceName)
                    .orElseThrow(() -> new RuntimeException("Service not found"));

            //Integration function start: Auth
            serviceUserRepository.findByUserIdAndServiceId(UUID.fromString(jwtUtility.extractId()), service.getId())
                    .orElseThrow(() -> new RuntimeException("Requesting user does not have access to this service"));
            //Integration function end: Auth
            Environment environment = environmentRepository.findByNameAndServiceId(environmentName, service.getId())
                    .orElseThrow(() -> new RuntimeException("Environment not found"));

            List<VariableResponse> variables = variableRepository.findByEnvironmentId(environment.getId())
                    .stream()
                    .map(VariableResponse::new)
                    .toList();

            return ResponseEntity.ok(variables);
        } catch (Exception ex) {
            LOGGER.error("Failed to get variables by environment: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(400).body(new ErrorResponse(ex.getMessage()));
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> getVariableByName(String serviceName, String environmentName, String variableName) {
        try {
            LOGGER.debug("Attempting to get variable by name");

            Service service = serviceRepository.findByName(serviceName)
                    .orElseThrow(() -> new RuntimeException("Service not found"));

            //Integration function start: Auth
            serviceUserRepository.findByUserIdAndServiceId(UUID.fromString(jwtUtility.extractId()), service.getId())
                    .orElseThrow(() -> new RuntimeException("Requesting user does not have access to this service"));
            //Integration function end: Auth
            Environment environment = environmentRepository.findByNameAndServiceId(environmentName, service.getId())
                    .orElseThrow(() -> new RuntimeException("Environment not found"));

            Variable variable = variableRepository.findByEnvironmentIdAndKey(environment.getId(), variableName)
                    .orElseThrow(() -> new RuntimeException("Variable not found"));

            return ResponseEntity.ok(new VariableResponse(variable));
        } catch (Exception ex) {
            LOGGER.error("Failed to get variable by name: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(400).body(new ErrorResponse(ex.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<?> addVariable(String serviceName, String environmentName, VariableRequest variableRequest) {
        try {
            LOGGER.info("Attempting to add variable");

            Service service = serviceRepository.findByName(serviceName)
                    .orElseThrow(() -> new RuntimeException("Service not found"));

            //Integration function start: Auth
            ServiceUser requestingUser = serviceUserRepository.findByUserIdAndServiceId(UUID.fromString(jwtUtility.extractId()), service.getId())
                    .orElseThrow(() -> new RuntimeException("Requesting user does not have access to this service"));
            //Integration function end: Auth
            Environment environment = environmentRepository.findByNameAndServiceId(environmentName, service.getId())
                    .orElseThrow(() -> new RuntimeException("Environment not found"));

            dotenvUtility.validateKeyAndValue(variableRequest.getKey(), variableRequest.getValue());

            //Ensure the variable name doesn't already exist
            if(variableRepository.findByEnvironmentIdAndKey(environment.getId(), variableRequest.getKey()).isPresent()) {
                LOGGER.error("Variable with that key already exists in this service/environment");
                return ResponseEntity.status(409).body(new ErrorResponse("Variable with that key already exists in this service/environment"));
            }

            Variable variable = new Variable();
            variable.setEnvironment(environment);
            variable.setKey(variableRequest.getKey());
            variable.setValue(variableRequest.getValue());
            variable.setLastUpdatedBy(requestingUser.getUserId());// Integration line: Auth
            variableRepository.save(variable);
            // Integration function start: Telemetry
            telemetryUtility.sendTelemetryEvent("vault-add-variable", Map.of(
                            "userId", jwtUtility.extractId(), // Integration line: Auth
                            "service-id", service.getId(),
                            "service-name", service.getName(),
                            "environment-id", environment.getId(),
                            "environment-name", environment.getName()
                    )
            ); // Integration function end: Telemetry

            LOGGER.info("New variable successfully added");
            return ResponseEntity.ok(new VariableResponse(variable));
        } catch (Exception ex) {
            LOGGER.error("Failed to add variable: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(400).body(new ErrorResponse(ex.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<?> updateVariable(String serviceName, String environmentName, VariableRequest variableRequest) {
        try {
            LOGGER.info("Attempting to update variable");

            Service service = serviceRepository.findByName(serviceName)
                    .orElseThrow(() -> new RuntimeException("Service not found"));

            //Integration function start: Auth
            ServiceUser requestingUser = serviceUserRepository.findByUserIdAndServiceId(UUID.fromString(jwtUtility.extractId()), service.getId())
                    .orElseThrow(() -> new RuntimeException("Requesting user does not have access to this service"));
            //Integration function end: Auth
            Environment environment = environmentRepository.findByNameAndServiceId(environmentName, service.getId())
                    .orElseThrow(() -> new RuntimeException("Environment not found"));

            dotenvUtility.validateKeyAndValue(variableRequest.getKey(), variableRequest.getValue());

            Variable var = variableRepository.findByEnvironmentIdAndKey(environment.getId(), variableRequest.getKey())
                    .orElseThrow(() -> new RuntimeException("Variable not found"));

            var.setValue(variableRequest.getValue());
            var.setLastUpdatedBy(requestingUser.getUserId());// Integration line: Auth
            variableRepository.save(var);
            // Integration function start: Telemetry
            telemetryUtility.sendTelemetryEvent("vault-update-variable", Map.of(
                            "userId", jwtUtility.extractId(), // Integration line: Auth
                            "service-id", service.getId(),
                            "service-name", service.getName(),
                            "environment-id", environment.getId(),
                            "environment-name", environment.getName()
                    )
            ); // Integration function end: Telemetry

            LOGGER.info("Variable successfully updated");
            return ResponseEntity.ok(new VariableResponse(var));
        } catch (Exception ex) {
            LOGGER.error("Failed to update variable: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(400).body(new ErrorResponse(ex.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<?> updateVariables(String serviceName, String environmentName, List<VariableRequest> variables) {
        try {
            LOGGER.info("Attempting to update list of variables");

            Service service = serviceRepository.findByName(serviceName)
                    .orElseThrow(() -> new RuntimeException("Service not found"));

            //Integration function start: Auth
            ServiceUser requestingUser = serviceUserRepository.findByUserIdAndServiceId(UUID.fromString(jwtUtility.extractId()), service.getId())
                    .orElseThrow(() -> new RuntimeException("Requesting user does not have access to this service"));
            //Integration function end: Auth
            Environment environment = environmentRepository.findByNameAndServiceId(environmentName, service.getId())
                    .orElseThrow(() -> new RuntimeException("Environment not found"));

            for(VariableRequest variable : variables)
                dotenvUtility.validateKeyAndValue(variable.getKey(), variable.getValue());

            List<String> skippedVars = new ArrayList<>();

            for(VariableRequest var : variables) {
                if(variableRepository.findByEnvironmentIdAndKey(environment.getId(), var.getKey()).isEmpty()) {
                    skippedVars.add(var.getKey());
                    continue;
                }

                Variable v = variableRepository.findByEnvironmentIdAndKey(environment.getId(), var.getKey()).get();

                v.setValue(var.getValue());
                v.setLastUpdatedBy(requestingUser.getUserId());// Integration line: Auth
                variableRepository.save(v);
            }
            // Integration function start: Telemetry
            telemetryUtility.sendTelemetryEvent("vault-update-variables", Map.of(
                            "userId", jwtUtility.extractId(), // Integration line: Auth
                            "service-id", service.getId(),
                            "service-name", service.getName(),
                            "environment-id", environment.getId(),
                            "environment-name", environment.getName(),
                            "updated-count", variables.size(),
                            "skipped-count", skippedVars.size()
                    )
            ); // Integration function end: Telemetry

            LOGGER.info("Variables successfully updated");
            return skippedVars.isEmpty() ? ResponseEntity.ok("All variables updated successfully") : ResponseEntity.ok("Skipped variables: " + skippedVars);
        } catch (Exception ex) {
            LOGGER.error("Failed to update multiple variables: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(400).body(new ErrorResponse(ex.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<?> addEnvFile(String serviceName, String environmentName, MultipartFile file) {
        LOGGER.info("Attempting to upload env file");

        try {
            Service service = serviceRepository.findByName(serviceName)
                    .orElseThrow(() -> new RuntimeException("Service not found"));

            //Integration function start: Auth
            ServiceUser requestingUser = serviceUserRepository.findByUserIdAndServiceId(UUID.fromString(jwtUtility.extractId()), service.getId())
                    .orElseThrow(() -> new RuntimeException("Requesting user does not have access to this service"));
            //Integration function end: Auth
            Environment environment = environmentRepository.findByNameAndServiceId(environmentName, service.getId())
                    .orElseThrow(() -> new RuntimeException("Environment not found"));

            String fileExtension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1);
            if (!fileExtension.equals("env")) {
                LOGGER.error("File extension is not .env");
                return ResponseEntity.status(400).body(new ErrorResponse("File extension is not .env"));
            }

            Map<String, String> importedVariables = dotenvUtility.parse(file.getInputStream());

            // Existing keys in decrypted form to skip duplicates
            Set<String> existingKeys = environment.getVariables().stream()
                    .map(var -> {
                        try {
                            return var.getKey();
                        } catch (Exception ex) {
                            LOGGER.error("Failed to decrypt a key while adding env file: " + ex.getMessage());
                            LOGGER.debug("Stack trace: ", ex);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            int added = 0;
            int skipped = 0;

            for(Map.Entry<String, String> importedVariable : importedVariables.entrySet()) {
                String key = importedVariable.getKey();
                String value = importedVariable.getValue();

                if (existingKeys.contains(key)) {
                    LOGGER.debug("Skipping existing variable");
                    skipped++;
                    continue;
                }

                Variable variable = new Variable();
                variable.setEnvironment(environment);
                variable.setKey(key);
                variable.setValue(value);
                variable.setLastUpdatedBy(requestingUser.getUserId());// Integration line: Auth
                variableRepository.save(variable);
                existingKeys.add(key);
                added++;
            }
            // Integration function start: Telemetry
            telemetryUtility.sendTelemetryEvent("vault-add-env-file", Map.of(
                            "userId", jwtUtility.extractId(), // Integration line: Auth
                            "service-id", service.getId(),
                            "service-name", service.getName(),
                            "environment-id", environment.getId(),
                            "environment-name", environment.getName(),
                            "added-count", added,
                            "skipped-count", skipped
                    )
            ); // Integration function end: Telemetry

            LOGGER.info("File processed: " + added + " variables added, " + skipped + " skipped.");
            return ResponseEntity.ok("Variables added: " + added + ", Skipped: " + skipped);
        } catch (IllegalArgumentException ex) {
            LOGGER.error("Failed to add env file: " + ex.getMessage());
            return ResponseEntity.status(400).body(new ErrorResponse(ex.getMessage()));
        } catch (Exception ex) {
            LOGGER.error("Failed to add env file: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(500).body(new ErrorResponse(ex.getMessage()));
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> downloadEnvFile(String serviceName, String environmentName) {
        try {
            LOGGER.info("Attempting to download .env file");

            Service service = serviceRepository.findByName(serviceName)
                    .orElseThrow(() -> new RuntimeException("Service not found"));

            //Integration function start: Auth
            serviceUserRepository.findByUserIdAndServiceId(UUID.fromString(jwtUtility.extractId()), service.getId())
                    .orElseThrow(() -> new RuntimeException("Requesting user does not have access to this service"));
            //Integration function end: Auth
            Environment environment = environmentRepository.findByNameAndServiceId(environmentName, service.getId())
                    .orElseThrow(() -> new RuntimeException("Environment not found"));

            List<Variable> decryptedVariables = variableRepository.findByEnvironmentId(environment.getId());

            // Generate .env content
            StringBuilder envContent = new StringBuilder();
            for(Variable variable : decryptedVariables)
                envContent.append(dotenvUtility.format(variable.getKey(), variable.getValue()));

            byte[] envBytes = envContent.toString().getBytes(StandardCharsets.UTF_8);
            ByteArrayResource resource = new ByteArrayResource(envBytes);

            String filename = serviceName + "-" + environmentName + ".env";
            // Integration function start: Telemetry
            telemetryUtility.sendTelemetryEvent("vault-download-env-file", Map.of(
                            "userId", jwtUtility.extractId(), // Integration line: Auth
                            "service-id", service.getId(),
                            "service-name", service.getName(),
                            "environment-id", environment.getId(),
                            "environment-name", environment.getName()
                    )
            ); // Integration function end: Telemetry

            LOGGER.info("Env file successfully downloaded");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(envBytes.length)
                    .body(resource);
        } catch (Exception ex) {
            LOGGER.error("Failed to download env file: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(500).body(new ErrorResponse(ex.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<?> deleteVariable(String serviceName, String environmentName, String variableName) {
        try {
            LOGGER.info("Attempting to delete variable");

            Service service = serviceRepository.findByName(serviceName)
                    .orElseThrow(() -> new RuntimeException("Service not found"));

            //Integration function start: Auth
            serviceUserRepository.findByUserIdAndServiceId(UUID.fromString(jwtUtility.extractId()), service.getId())
                    .orElseThrow(() -> new RuntimeException("Requesting user does not have access to this service"));
            //Integration function end: Auth
            Environment environment = environmentRepository.findByNameAndServiceId(environmentName, service.getId())
                    .orElseThrow(() -> new RuntimeException("Environment not found"));

            Variable variable = variableRepository.findByEnvironmentIdAndKey(environment.getId(), variableName)
                    .orElseThrow(() -> new RuntimeException("Variable not found"));

            variableRepository.deleteById(variable.getId());
            // Integration function start: Telemetry
            telemetryUtility.sendTelemetryEvent("vault-delete-variable", Map.of(
                            "userId", jwtUtility.extractId(), // Integration line: Auth
                            "service-id", service.getId(),
                            "service-name", service.getName(),
                            "environment-id", environment.getId(),
                            "environment-name", environment.getName()
                    )
            ); // Integration function end: Telemetry

            LOGGER.info("Variable successfully deleted");
            return ResponseEntity.ok("Variable successfully deleted");
        } catch (Exception ex) {
            LOGGER.error("Failed to delete variable: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(400).body(new ErrorResponse(ex.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<?> deleteEnvironment(String serviceName, String environmentName) {
        try {
            LOGGER.info("Attempting to delete environment");

            Service service = serviceRepository.findByName(serviceName)
                    .orElseThrow(() -> new RuntimeException("Service not found"));

            //Integration function start: Auth
            ServiceUser requestingUser = serviceUserRepository.findByUserIdAndServiceId(UUID.fromString(jwtUtility.extractId()), service.getId())
                    .orElseThrow(() -> new RuntimeException("Requesting user does not have access to this service"));

            // Ensure that the request user has the OWNER or MANAGER role
            if (requestingUser.getRole() != ServiceUserRole.OWNER && requestingUser.getRole() != ServiceUserRole.MANAGER) {
                throw new RuntimeException("Only service users with OWNER or MANAGER roles can delete environments");
            }
            //Integration function end: Auth
            Environment environment = environmentRepository.findByNameAndServiceId(environmentName, service.getId())
                    .orElseThrow(() -> new RuntimeException("Environment not found"));

            environmentRepository.delete(environment);
            // Integration function start: Telemetry
            telemetryUtility.sendTelemetryEvent("vault-delete-environment", Map.of(
                            "userId", jwtUtility.extractId(), // Integration line: Auth
                            "service-id", service.getId(),
                            "service-name", service.getName(),
                            "environment-id", environment.getId(),
                            "environment-name", environment.getName()
                    )
            ); // Integration function end: Telemetry

            LOGGER.info("Environment successfully deleted");
            return ResponseEntity.ok("Environment successfully deleted");
        } catch (Exception ex) {
            LOGGER.error("Failed to delete environment: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(400).body(new ErrorResponse(ex.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<?> deleteService(String serviceName) {
        try {
            LOGGER.info("Attempting to delete service");

            Service service = serviceRepository.findByNameForUpdate(serviceName)
                    .orElseThrow(() -> new RuntimeException("Service not found"));

            //Integration function start: Auth
            ServiceUser requestingUser = serviceUserRepository.findByUserIdAndServiceId(UUID.fromString(jwtUtility.extractId()), service.getId())
                    .orElseThrow(() -> new RuntimeException("Requesting user does not have access to this service"));

            // Ensure that the request user has the OWNER role
            if (requestingUser.getRole() != ServiceUserRole.OWNER) {
                throw new RuntimeException("Only service users with OWNER role can delete services");
            }
            //Integration function end: Auth
            serviceRepository.delete(service);
            // Integration function start: Telemetry
            telemetryUtility.sendTelemetryEvent("vault-delete-service", Map.of(
                            "userId", jwtUtility.extractId(), // Integration line: Auth
                            "service-id", service.getId(),
                            "service-name", service.getName()
                    )
            ); // Integration function end: Telemetry

            LOGGER.info("Service successfully deleted");
            return ResponseEntity.ok("Service successfully deleted");
        } catch (Exception ex) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            LOGGER.error("Failed to delete service: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(400).body(new ErrorResponse(ex.getMessage()));
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllServices() {
        LOGGER.debug("Attempting to get all services for user");

        try {
            List<Service> services = serviceRepository.findAll();
            services = serviceUserRepository.findServicesByUserId(UUID.fromString(jwtUtility.extractId())); // Integration line: Auth

            List<String> serviceNames = new ArrayList<>();
            for(Service service : services)
                serviceNames.add(service.getName());

            LOGGER.debug("Service retrieval successful");
            return ResponseEntity.ok(serviceNames);
        } catch (Exception ex) {
            LOGGER.error("Failed to get all services: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(400).body(new ErrorResponse(ex.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<?> bootstrapEnvFile(String serviceName, String environmentName, MultipartFile file, String bootstrapToken) {
        LOGGER.info("Attempting to bootstrap env file");

        try {
            if(BOOTSTRAP_TOKEN.isBlank() || !BOOTSTRAP_TOKEN.equals(bootstrapToken)) {
                LOGGER.error("Invalid bootstrap token");
                return ResponseEntity.status(400).body(new ErrorResponse("Invalid bootstrap token"));
            }

            String fileExtension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1);
            if (!fileExtension.equals("env")) {
                LOGGER.error("File extension is not .env");
                return ResponseEntity.status(400).body(new ErrorResponse("File extension is not .env"));
            }

            Map<String, String> importedVariables = dotenvUtility.parse(file.getInputStream());

            Service service;
            Optional<Service> optionalService = serviceRepository.findByName(serviceName);

            if(optionalService.isPresent()) {
                service = optionalService.get();
            } else {
                service = new Service();
                service.setName(serviceName);
                serviceRepository.save(service);
            }

            if(environmentRepository.findByNameAndServiceId(environmentName, service.getId()).isPresent()) {
                LOGGER.error("Environment creation failed during bootstrap - That environment name already exists");
                return ResponseEntity.status(409).body(new ErrorResponse("Environment with that name already exists"));
            }

            // Create the env
            Environment environment = new Environment();
            environment.setName(environmentName);
            environment.setService(service);
            environmentRepository.save(environment);

            for(Map.Entry<String, String> importedVariable : importedVariables.entrySet()) {
                Variable variable = new Variable();
                variable.setEnvironment(environment);
                variable.setKey(importedVariable.getKey());
                variable.setValue(importedVariable.getValue());
                variableRepository.save(variable);
            }
            // Integration function start: Telemetry
            telemetryUtility.sendTelemetryEvent("vault-bootstrap-env-file", Map.of(
                            "service-id", service.getId(),
                            "service-name", service.getName(),
                            "environment-id", environment.getId(),
                            "environment-name", environment.getName()
                    )
            ); // Integration function end: Telemetry

            LOGGER.info("Env file successfully bootstrapped");
            return ResponseEntity.ok("Env file successfully bootstrapped");
        } catch (IllegalArgumentException ex) {
            LOGGER.error("Failed to bootstrap env file: " + ex.getMessage());
            return ResponseEntity.status(400).body(new ErrorResponse(ex.getMessage()));
        } catch (Exception ex) {
            LOGGER.error("Failed to bootstrap env file: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(500).body(new ErrorResponse(ex.getMessage()));
        }
    }
    // Integration function start: Auth
    @Transactional(readOnly = true)
    public ResponseEntity<?> getUsersByService(String serviceName) {
        LOGGER.debug("Attempting to get all users for service");

        try {
            Service service = serviceRepository.findByName(serviceName)
                    .orElseThrow(() -> new RuntimeException("Service not found"));

            ServiceUser requestingUser = serviceUserRepository.findByUserIdAndServiceId(UUID.fromString(jwtUtility.extractId()), service.getId())
                    .orElseThrow(() -> new RuntimeException("Requesting user does not have access to this service"));

            if(requestingUser.getRole() != ServiceUserRole.OWNER && requestingUser.getRole() != ServiceUserRole.MANAGER)
                throw new RuntimeException("Only service users with OWNER or MANAGER roles can view service users");

            List<ServiceUserResponse> users = serviceUserRepository.findAllByServiceId(service.getId())
                    .stream()
                    .map(user -> new ServiceUserResponse(user.getUserId(), user.getRole()))
                    .toList();

            LOGGER.debug("User list retrieval successful");
            return ResponseEntity.ok(users);
        } catch (Exception ex) {
            LOGGER.error("Failed to get users by service: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(400).body(new ErrorResponse(ex.getMessage()));
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllRoles() {
        LOGGER.debug("Attempting to get all roles");

        return ResponseEntity.ok(ServiceUserRole.values());
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> getCurrentUserRole(String serviceName) {
        LOGGER.debug("Attempting to get current user's role");

        try {
            Service service = serviceRepository.findByName(serviceName)
                    .orElseThrow(() -> new RuntimeException("Service with this name does not exist"));

            ServiceUser requestingUser = serviceUserRepository.findByUserIdAndServiceId(UUID.fromString(jwtUtility.extractId()), service.getId())
                    .orElseThrow(() -> new RuntimeException("Requesting user does not have access to this service"));

            LOGGER.debug("Successfully retrieved current user's role");
            return ResponseEntity.ok(requestingUser.getRole());
        } catch (Exception ex) {
            LOGGER.error("Failed to get current user role: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(400).body(new ErrorResponse(ex.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<?> updateUserRole(ServiceUserRequest serviceUserRequest) {
        LOGGER.info("Attempting to update user's role");

        try {
            Service service = serviceRepository.findByNameForUpdate(serviceUserRequest.getServiceName())
                    .orElseThrow(() -> new RuntimeException("Service with this name does not exist"));

            ServiceUser requestingUser = serviceUserRepository.findByUserIdAndServiceId(UUID.fromString(jwtUtility.extractId()), service.getId())
                    .orElseThrow(() -> new RuntimeException("Requesting user does not have access to this service"));

            // Ensure that the request user has the OWNER or MANAGER role
            if (requestingUser.getRole() != ServiceUserRole.OWNER && requestingUser.getRole() != ServiceUserRole.MANAGER) {
                throw new RuntimeException("Only service users with OWNER or MANAGER roles can update user roles");
            }

            // Ensure only OWNER users can promote other users to OWNER
            if (serviceUserRequest.getRole() == ServiceUserRole.OWNER && requestingUser.getRole() != ServiceUserRole.OWNER) {
                throw new RuntimeException("Only OWNERs can promote other users to OWNER");
            }

            // Ensure the target user exists
            String userIdStr = authUtility.getUserId(serviceUserRequest.getUsername());
            if (userIdStr == null) {
                throw new RuntimeException("Unable to retrieve user id");
            }
            UUID userId = UUID.fromString(userIdStr);

            ServiceUser targetUser = serviceUserRepository.findByUserIdAndServiceId(userId, service.getId())
                    .orElseThrow(() -> new RuntimeException("Target user is not part of this service"));

            // If the target user is OWNER, requesting user must also be OWNER
            if(targetUser.getRole() == ServiceUserRole.OWNER && requestingUser.getRole() != ServiceUserRole.OWNER) {
                throw new RuntimeException("Only OWNERs can change the roles of other OWNERs");
            }

            // If the target user has an OWNER role, we must ensure that we're not removing the last OWNER from the service
            if(targetUser.getRole() == ServiceUserRole.OWNER) {
                long ownerCount = service.getServiceUsers().stream()
                        .filter(su -> su.getRole() == ServiceUserRole.OWNER)
                        .count();

                if (ownerCount <= 1) {
                    throw new RuntimeException("Cannot remove the last OWNER from the service.");
                }
            }

            //Update the target user's role
            targetUser.setRole(serviceUserRequest.getRole());
            serviceUserRepository.save(targetUser);
            // Integration function start: Telemetry
            telemetryUtility.sendTelemetryEvent("vault-update-user-role", Map.of(
                            "userId", jwtUtility.extractId(), // Integration line: Auth
                            "service-id", service.getId(),
                            "service-name", service.getName(),
                            "role", serviceUserRequest.getRole().name()
                    )
            ); // Integration function end: Telemetry

            LOGGER.info("User role successfully updated");
            return ResponseEntity.ok("User role successfully updated");
        } catch (Exception ex) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            LOGGER.error("Failed to update user role: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(400).body(new ErrorResponse(ex.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<?> addUserToService(ServiceUserRequest serviceUserRequest) {
        LOGGER.info("Attempting to add user to service");

        try {
            Service service = serviceRepository.findByNameForUpdate(serviceUserRequest.getServiceName())
                    .orElseThrow(() -> new RuntimeException("Service with this name does not exist"));

            ServiceUser requestingUser = serviceUserRepository.findByUserIdAndServiceId(UUID.fromString(jwtUtility.extractId()), service.getId())
                    .orElseThrow(() -> new RuntimeException("Requesting user does not have access to this service"));

            // Ensure that the request user has the OWNER role
            if (requestingUser.getRole() != ServiceUserRole.OWNER) {
                throw new RuntimeException("Only service users with OWNER role can add users to services");
            }

            // Ensure the target user exists
            String userIdStr = authUtility.getUserId(serviceUserRequest.getUsername());
            if (userIdStr == null) {
                throw new RuntimeException("Unable to retrieve user id");
            }
            UUID userId = UUID.fromString(userIdStr);

            // Avoid duplicate users
            if(serviceUserRepository.findByUserIdAndServiceId(userId, service.getId()).isPresent())
                return ResponseEntity.status(409).body(new ErrorResponse("User is already part of this service"));

            service.addUser(new ServiceUser(service, userId, serviceUserRequest.getRole()));

            serviceRepository.save(service);
            // Integration function start: Telemetry
            telemetryUtility.sendTelemetryEvent("vault-add-user-to-service", Map.of(
                            "userId", jwtUtility.extractId(), // Integration line: Auth
                            "service-id", service.getId(),
                            "service-name", service.getName()
                    )
            ); // Integration function end: Telemetry

            LOGGER.info("User successfully added to service");
            return ResponseEntity.ok("User successfully added to service");
        } catch (Exception ex) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            LOGGER.error("Failed to add user to service: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(400).body(new ErrorResponse(ex.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<?> deleteUserFromService(ServiceUserRequest serviceUserRequest) {
        LOGGER.info("Attempting to delete user from service");

        try {
            Service service = serviceRepository.findByNameForUpdate(serviceUserRequest.getServiceName())
                    .orElseThrow(() -> new RuntimeException("Service with this name does not exist"));

            ServiceUser requestingUser = serviceUserRepository.findByUserIdAndServiceId(UUID.fromString(jwtUtility.extractId()), service.getId())
                    .orElseThrow(() -> new RuntimeException("Requesting user does not have access to this service"));

            // Ensure the target user exists
            String userIdStr = authUtility.getUserId(serviceUserRequest.getUsername());
            if (userIdStr == null) {
                throw new RuntimeException("Unable to retrieve user id");
            }
            UUID userId = UUID.fromString(userIdStr);

            ServiceUser targetUser = serviceUserRepository.findByUserIdAndServiceId(userId, service.getId())
                    .orElseThrow(() -> new RuntimeException("Target user is not part of this service"));

            // Check if the requesting user is either attempting to remove self or is an OWNER
            if(!requestingUser.getUserId().equals(targetUser.getUserId()) && requestingUser.getRole() != ServiceUserRole.OWNER) {
                throw new RuntimeException("Only OWNER users can remove others");
            }

            // If the target user has an OWNER role, we must ensure that we're not removing the last OWNER from the service
            if(targetUser.getRole() == ServiceUserRole.OWNER) {
                long ownerCount = service.getServiceUsers().stream()
                        .filter(su -> su.getRole() == ServiceUserRole.OWNER)
                        .count();

                if (ownerCount <= 1) {
                    throw new RuntimeException("Cannot remove the last OWNER from the service.");
                }
            }

            int deletedCount = serviceUserRepository.deleteServiceUser(userId, service.getId());

            if(deletedCount == 0)
                throw new RuntimeException("User was not successfully deleted from service");

            // Integration function start: Telemetry
            telemetryUtility.sendTelemetryEvent("vault-delete-user-from-service", Map.of(
                            "userId", jwtUtility.extractId(), // Integration line: Auth
                            "service-id", service.getId(),
                            "service-name", service.getName()
                    )
            ); // Integration function end: Telemetry
            LOGGER.info("User successfully deleted from service");
            return ResponseEntity.ok("User successfully deleted from service");
        } catch(RuntimeException ex) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            LOGGER.error("Failed to delete user from service: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(400).body(new ErrorResponse(ex.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<?> deleteUserFromAllServices(ServiceUserRequest serviceUserRequest) {
        LOGGER.info("Attempting to delete user from all services");

        try {
            // Ensure the target user exists
            String userIdStr = authUtility.getUserId(serviceUserRequest.getUsername());
            if (userIdStr == null) {
                throw new RuntimeException("Unable to retrieve user id");
            }
            UUID userId = UUID.fromString(userIdStr);

            List<Service> services = serviceUserRepository.findServicesByUserIdForUpdate(userId);

            List<Map<String, String>> errors = new ArrayList<>();
            List<Service> servicesToDelete = new ArrayList<>();
            boolean authService = jwtUtility.isAuthService();

            for(Service service : services) {
                ServiceUser targetUser = serviceUserRepository.findByUserIdAndServiceId(userId, service.getId())
                        .orElseGet(() -> {
                            errors.add(Map.of(service.getName(), "Target user is not part of this service"));
                            return null;
                        });

                if(targetUser == null)
                    continue;

                if(!authService) {
                    ServiceUser requestingUser = serviceUserRepository.findByUserIdAndServiceId(UUID.fromString(jwtUtility.extractId()), service.getId())
                            .orElseGet(() -> {
                                errors.add(Map.of(service.getName(), "Requesting user does not have access to this service"));
                                return null;
                            });

                    if(requestingUser == null)
                        continue;

                // Check if the requesting user is either attempting to remove self or is an OWNER
                    if(!requestingUser.getUserId().equals(targetUser.getUserId()) && requestingUser.getRole() != ServiceUserRole.OWNER) {
                        errors.add(Map.of(service.getName(), "Only OWNER users can remove others"));
                        continue;
                    }
                }

                // If the target user has an OWNER role, we must ensure that we're not removing the last OWNER from the service
                if(targetUser.getRole() == ServiceUserRole.OWNER) {
                    long ownerCount = service.getServiceUsers().stream()
                            .filter(su -> su.getRole() == ServiceUserRole.OWNER)
                            .count();

                    if (ownerCount <= 1) {
                        // If the user being deleted is the only user in the service, just delete the service
                        if(service.getServiceUsers().size() == 1)
                            servicesToDelete.add(service);
                        else
                            errors.add(Map.of(service.getName(), "Cannot remove the last OWNER from the service"));
                    }
                }
            }

            // If there were errors, return
            if(!errors.isEmpty()) {
                LOGGER.error("Error when trying to remove user from all services");
                return ResponseEntity.status(400).body(errors);
            }

            for(Service service : servicesToDelete)
                serviceRepository.delete(service);

            for(Service service : services) {
                if(!servicesToDelete.contains(service))
                    serviceUserRepository.deleteServiceUser(userId, service.getId());
            }
            // Integration function start: Telemetry
            telemetryUtility.sendTelemetryEvent("vault-delete-user-from-all-services", Map.of(
                            "userId", jwtUtility.extractId(), // Integration line: Auth
                            "services-count", services.size()
                    )
            ); // Integration function end: Telemetry

            LOGGER.info("User successfully deleted from all services");
            return ResponseEntity.ok("User successfully deleted from all services");
        } catch(RuntimeException ex) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            LOGGER.error("Failed to delete user from all services: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(400).body(new ErrorResponse(ex.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<?> bootstrapUser(String serviceName, String bootstrapToken) {
        LOGGER.info("Attempting to bootstrap user to service");

        try {
            if(BOOTSTRAP_TOKEN.isBlank() || !BOOTSTRAP_TOKEN.equals(bootstrapToken)) {
                LOGGER.error("Invalid bootstrap token");
                return ResponseEntity.status(400).body(new ErrorResponse("Invalid bootstrap token"));
            }

            Service service = serviceRepository.findByName(serviceName)
                    .orElseThrow(() -> new RuntimeException("Service with this name does not exist"));

            if(serviceUserRepository.findByUserIdAndServiceId(UUID.fromString(jwtUtility.extractId()), service.getId()).isPresent())
                return ResponseEntity.status(409).body(
                        new ErrorResponse("Unable to bootstrap user - user already belongs to service")
                );

            // Add the requesting user to the bootstrapped service as the owner
            service.addUser(new ServiceUser(service, UUID.fromString(jwtUtility.extractId()), ServiceUserRole.OWNER));

            serviceRepository.save(service);
            // Integration function start: Telemetry
            telemetryUtility.sendTelemetryEvent("vault-bootstrap-user-to-service", Map.of(
                            "userId", jwtUtility.extractId(),
                            "service-id", service.getId(),
                            "service-name", service.getName()
                    )
            ); // Integration function end: Telemetry

            LOGGER.info("User successfully bootstrapped to service");
            return ResponseEntity.ok("User successfully bootstrapped to service");
        } catch (Exception ex) {
            LOGGER.error("Failed to bootstrap user to service: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(400).body(new ErrorResponse(ex.getMessage()));
        }
    }

    public ResponseEntity<?> cicdGet(String serviceName, String environmentName) {
        try {
            LOGGER.info("Attempting to CICD get");

            // Get the API token from the header
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                throw new IllegalStateException("No request context available");
            }

            HttpServletRequest request = attrs.getRequest();
            String cicdToken = request.getHeader("X-CICD-TOKEN");

            if(!validateCicdToken(serviceName, cicdToken))
                return ResponseEntity.status(403).body(new ErrorResponse("Invalid CICD request token"));

            Service service = serviceRepository.findByName(serviceName)
                    .orElseThrow(() -> new RuntimeException("Service not found"));

            Environment environment = environmentRepository.findByNameAndServiceId(environmentName, service.getId())
                    .orElseThrow(() -> new RuntimeException("Environment not found"));

            List<Variable> variables = variableRepository.findByEnvironmentId(environment.getId());
            if(variables.isEmpty()) {
                throw new RuntimeException("No environment variables found");
            }

            String envFile = "";

            for(Variable variable : variables) {
                envFile += dotenvUtility.format(variable.getKey(), variable.getValue());
            }

            LOGGER.info("CICD get success");
            return ResponseEntity.ok(envFile);
        } catch (Exception ex) {
            LOGGER.error("CICD get failure: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return ResponseEntity.status(400).body(new ErrorResponse(ex.getMessage()));
        }
    }

    private boolean validateCicdToken(String serviceName, String cicdToken) {
        if(cicdToken == null)
            return false;

        if(serviceName.equals("authservice"))
            return cicdToken.equals(AUTH_CICD_TOKEN); // Integration line: Auth
        if(serviceName.equals("emailservice"))
            return cicdToken.equals(EMAIL_CICD_TOKEN); // Integration line: Email
        if(serviceName.equals("fileservice"))
            return cicdToken.equals(FILE_CICD_TOKEN); // Integration line: File
        if(serviceName.equals("gatewayservice"))
            return cicdToken.equals(GATEWAY_CICD_TOKEN); // Integration line: Gateway
        if(serviceName.equals("loggerservice"))
            return cicdToken.equals(LOGGER_CICD_TOKEN); // Integration line: Logger
        if(serviceName.equals("reactservice"))
            return cicdToken.equals(REACT_CICD_TOKEN); // Integration line: React
        if(serviceName.equals("telemetryservice"))
            return cicdToken.equals(TELEMETRY_CICD_TOKEN); // Integration line: Telemetry

        return false;
    }// Integration function end: Auth
}
