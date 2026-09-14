package com.example.vaultservice.environment;

import com.example.vaultservice.variable.VariableResponse;

import java.util.List;

public class EnvironmentResponse {
    private String name;
    private List<VariableResponse> variables;

    public EnvironmentResponse(String name, List<VariableResponse> variables) {
        this.name = name;
        this.variables = variables;
    }

    public String getName() {
        return name;
    }

    public List<VariableResponse> getVariables() {
        return variables;
    }
}
