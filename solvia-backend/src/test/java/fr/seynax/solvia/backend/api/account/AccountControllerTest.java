package fr.seynax.solvia.backend.api.account;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import fr.seynax.solvia.backend.api.MockMvcSupport;
import fr.seynax.solvia.domain.model.Account;
import fr.seynax.solvia.domain.model.AccountType;
import fr.seynax.solvia.domain.model.EnvelopeType;
import fr.seynax.solvia.domain.money.CurrencyCode;
import fr.seynax.solvia.infrastructure.persistence.account.AccountJdbcRepository;

class AccountControllerTest {

    @Test
    void listsAccounts() throws Exception {
        AccountJdbcRepository repository = mock(AccountJdbcRepository.class);
        Account account = new Account(
                UUID.randomUUID(),
                "Main account",
                AccountType.CHECKING,
                EnvelopeType.CURRENT_ACCOUNT,
                CurrencyCode.eur(),
                true,
                Instant.parse("2026-06-01T12:00:00Z")
        );
        when(repository.findAll()).thenReturn(List.of(account));

        MockMvc mvc = MockMvcSupport.standaloneMvc(new AccountController(repository));

        mvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(account.id().toString()))
                .andExpect(jsonPath("$[0].name").value("Main account"))
                .andExpect(jsonPath("$[0].currencyCode").value("EUR"));
    }

    @Test
    void createsAccount() throws Exception {
        AccountJdbcRepository repository = mock(AccountJdbcRepository.class);
        MockMvc mvc = MockMvcSupport.standaloneMvc(new AccountController(repository));

        mvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "PEA",
                                  "type": "PEA",
                                  "envelopeType": "PEA",
                                  "currencyCode": "EUR"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, notNullValue()))
                .andExpect(jsonPath("$.name").value("PEA"))
                .andExpect(jsonPath("$.type").value("PEA"))
                .andExpect(jsonPath("$.currencyCode").value("EUR"));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(repository).save(captor.capture());
    }

    @Test
    void returnsNotFoundForUnknownAccount() throws Exception {
        AccountJdbcRepository repository = mock(AccountJdbcRepository.class);
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        MockMvc mvc = MockMvcSupport.standaloneMvc(new AccountController(repository));

        mvc.perform(get("/api/accounts/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found: " + id));
    }

    @Test
    void validatesCreateRequest() throws Exception {
        AccountJdbcRepository repository = mock(AccountJdbcRepository.class);
        MockMvc mvc = MockMvcSupport.standaloneMvc(new AccountController(repository));

        mvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " ",
                                  "type": "CHECKING",
                                  "envelopeType": "CURRENT_ACCOUNT",
                                  "currencyCode": "EUR"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }
}
