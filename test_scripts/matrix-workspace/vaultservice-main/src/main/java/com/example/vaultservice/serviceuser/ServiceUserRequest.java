// Integration file: Auth

package com.example.vaultservice.serviceuser;

public class ServiceUserRequest {
    private String serviceName;
    private String username;
    private ServiceUserRole role;

    public ServiceUserRequest() {

    }

    public ServiceUserRequest(String serviceName, String username, ServiceUserRole role) {
        this.serviceName = serviceName;
        this.username = username;
        this.role = role;
    }


    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public ServiceUserRole getRole() {
        return role;
    }

    public void setRole(ServiceUserRole role) {
        this.role = role;
    }
}
