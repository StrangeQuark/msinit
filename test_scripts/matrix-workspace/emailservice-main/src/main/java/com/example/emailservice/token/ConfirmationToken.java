package com.example.emailservice.token;

import com.example.emailservice.utility.LocalDateTimeEncryptDecryptConverter;
import com.example.emailservice.utility.StringEncryptDecryptConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class ConfirmationToken {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable=false)
    private UUID token;

    @Column(nullable=false, updatable=false)
    @Convert(converter = StringEncryptDecryptConverter.class)
    private String purpose;

    @Convert(converter = StringEncryptDecryptConverter.class)
    private String email;

    @Column(nullable=false)
    @Convert(converter = LocalDateTimeEncryptDecryptConverter.class)
    private LocalDateTime createdAt;

    @Column(nullable=false)
    @Convert(converter = LocalDateTimeEncryptDecryptConverter.class)
    private LocalDateTime expiresAt;

    @Convert(converter = LocalDateTimeEncryptDecryptConverter.class)
    private LocalDateTime confirmedAt;

    public ConfirmationToken() {

    }

    public ConfirmationToken(UUID token, LocalDateTime createdAt, LocalDateTime expiresAt, String email, String purpose) {
        this.token = token;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.email = email;
        this.purpose = purpose;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getToken() {
        return token;
    }

    public void setToken(UUID token) {
        this.token = token;
    }

    public String getPurpose() {
        return purpose;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
