package com.example.emailservice.template;

import com.example.emailservice.utility.LocalDateTimeEncryptDecryptConverter;
import com.example.emailservice.utility.StringEncryptDecryptConverter;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class EmailTemplate {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable=false)
    @Convert(converter = StringEncryptDecryptConverter.class)
    private String name;

    @Column(nullable=false)
    @Convert(converter = StringEncryptDecryptConverter.class)
    private String subject;

    @Column(columnDefinition = "TEXT", nullable = false)
    @Convert(converter = StringEncryptDecryptConverter.class)
    private String body;

    @Column(updatable=false)
    @Convert(converter = StringEncryptDecryptConverter.class)
    private String tokenPurpose;

    @Column(nullable=false)
    @Convert(converter = LocalDateTimeEncryptDecryptConverter.class)
    private LocalDateTime createdAt;

    @Convert(converter = LocalDateTimeEncryptDecryptConverter.class)
    private LocalDateTime updatedAt;

    public EmailTemplate() {

    }

    public EmailTemplate(String name, String subject, String body) {
        this(name, subject, body, null);
    }

    public EmailTemplate(String name, String subject, String body, String tokenPurpose) {
        this.name = name;
        this.subject = subject;
        this.body = body;
        this.tokenPurpose = tokenPurpose;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getTokenPurpose() {
        return tokenPurpose;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
