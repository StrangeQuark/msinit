package com.example.vaultservice.variable;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VariableRepository extends JpaRepository<Variable, UUID> {
    List<Variable> findByEnvironmentId(UUID environmentId);
    List<Variable> findByEnvironmentServiceId(UUID serviceId);
    Optional<Variable> findByEnvironmentIdAndKey(UUID environmentId, String key);
}
