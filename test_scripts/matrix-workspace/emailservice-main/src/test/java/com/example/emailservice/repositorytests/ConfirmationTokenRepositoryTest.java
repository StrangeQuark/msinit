package com.example.emailservice.repositorytests;

import com.example.emailservice.token.ConfirmationToken;
import com.example.emailservice.token.ConfirmationTokenRepository;
import com.example.emailservice.token.TokenPurpose;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class ConfirmationTokenRepositoryTest extends BaseRepositoryTest {
    @Autowired
    private TestEntityManager testEntityManager;
    @Autowired
    private ConfirmationTokenRepository confirmationTokenRepository;

    UUID token;

    @BeforeEach
    void setup() {
        token = UUID.randomUUID();
        ConfirmationToken confirmationToken = new ConfirmationToken(token, LocalDateTime.now(), LocalDateTime.now().plusMinutes(15), "test@test.com", TokenPurpose.REGISTRATION.name());

        testEntityManager.persistAndFlush(confirmationToken);
    }

    @Test
    void findByTokenTest() {
        Optional<ConfirmationToken> confirmationToken = confirmationTokenRepository.findByToken(token);

        Assertions.assertTrue(confirmationToken.isPresent());
    }

    @Test
    void updateConfirmedAtTest() {
        LocalDateTime localDateTime = LocalDateTime.now().withNano((LocalDateTime.now().getNano() / 1_000_000) * 1_000_000);

        confirmationTokenRepository.updateConfirmedAt(token, localDateTime);

        testEntityManager.clear(); // Clear persistence context to ensure fresh fetch

        Optional<ConfirmationToken> confirmationToken = confirmationTokenRepository.findByToken(token);

        Assertions.assertEquals(confirmationToken.get().getConfirmedAt(), localDateTime);
    }

    @Test
    void deleteTokenTest() {
        confirmationTokenRepository.deleteToken(token);

        Assertions.assertTrue(confirmationTokenRepository.findByToken(token).isEmpty());
    }
}
