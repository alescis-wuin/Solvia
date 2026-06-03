package fr.seynax.solvia.backend.api.snapshot;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import fr.seynax.solvia.backend.api.money.MoneyRequest;
import fr.seynax.solvia.domain.model.ValuationConfidence;

public record AccountSnapshotCreateRequest(
        @NotNull UUID accountId,
        @NotNull LocalDate valueDate,
        @NotNull @Valid MoneyRequest balance,
        ValuationConfidence confidence,
        String note
) {
}
