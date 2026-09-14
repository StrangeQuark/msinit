package com.example.vaultservice.vault;

import com.example.vaultservice.variable.VariableRequest;

import java.util.List;

public class VaultRequest {
    private String serviceName;
    private String environmentName;
    private String variableName;
    private VariableRequest variable;
    private List<VariableRequest> variables;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public VariableRequest getVariable() {
        return variable;
    }

    public void setVariable(VariableRequest variable) {
        this.variable = variable;
    }

    public List<VariableRequest> getVariables() {
        return variables;
    }

    public void setVariables(List<VariableRequest> variables) {
        this.variables = variables;
    }
}
