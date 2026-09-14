package com.example.vaultservice.repositorytests;

import com.example.vaultservice.variable.Variable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

public class VariableRepositoryTest extends BaseRepositoryTest {

    @Test
    void findByEnvironmentIdTest() {
        List<Variable> response  = variableRepository.findByEnvironmentId(testEnvironment.getId());

        Assertions.assertTrue(response.contains(testVariable));
    }

    @Test
    void findByEnvironmentServiceIdTest() {
        List<Variable> response  = variableRepository.findByEnvironmentServiceId(testService.getId());

        Assertions.assertTrue(response.contains(testVariable));
    }

    @Test
    void findByEnvironmentIdAndKeyTest() {
        Optional<Variable> response  = variableRepository.findByEnvironmentIdAndKey(testEnvironment.getId(), testVariable.getKey());

        Assertions.assertTrue(response.isPresent());
    }

    @Test
    void environmentAndKeyAreUniqueTest() {
        Assertions.assertThrows(DataIntegrityViolationException.class,
                () -> variableRepository.saveAndFlush(
                        new Variable(testEnvironment, testVariable.getKey(), "duplicateValue")
                ));
    }
}
