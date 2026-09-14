package com.example.vaultservice.repositorytests;

import com.example.vaultservice.environment.Environment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

public class EnvironmentRepositoryTest extends BaseRepositoryTest {
    @Test
    void findByNameAndServiceIdTest() {
        Optional<Environment> response = environmentRepository.findByNameAndServiceId(testEnvironment.getName(), testService.getId());

        Assertions.assertTrue(response.isPresent());
    }

    @Test
    void serviceAndNameAreUniqueTest() {
        Assertions.assertThrows(DataIntegrityViolationException.class,
                () -> environmentRepository.saveAndFlush(
                        new Environment(testService, testEnvironment.getName())
                ));
    }
}
