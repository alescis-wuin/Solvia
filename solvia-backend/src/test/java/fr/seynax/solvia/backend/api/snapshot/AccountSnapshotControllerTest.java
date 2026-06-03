package fr.seynax.solvia.backend.api.snapshot;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import fr.seynax.solvia.backend.api.MockMvcSupport;
import fr.seynax.solvia.domain.model.Account;
import fr.seynax.solvia.domain.model.AccountBalanceSnapshot;
import fr.seynax.solvia.domain.model.AccountType;
import fr.seynax.solvia.domain.model.EnvelopeType;
import fr.seynax.solvia.domain.money.CurrencyCode;
import fr.seynax.solvia.infrastructure.persistence.account.AccountJdbcRepository;
import fr.seynax.solvia.infrastructure.persistence.account.AccountSnapshotJdbcRepository;

class AccountSnapshotControllerTest {

    @Test
    void createsAccountSnapshot() throws Exception {
        AccountSnapshotJdbcRepository snapshotRepository = mock(AccountSnapshotJdbcRepository.class);
        AccountJdbcRepository accountRepository = mock(AccountJdbcRepository.class);
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(new Account(
                accountId,
                "Savings",
                AccountType.SAVINGS,
                EnvelopeType.REGULATED_SAVINGS,
                CurrencyCode.eur(),
                true,
                Instant.parse("2026-06-01T12:00:00Z")
        )));

        MockMvc mvc = MockMvcSupport.standaloneMvc(new AccountSnapshotController(snapshotRepository, accountRepository));

        mvc.perform(post("/api/account-snapshots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": "%s",
                                  "valueDate": "2026-06-01",
                                  "balance": {
                                    "amount": 1200.75,
                                    "currencyCode": "EUR"
                                  }
                                }
                                """.formatted(accountId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.balance.amount").value(1200.75))
                .andExpect(jsonPath("$.balance.currencyCode").value("EUR"))
                .andExpect(jsonPath("$.confidence").value("OBSERVED"));

        ArgumentCaptor<AccountBalanceSnapshot> captor = ArgumentCaptor.forClass(AccountBalanceSnapshot.class);
        verify(snapshotRepository).save(captor.capture());
    }
}
