package fr.seynax.solvia.backend.api.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import fr.seynax.solvia.domain.model.AccountType;
import fr.seynax.solvia.domain.model.EnvelopeType;

public record AccountCreateRequest(
        @NotBlank String name,
        @NotNull AccountType type,
        @NotNull EnvelopeType envelopeType,
        @NotBlank String currencyCode
) {
}
