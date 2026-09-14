// Integration file: Auth

package com.example.vaultservice.serviceuser;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.example.vaultservice.service.Service;
import com.example.vaultservice.utility.RoleEncryptDecryptConverter;
import jakarta.persistence.Entity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "service_users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"service_id", "user_id"})
})
public class ServiceUser {

    public ServiceUser() {

    }

    public ServiceUser(Service service, UUID userId, ServiceUserRole role) {
        this.service = service;
        this.userId = userId;
        this.role = role;
    }

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    @JsonBackReference
    private Service service;

    private UUID userId;

    @Convert(converter = RoleEncryptDecryptConverter.class)
    private ServiceUserRole role;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public ServiceUserRole getRole() {
        return role;
    }

    public void setRole(ServiceUserRole role) {
        this.role = role;
    }
}
