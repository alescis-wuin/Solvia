package fr.seynax.solvia.domain.calculation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import fr.seynax.solvia.domain.calculation.NetWorthCalculator.AggregationMode;
import fr.seynax.solvia.domain.calculation.NetWorthCalculator.CalculationData;
import fr.seynax.solvia.domain.calculation.NetWorthCalculator.NetWorthSeriesPoint;
import fr.seynax.solvia.domain.calculation.NetWorthCalculator.NetWorthValuation;
import fr.seynax.solvia.domain.calculation.NetWorthCalculator.PerformanceSummary;
import fr.seynax.solvia.domain.calculation.NetWorthCalculator.TimeBucket;
import fr.seynax.solvia.domain.model.Account;
import fr.seynax.solvia.domain.model.AccountBalanceSnapshot;
import fr.seynax.solvia.domain.model.AccountType;
import fr.seynax.solvia.domain.model.Asset;
import fr.seynax.solvia.domain.model.AssetType;
import fr.seynax.solvia.domain.model.CashFlow;
import fr.seynax.solvia.domain.model.CashFlowType;
import fr.seynax.solvia.domain.model.EnvelopeType;
import fr.seynax.solvia.domain.model.FxRate;
import fr.seynax.solvia.domain.model.Position;
import fr.seynax.solvia.domain.model.PositionSnapshot;
import fr.seynax.solvia.domain.model.ValuationConfidence;
import fr.seynax.solvia.domain.money.CurrencyCode;
import fr.seynax.solvia.domain.money.MoneyAmount;

class NetWorthCalculatorTest {

    private final NetWorthCalculator calculator = new NetWorthCalculator();

    @Test
    void valuesNetWorthWithLastKnownSnapshotsAndAllocation() {
        Fixture fixture = fixture();

        NetWorthValuation valuation = calculator.valueAt(
                fixture.data(),
                LocalDate.parse("2026-06-10"),
                CurrencyCode.eur()
        );

        assertEquals(0, new BigDecimal("2300").compareTo(valuation.total().amount()));
        assertEquals(2, valuation.accountValues().size());
        assertEquals(2, valuation.assetTypeValues().size());
        assertEquals(0, new BigDecimal("52.17391304347826086956521739130435")
                .compareTo(valuation.assetTypeValues().stream()
                        .filter(value -> value.assetType() == AssetType.FIAT_CURRENCY)
                        .findFirst()
                        .orElseThrow()
                        .allocation()
                        .asPercent()));
    }

    @Test
    void convertsForeignCurrencyValuesWithLatestKnownFxRate() {
        Account usdAccount = new Account(
                UUID.randomUUID(),
                "USD cash",
                AccountType.CHECKING,
                EnvelopeType.CURRENT_ACCOUNT,
                CurrencyCode.usd(),
                true,
                Instant.parse("2026-01-01T00:00:00Z")
        );
        CalculationData data = new CalculationData(
                List.of(usdAccount),
                List.of(),
                List.of(),
                List.of(new AccountBalanceSnapshot(
                        UUID.randomUUID(),
                        usdAccount.id(),
                        LocalDate.parse("2026-06-01"),
                        MoneyAmount.of(new BigDecimal("1000"), CurrencyCode.usd()),
                        ValuationConfidence.OBSERVED,
                        null,
                        Instant.parse("2026-06-01T20:00:00Z")
                )),
                List.of(),
                List.of(),
                List.of(FxRate.of(CurrencyCode.usd(), CurrencyCode.eur(), LocalDate.parse("2026-05-31"), new BigDecimal("0.92"), "test"))
        );

        NetWorthValuation valuation = calculator.valueAt(data, LocalDate.parse("2026-06-10"), CurrencyCode.eur());

        assertEquals(0, new BigDecimal("920").compareTo(valuation.total().amount()));
    }

    @Test
    void rejectsMissingFxRate() {
        Account usdAccount = new Account(
                UUID.randomUUID(),
                "USD cash",
                AccountType.CHECKING,
                EnvelopeType.CURRENT_ACCOUNT,
                CurrencyCode.usd(),
                true,
                Instant.parse("2026-01-01T00:00:00Z")
        );
        CalculationData data = new CalculationData(
                List.of(usdAccount),
                List.of(),
                List.of(),
                List.of(AccountBalanceSnapshot.observed(
                        usdAccount.id(),
                        LocalDate.parse("2026-06-01"),
                        MoneyAmount.of(new BigDecimal("100"), CurrencyCode.usd())
                )),
                List.of(),
                List.of(),
                List.of()
        );

        assertThrows(IllegalArgumentException.class, () -> calculator.valueAt(data, LocalDate.parse("2026-06-10"), CurrencyCode.eur()));
    }

    @Test
    void computesFlowAdjustedGain() {
        Fixture fixture = fixture();

        PerformanceSummary performance = calculator.performance(
                fixture.data(),
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-10"),
                CurrencyCode.eur()
        );

        assertEquals(0, new BigDecimal("1700").compareTo(performance.startValue().amount()));
        assertEquals(0, new BigDecimal("2300").compareTo(performance.endValue().amount()));
        assertEquals(0, new BigDecimal("600").compareTo(performance.grossChange().amount()));
        assertEquals(0, new BigDecimal("500").compareTo(performance.netExternalFlow().amount()));
        assertEquals(0, new BigDecimal("100").compareTo(performance.flowAdjustedGain().amount()));
    }

    @Test
    void createsCustomLastKnownSeries() {
        Fixture fixture = fixture();

        List<NetWorthSeriesPoint> points = calculator.series(
                fixture.data(),
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-10"),
                TimeBucket.days(3),
                AggregationMode.LAST_KNOWN,
                CurrencyCode.eur()
        );

        assertEquals(4, points.size());
        assertEquals(LocalDate.parse("2026-06-01"), points.get(0).valueDate());
        assertEquals(0, new BigDecimal("1700").compareTo(points.get(0).value().amount()));
        assertEquals(LocalDate.parse("2026-06-10"), points.get(3).valueDate());
        assertEquals(0, new BigDecimal("2300").compareTo(points.get(3).value().amount()));
    }

    private Fixture fixture() {
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

        CalculationData data = new CalculationData(
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
        return new Fixture(data);
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

    private record Fixture(CalculationData data) {
    }
}
