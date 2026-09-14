package com.example.vaultservice.variable;

public class VariableResponse {
    private String key;
    private String value;

    public VariableResponse(Variable variable) {
        this.key = variable.getKey();
        this.value = variable.getValue();
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
