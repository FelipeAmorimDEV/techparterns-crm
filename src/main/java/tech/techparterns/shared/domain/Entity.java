package tech.techparterns.shared.domain;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public class Entity {
    private final UUID id;
    private final Instant createdAt;
    private final Instant updatedAt;
    private OffsetDateTime deletedAt;

    public Entity(UUID id) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.deletedAt = null;
    }

    public Entity(UUID id, Instant createdAt, Instant updatedAt, OffsetDateTime deletedAt) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
        this.deletedAt = deletedAt;
    }

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        if (this.deletedAt != null) {
            throw new IllegalStateException("Entity already deleted");
        }
        this.deletedAt = deletedAt;
    }

    public void restaurar() {
        if (this.deletedAt == null) {
            throw new IllegalStateException("Entity already active");
        }
        this.deletedAt = null;
    }
}
