package fr.seynax.solvia.backend.api.snapshot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import fr.seynax.solvia.backend.api.money.MoneyRequest;
import fr.seynax.solvia.domain.model.ValuationConfidence;

public record PositionSnapshotCreateRequest(
        @NotNull UUID positionId,
        @NotNull LocalDate valueDate,
        @NotNull @PositiveOrZero BigDecimal quantity,
        @NotNull @Valid MoneyRequest marketValue,
        ValuationConfidence confidence
) {
}
