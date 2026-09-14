package com.example.vaultservice.service;

import java.util.List;

public class ServiceResponse {
    private String name;
    private List<String> environments;

    public ServiceResponse(String name, List<String> environments) {
        this.name = name;
        this.environments = environments;
    }

    public String getName() {
        return name;
    }

    public List<String> getEnvironments() {
        return environments;
    }
}
