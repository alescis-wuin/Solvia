package fr.seynax.solvia.backend.api.snapshot;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import fr.seynax.solvia.backend.api.money.MoneyResponse;
import fr.seynax.solvia.domain.model.AccountBalanceSnapshot;
import fr.seynax.solvia.domain.model.ValuationConfidence;

public record AccountSnapshotResponse(
        UUID id,
        UUID accountId,
        LocalDate valueDate,
        MoneyResponse balance,
        ValuationConfidence confidence,
        String note,
        Instant recordedAt
) {

    public static AccountSnapshotResponse from(AccountBalanceSnapshot snapshot) {
        return new AccountSnapshotResponse(
                snapshot.id(),
                snapshot.accountId(),
                snapshot.valueDate(),
                MoneyResponse.from(snapshot.balance()),
                snapshot.confidence(),
                snapshot.note(),
                snapshot.recordedAt()
        );
    }
}
