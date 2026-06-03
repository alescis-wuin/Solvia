package fr.seynax.solvia.infrastructure.persistence.audit;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import fr.seynax.solvia.domain.common.DomainValidation;

public record AuditLogEntry(
        UUID id,
        Instant occurredAt,
        String actor,
        String eventName,
        String entityType,
        UUID entityId,
        String detailsJson
) {

    public AuditLogEntry {
        id = DomainValidation.requireId(id, "audit log id");
        occurredAt = DomainValidation.requireInstant(occurredAt, "audit log occurrence date");
        actor = DomainValidation.requireNonBlank(actor, "audit log actor");
        eventName = DomainValidation.requireNonBlank(eventName, "audit log event name");
        entityType = DomainValidation.requireNonBlank(entityType, "audit log entity type");
        detailsJson = Objects.requireNonNullElse(detailsJson, "{}");
    }

    public static AuditLogEntry create(String actor, String eventName, String entityType, UUID entityId, String detailsJson) {
        return new AuditLogEntry(UUID.randomUUID(), Instant.now(), actor, eventName, entityType, entityId, detailsJson);
    }
}
