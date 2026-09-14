package com.example.emailservice.email;

import java.util.Map;

public class EmailRequest {
    private String recipient;
    private String sender;
    private String body;
    private String subject;
    private boolean includeToken;
    private String templateName;
    private String tokenPurpose;
    private Map<String, String> templateVariables;

    public EmailRequest() {

    }

    public EmailRequest(String body, String subject) {
        this.body = body;
        this.subject = subject;
    }

    public EmailRequest(String body, String subject, String templateName) {
        this(body, subject);
        this.templateName = templateName;
    }

    public EmailRequest(String recipient, String sender, String body, String subject, boolean includeToken) {
        this(body, subject);
        this.recipient = recipient;
        this.sender = sender;
        this.includeToken = includeToken;
    }

    public EmailRequest(String recipient, String sender, boolean includeToken, String templateName, Map<String, String> templateVariables) {
        this.recipient = recipient;
        this.sender = sender;
        this.includeToken = includeToken;
        this.templateName = templateName;
        this.templateVariables = templateVariables;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public boolean getIncludeToken() {
        return includeToken;
    }

    public void setIncludeToken(boolean includeToken) {
        this.includeToken = includeToken;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTokenPurpose() {
        return tokenPurpose;
    }

    public void setTokenPurpose(String tokenPurpose) {
        this.tokenPurpose = tokenPurpose;
    }

    public Map<String, String> getTemplateVariables() {
        return templateVariables;
    }

    public void setTemplateVariables(Map<String, String> templateVariables) {
        this.templateVariables = templateVariables;
    }
}
