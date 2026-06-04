package fr.seynax.solvia.backend.api.calculation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import fr.seynax.solvia.backend.api.MockMvcSupport;
import fr.seynax.solvia.domain.calculation.NetWorthCalculator.CalculationData;
import fr.seynax.solvia.domain.model.Account;
import fr.seynax.solvia.domain.model.AccountBalanceSnapshot;
import fr.seynax.solvia.domain.model.AccountType;
import fr.seynax.solvia.domain.model.Asset;
import fr.seynax.solvia.domain.model.AssetType;
import fr.seynax.solvia.domain.model.CashFlow;
import fr.seynax.solvia.domain.model.CashFlowType;
import fr.seynax.solvia.domain.model.EnvelopeType;
import fr.seynax.solvia.domain.model.Position;
import fr.seynax.solvia.domain.model.PositionSnapshot;
import fr.seynax.solvia.domain.model.ValuationConfidence;
import fr.seynax.solvia.domain.money.CurrencyCode;
import fr.seynax.solvia.domain.money.MoneyAmount;

class CalculationControllerTest {

    @Test
    void returnsNetWorth() throws Exception {
        CalculationDataLoader dataLoader = mock(CalculationDataLoader.class);
        when(dataLoader.load()).thenReturn(data());
        MockMvc mvc = MockMvcSupport.standaloneMvc(new CalculationController(dataLoader));

        mvc.perform(get("/api/net-worth")
                        .param("date", "2026-06-10")
                        .param("currency", "EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total.amount").value(2300))
                .andExpect(jsonPath("$.total.currencyCode").value("EUR"))
                .andExpect(jsonPath("$.accounts[0].accountName").value("Main account"))
                .andExpect(jsonPath("$.allocation[0].assetType").exists());
    }

    @Test
    void returnsNetWorthSeries() throws Exception {
        CalculationDataLoader dataLoader = mock(CalculationDataLoader.class);
        when(dataLoader.load()).thenReturn(data());
        MockMvc mvc = MockMvcSupport.standaloneMvc(new CalculationController(dataLoader));

        mvc.perform(get("/api/net-worth/series")
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-10")
                        .param("bucket", "3d")
                        .param("aggregation", "last")
                        .param("currency", "EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].valueDate").value("2026-06-01T00:00:00"))
                .andExpect(jsonPath("$[0].value.amount").value(1700))
                .andExpect(jsonPath("$[3].value.amount").value(2300));
    }

    @Test
    void returnsHourlyNetWorthSeries() throws Exception {
        CalculationDataLoader dataLoader = mock(CalculationDataLoader.class);
        when(dataLoader.load()).thenReturn(data());
        MockMvc mvc = MockMvcSupport.standaloneMvc(new CalculationController(dataLoader));

        mvc.perform(get("/api/net-worth/series")
                        .param("from", "2026-06-10")
                        .param("to", "2026-06-10")
                        .param("bucket", "1h")
                        .param("aggregation", "last")
                        .param("currency", "EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].valueDate").value("2026-06-10T00:00:00"))
                .andExpect(jsonPath("$[1].valueDate").value("2026-06-10T01:00:00"))
                .andExpect(jsonPath("$[23].value.amount").value(2300));
    }

    @Test
    void returnsSecondNetWorthSeriesWhenPointLimitAllowsIt() throws Exception {
        CalculationDataLoader dataLoader = mock(CalculationDataLoader.class);
        when(dataLoader.load()).thenReturn(data());
        MockMvc mvc = MockMvcSupport.standaloneMvc(new CalculationController(dataLoader));

        mvc.perform(get("/api/net-worth/series")
                        .param("from", "2026-06-10")
                        .param("to", "2026-06-10")
                        .param("bucket", "1s")
                        .param("aggregation", "last")
                        .param("currency", "EUR")
                        .param("maxPoints", "86400"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].valueDate").value("2026-06-10T00:00:00"))
                .andExpect(jsonPath("$[1].valueDate").value("2026-06-10T00:00:01"));
    }

    @Test
    void returnsPerformance() throws Exception {
        CalculationDataLoader dataLoader = mock(CalculationDataLoader.class);
        when(dataLoader.load()).thenReturn(data());
        MockMvc mvc = MockMvcSupport.standaloneMvc(new CalculationController(dataLoader));

        mvc.perform(get("/api/performance")
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-10")
                        .param("currency", "EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grossChange.amount").value(600))
                .andExpect(jsonPath("$.netExternalFlow.amount").value(500))
                .andExpect(jsonPath("$.flowAdjustedGain.amount").value(100));
    }

    @Test
    void rejectsInvalidBucket() throws Exception {
        CalculationDataLoader dataLoader = mock(CalculationDataLoader.class);
        when(dataLoader.load()).thenReturn(data());
        MockMvc mvc = MockMvcSupport.standaloneMvc(new CalculationController(dataLoader));

        mvc.perform(get("/api/net-worth/series")
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-10")
                        .param("bucket", "0d"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    private CalculationData data() {
        Account cash = new Account(
                UUID.randomUUID(),
                "Main account",
                AccountType.CHECKING,
                EnvelopeType.CURRENT_ACCOUNT,
                CurrencyCode.eur(),
                true,
                Instant.parse("2026-01-01T00:00:00Z")
        );
        Account pea = new Account(
                UUID.randomUUID(),
                "PEA",
                AccountType.PEA,
                EnvelopeType.PEA,
                CurrencyCode.eur(),
                true,
                Instant.parse("2026-01-01T00:00:00Z")
        );
        Asset etf = new Asset(
                UUID.randomUUID(),
                "World ETF",
                AssetType.ETF,
                CurrencyCode.eur(),
                "WLD",
                true,
                Instant.parse("2026-01-01T00:00:00Z")
        );
        Position position = new Position(
                UUID.randomUUID(),
                pea.id(),
                etf.id(),
                new BigDecimal("10"),
                true,
                Instant.parse("2026-01-01T00:00:00Z")
        );
        return new CalculationData(
                List.of(cash, pea),
                List.of(etf),
                List.of(position),
                List.of(
                        snapshot(cash.id(), "2026-06-01", "1000"),
                        snapshot(cash.id(), "2026-06-10", "1200")
                ),
                List.of(
                        positionSnapshot(position.id(), "2026-06-01", "10", "700"),
                        positionSnapshot(position.id(), "2026-06-10", "10", "1100")
                ),
                List.of(CashFlow.of(
                        cash.id(),
                        CashFlowType.DEPOSIT,
                        LocalDate.parse("2026-06-05"),
                        MoneyAmount.of(new BigDecimal("500"), CurrencyCode.eur()),
                        "External deposit"
                )),
                List.of()
        );
    }

    private AccountBalanceSnapshot snapshot(UUID accountId, String date, String amount) {
        return new AccountBalanceSnapshot(
                UUID.randomUUID(),
                accountId,
                LocalDate.parse(date),
                MoneyAmount.of(new BigDecimal(amount), CurrencyCode.eur()),
                ValuationConfidence.OBSERVED,
                null,
                Instant.parse(date + "T20:00:00Z")
        );
    }

    private PositionSnapshot positionSnapshot(UUID positionId, String date, String quantity, String amount) {
        return new PositionSnapshot(
                UUID.randomUUID(),
                positionId,
                LocalDate.parse(date),
                new BigDecimal(quantity),
                MoneyAmount.of(new BigDecimal(amount), CurrencyCode.eur()),
                ValuationConfidence.OBSERVED,
                Instant.parse(date + "T20:00:00Z")
        );
    }
}
