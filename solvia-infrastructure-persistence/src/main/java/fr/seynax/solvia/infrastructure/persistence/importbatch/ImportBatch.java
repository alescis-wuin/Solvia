package fr.seynax.solvia.infrastructure.persistence.importbatch;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import fr.seynax.solvia.domain.common.DomainValidation;

public record ImportBatch(
        UUID id,
        ImportFormat format,
        String sourceName,
        Instant importedAt,
        int itemCount,
        ImportStatus status,
        String errorMessage
) {

    public ImportBatch {
        id = DomainValidation.requireId(id, "import batch id");
        format = Objects.requireNonNull(format, "import format is required");
        sourceName = sourceName == null ? null : sourceName.strip();
        importedAt = DomainValidation.requireInstant(importedAt, "import date");
        if (itemCount < 0) {
            throw new IllegalArgumentException("import item count must not be negative");
        }
        status = Objects.requireNonNull(status, "import status is required");
        errorMessage = errorMessage == null ? null : errorMessage.strip();
    }

    public static ImportBatch imported(ImportFormat format, String sourceName, int itemCount) {
        return new ImportBatch(UUID.randomUUID(), format, sourceName, Instant.now(), itemCount, ImportStatus.IMPORTED, null);
    }

    public static ImportBatch failed(ImportFormat format, String sourceName, String errorMessage) {
        return new ImportBatch(UUID.randomUUID(), format, sourceName, Instant.now(), 0, ImportStatus.FAILED, errorMessage);
    }
}
