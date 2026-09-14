package com.example.emailservice.email;

import org.springframework.http.ResponseEntity;

public interface EmailSender {
    ResponseEntity<?> send(String recipient, String sender, String email, String subject);
}
