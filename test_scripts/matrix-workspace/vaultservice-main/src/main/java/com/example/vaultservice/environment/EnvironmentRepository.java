package com.example.vaultservice.environment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnvironmentRepository extends JpaRepository<Environment, UUID> {
    Optional<Environment> findByNameAndServiceId(String name, UUID serviceId);
    List<Environment> findAllByServiceId(UUID serviceId);
}
