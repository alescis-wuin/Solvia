package fr.seynax.solvia.backend.api.position;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record PositionUpdateRequest(
        @NotNull @PositiveOrZero BigDecimal quantity,
        boolean active
) {
}
