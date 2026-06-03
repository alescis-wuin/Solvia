package fr.seynax.solvia.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import fr.seynax.solvia.domain.money.CurrencyCode;

class AccountTest {

    @Test
    void createsActiveAccount() {
        Account account = Account.create(
                "Livret A",
                AccountType.SAVINGS,
                EnvelopeType.REGULATED_SAVINGS,
                CurrencyCode.eur()
        );

        assertTrue(account.active());
        assertEquals("Livret A", account.name());
        assertEquals(AccountType.SAVINGS, account.type());
    }

    @Test
    void trimsRenamedAccount() {
        Account account = Account.create(
                "Current account",
                AccountType.CHECKING,
                EnvelopeType.CURRENT_ACCOUNT,
                CurrencyCode.eur()
        );

        Account renamed = account.rename(" Main account ");

        assertEquals("Main account", renamed.name());
    }

    @Test
    void canDeactivateAccount() {
        Account account = Account.create(
                "PEA",
                AccountType.PEA,
                EnvelopeType.PEA,
                CurrencyCode.eur()
        );

        assertFalse(account.deactivate().active());
    }

    @Test
    void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new Account(
                UUID.randomUUID(),
                " ",
                AccountType.CHECKING,
                EnvelopeType.CURRENT_ACCOUNT,
                CurrencyCode.eur(),
                true,
                Instant.now()
        ));
    }
}
