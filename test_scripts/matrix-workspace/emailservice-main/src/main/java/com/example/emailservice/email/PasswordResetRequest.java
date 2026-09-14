package com.example.emailservice.email;

import java.util.UUID;

public class PasswordResetRequest {
    private UUID token;
    private String newPassword;

    public UUID getToken() {
        return token;
    }

    public void setToken(UUID token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
