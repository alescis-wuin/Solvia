package fr.seynax.solvia.backend.api.position;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record PositionCreateRequest(
        @NotNull UUID accountId,
        @NotNull UUID assetId,
        @NotNull @PositiveOrZero BigDecimal quantity
) {
}
