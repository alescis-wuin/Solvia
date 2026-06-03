package fr.seynax.solvia.backend.api.snapshot;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import fr.seynax.solvia.backend.api.money.MoneyResponse;
import fr.seynax.solvia.domain.model.PositionSnapshot;
import fr.seynax.solvia.domain.model.ValuationConfidence;

public record PositionSnapshotResponse(
        UUID id,
        UUID positionId,
        LocalDate valueDate,
        BigDecimal quantity,
        MoneyResponse marketValue,
        ValuationConfidence confidence,
        Instant recordedAt
) {

    public static PositionSnapshotResponse from(PositionSnapshot snapshot) {
        return new PositionSnapshotResponse(
                snapshot.id(),
                snapshot.positionId(),
                snapshot.valueDate(),
                snapshot.quantity(),
                MoneyResponse.from(snapshot.marketValue()),
                snapshot.confidence(),
                snapshot.recordedAt()
        );
    }
}
