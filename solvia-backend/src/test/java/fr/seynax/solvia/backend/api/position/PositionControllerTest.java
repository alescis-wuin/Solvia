package fr.seynax.solvia.backend.api.position;

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
import fr.seynax.solvia.domain.model.Asset;
import fr.seynax.solvia.domain.model.AssetType;
import fr.seynax.solvia.domain.model.EnvelopeType;
import fr.seynax.solvia.domain.model.Position;
import fr.seynax.solvia.domain.money.CurrencyCode;
import fr.seynax.solvia.infrastructure.persistence.account.AccountJdbcRepository;
import fr.seynax.solvia.infrastructure.persistence.asset.AssetJdbcRepository;
import fr.seynax.solvia.infrastructure.persistence.position.PositionJdbcRepository;

class PositionControllerTest {

    @Test
    void createsPositionWhenAccountAndAssetExist() throws Exception {
        PositionJdbcRepository positionRepository = mock(PositionJdbcRepository.class);
        AccountJdbcRepository accountRepository = mock(AccountJdbcRepository.class);
        AssetJdbcRepository assetRepository = mock(AssetJdbcRepository.class);
        UUID accountId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(new Account(
                accountId,
                "PEA",
                AccountType.PEA,
                EnvelopeType.PEA,
                CurrencyCode.eur(),
                true,
                Instant.parse("2026-06-01T12:00:00Z")
        )));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(new Asset(
                assetId,
                "ETF",
                AssetType.ETF,
                CurrencyCode.eur(),
                "CW8",
                true,
                Instant.parse("2026-06-01T12:00:00Z")
        )));

        MockMvc mvc = MockMvcSupport.standaloneMvc(new PositionController(positionRepository, accountRepository, assetRepository));

        mvc.perform(post("/api/positions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": "%s",
                                  "assetId": "%s",
                                  "quantity": 3.5
                                }
                                """.formatted(accountId, assetId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.assetId").value(assetId.toString()))
                .andExpect(jsonPath("$.quantity").value(3.5));

        ArgumentCaptor<Position> captor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).save(captor.capture());
    }

    @Test
    void returnsNotFoundWhenAccountIsMissing() throws Exception {
        PositionJdbcRepository positionRepository = mock(PositionJdbcRepository.class);
        AccountJdbcRepository accountRepository = mock(AccountJdbcRepository.class);
        AssetJdbcRepository assetRepository = mock(AssetJdbcRepository.class);
        UUID accountId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        MockMvc mvc = MockMvcSupport.standaloneMvc(new PositionController(positionRepository, accountRepository, assetRepository));

        mvc.perform(post("/api/positions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": "%s",
                                  "assetId": "%s",
                                  "quantity": 1
                                }
                                """.formatted(accountId, assetId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found: " + accountId));
    }
}
