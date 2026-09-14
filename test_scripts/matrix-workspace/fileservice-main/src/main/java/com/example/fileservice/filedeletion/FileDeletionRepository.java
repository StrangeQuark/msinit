package com.example.fileservice.filedeletion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FileDeletionRepository extends JpaRepository<FileDeletion, UUID> {
    Optional<FileDeletion> findByCollectionIdAndFileName(UUID collectionId, String fileName);
}
