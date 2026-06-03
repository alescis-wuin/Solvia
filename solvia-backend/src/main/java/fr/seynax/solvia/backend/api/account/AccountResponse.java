package fr.seynax.solvia.backend.api.account;

import java.time.Instant;
import java.util.UUID;

import fr.seynax.solvia.domain.model.Account;
import fr.seynax.solvia.domain.model.AccountType;
import fr.seynax.solvia.domain.model.EnvelopeType;

public record AccountResponse(
        UUID id,
        String name,
        AccountType type,
        EnvelopeType envelopeType,
        String currencyCode,
        boolean active,
        Instant createdAt
) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.id(),
                account.name(),
                account.type(),
                account.envelopeType(),
                account.currency().value(),
                account.active(),
                account.createdAt()
        );
    }
}
