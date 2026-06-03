package fr.seynax.solvia.backend.api.flow;

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
import fr.seynax.solvia.domain.model.AccountType;
import fr.seynax.solvia.domain.model.CashFlow;
import fr.seynax.solvia.domain.model.EnvelopeType;
import fr.seynax.solvia.domain.money.CurrencyCode;
import fr.seynax.solvia.infrastructure.persistence.account.AccountJdbcRepository;
import fr.seynax.solvia.infrastructure.persistence.flow.CashFlowJdbcRepository;

class CashFlowControllerTest {

    @Test
    void createsCashFlow() throws Exception {
        CashFlowJdbcRepository cashFlowRepository = mock(CashFlowJdbcRepository.class);
        AccountJdbcRepository accountRepository = mock(AccountJdbcRepository.class);
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(new Account(
                accountId,
                "Main account",
                AccountType.CHECKING,
                EnvelopeType.CURRENT_ACCOUNT,
                CurrencyCode.eur(),
                true,
                Instant.parse("2026-06-01T12:00:00Z")
        )));

        MockMvc mvc = MockMvcSupport.standaloneMvc(new CashFlowController(cashFlowRepository, accountRepository));

        mvc.perform(post("/api/cash-flows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": "%s",
                                  "type": "DEPOSIT",
                                  "valueDate": "2026-06-01",
                                  "amount": {
                                    "amount": 500,
                                    "currencyCode": "EUR"
                                  },
                                  "label": "Initial deposit"
                                }
                                """.formatted(accountId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount.amount").value(500));

        ArgumentCaptor<CashFlow> captor = ArgumentCaptor.forClass(CashFlow.class);
        verify(cashFlowRepository).save(captor.capture());
    }
}
