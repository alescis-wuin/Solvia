package fr.seynax.solvia.backend.api.calculation;

import java.util.UUID;

import fr.seynax.solvia.backend.api.money.MoneyResponse;

public record AccountValueResponse(UUID accountId, String accountName, MoneyResponse value) {
}
