package com.example.emailservice.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConfirmationTokenRepository extends JpaRepository<ConfirmationToken, UUID> {

    @Query("SELECT t FROM ConfirmationToken t WHERE t.token = ?1")
    Optional<ConfirmationToken> findByToken(UUID token);

    @Transactional
    @Modifying
    @Query("UPDATE ConfirmationToken t SET t.confirmedAt = ?2 WHERE t.token = ?1")
    int updateConfirmedAt(UUID token, LocalDateTime confirmedAt);

    @Transactional
    @Modifying
    @Query(value = "DELETE ConfirmationToken t WHERE t.token = ?1")
    int deleteToken(UUID token);
}
