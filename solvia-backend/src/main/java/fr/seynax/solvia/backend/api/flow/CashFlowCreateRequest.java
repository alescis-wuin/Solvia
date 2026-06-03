package fr.seynax.solvia.backend.api.flow;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import fr.seynax.solvia.backend.api.money.MoneyRequest;
import fr.seynax.solvia.domain.model.CashFlowType;

public record CashFlowCreateRequest(
        @NotNull UUID accountId,
        @NotNull CashFlowType type,
        @NotNull LocalDate valueDate,
        @NotNull @Valid MoneyRequest amount,
        String label
) {
}
