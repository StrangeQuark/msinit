// Integration file: Auth

package com.example.vaultservice.serviceuser;

import com.example.vaultservice.service.Service;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceUserRepository extends JpaRepository<ServiceUser, UUID> {
    Optional<ServiceUser> findByUserIdAndServiceId(UUID userId, UUID serviceId);

    @Query("SELECT su.service FROM ServiceUser su WHERE su.userId = :userId")
    List<Service> findServicesByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT su.service FROM ServiceUser su WHERE su.userId = :userId ORDER BY su.service.id")
    List<Service> findServicesByUserIdForUpdate(UUID userId);

    List<ServiceUser> findAllByServiceId(UUID serviceId);

    @Modifying
    @Transactional
    @Query("DELETE ServiceUser su WHERE su.userId = :userId AND su.service.id = :serviceId")
    int deleteServiceUser(UUID userId, UUID serviceId);
}
