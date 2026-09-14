package com.example.vaultservice.repositorytests;

import com.example.vaultservice.service.Service;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public class ServiceRepositoryTest extends BaseRepositoryTest {

    @Test
    void findByNameTest() {
        Optional<Service> response = serviceRepository.findByName(testService.getName());

        Assertions.assertTrue(response.isPresent());
    }
}
