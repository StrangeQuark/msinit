package com.example.emailservice.email;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/email")
@CrossOrigin
public class EmailController {
    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping(path = "/send-email")
    public ResponseEntity<?> sendEmail(@RequestBody EmailRequest request) {
        return emailService.sendEmail(request, true);
    }

    @PostMapping(path = "/get-template-email")
    public ResponseEntity<?> getTemplateEmail(@RequestBody EmailRequest emailRequest) {
        return emailService.getTemplateEmail(emailRequest.getTemplateName(), true);
    }

    @GetMapping(path = "/get-all-template-emails")
    public ResponseEntity<?> getAllTemplateEmails() {
        return emailService.getAllTemplateEmails(true);
    }

    @PostMapping(path = "/send-template-email")
    public ResponseEntity<?> sendTemplateEmail(@RequestBody EmailRequest request) {
        return emailService.sendTemplateEmail(request, true);
    }

    @PostMapping(path = "/create-template-email")
    public ResponseEntity<?> createTemplateEmail(@RequestBody EmailRequest request) {
        return emailService.createTemplateEmail(request, true);
    }

    @PutMapping(path = "/update-template-email")
    public ResponseEntity<?> updateTemplateEmail(@RequestBody EmailRequest request) {
        return emailService.updateTemplateEmail(request, true);
    }

    @DeleteMapping(path = "/delete-template-email")
    public ResponseEntity<?> deleteTemplateEmail(@RequestBody EmailRequest emailRequest) {
        return emailService.deleteTemplateEmail(emailRequest.getTemplateName(), true);
    }

    @GetMapping(path = "/confirm-token")
    public ResponseEntity<?> confirmToken(@RequestParam("token") UUID token) {
        return emailService.confirmToken(token);
    }
    @GetMapping(path = "/enable-user")
    public ResponseEntity<?> enableUser(@RequestParam("token") UUID token) {
        return emailService.enableUser(token);
    }

    @PostMapping(path = "/reset-user-password")
    public ResponseEntity<?> resetUserPassword(@RequestBody PasswordResetRequest request) {
        return emailService.resetUserPassword(request.getToken(), request.getNewPassword());
    }
}
