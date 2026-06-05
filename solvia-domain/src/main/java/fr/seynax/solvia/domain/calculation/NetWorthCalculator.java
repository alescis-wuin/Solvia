package fr.seynax.solvia.domain.calculation;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import fr.seynax.solvia.domain.model.Account;
import fr.seynax.solvia.domain.model.AccountBalanceSnapshot;
import fr.seynax.solvia.domain.model.Asset;
import fr.seynax.solvia.domain.model.AssetType;
import fr.seynax.solvia.domain.model.CashFlow;
import fr.seynax.solvia.domain.model.CashFlowType;
import fr.seynax.solvia.domain.model.FxRate;
import fr.seynax.solvia.domain.model.Position;
import fr.seynax.solvia.domain.model.PositionSnapshot;
import fr.seynax.solvia.domain.money.CurrencyCode;
import fr.seynax.solvia.domain.money.MoneyAmount;
import fr.seynax.solvia.domain.money.Percentage;

public final class NetWorthCalculator {

    private static final MathContext MATH_CONTEXT = MathContext.DECIMAL128;
    private static final ZoneId TEMPORAL_SERIES_ZONE = ZoneId.systemDefault();

    public NetWorthValuation valueAt(CalculationData data, LocalDate valueDate, CurrencyCode reportingCurrency) {
        Objects.requireNonNull(data, "calculation data is required");
        Objects.requireNonNull(valueDate, "value date is required");
        Objects.requireNonNull(reportingCurrency, "reporting currency is required");

        Map<UUID, Account> accountsById = data.accounts().stream()
                .collect(Collectors.toMap(Account::id, Function.identity(), (first, second) -> first, LinkedHashMap::new));
        Map<UUID, Asset> assetsById = data.assets().stream()
                .collect(Collectors.toMap(Asset::id, Function.identity(), (first, second) -> first, LinkedHashMap::new));

        Map<UUID, MoneyAmount> accountValues = new LinkedHashMap<>();
        accountsById.keySet().forEach(accountId -> accountValues.put(accountId, MoneyAmount.zero(reportingCurrency)));

        Map<AssetType, MoneyAmount> assetTypeValues = new EnumMap<>(AssetType.class);

        for (Account account : data.accounts()) {
            latestAccountSnapshot(data, account.id(), valueDate)
                    .map(AccountBalanceSnapshot::balance)
                    .map(balance -> convert(balance, reportingCurrency, valueDate, data.fxRates()))
                    .ifPresent(balance -> {
                        add(accountValues, account.id(), balance, reportingCurrency);
                        add(assetTypeValues, AssetType.FIAT_CURRENCY, balance, reportingCurrency);
                    });
        }

        for (Position position : data.positions()) {
            latestPositionSnapshot(data, position.id(), valueDate)
                    .map(PositionSnapshot::marketValue)
                    .map(marketValue -> convert(marketValue, reportingCurrency, valueDate, data.fxRates()))
                    .ifPresent(marketValue -> {
                        add(accountValues, position.accountId(), marketValue, reportingCurrency);
                        AssetType assetType = Optional.ofNullable(assetsById.get(position.assetId()))
                                .map(Asset::type)
                                .orElse(AssetType.OTHER);
                        add(assetTypeValues, assetType, marketValue, reportingCurrency);
                    });
        }

        MoneyAmount total = accountValues.values().stream()
                .reduce(MoneyAmount.zero(reportingCurrency), MoneyAmount::plus);

        List<AccountValuation> accountValuations = accountValues.entrySet().stream()
                .filter(entry -> !entry.getValue().isZero())
                .map(entry -> new AccountValuation(entry.getKey(), accountName(accountsById, entry.getKey()), entry.getValue()))
                .toList();

        List<AssetTypeValuation> assetTypeValuations = assetTypeValues.entrySet().stream()
                .filter(entry -> !entry.getValue().isZero())
                .map(entry -> new AssetTypeValuation(
                        entry.getKey(),
                        entry.getValue(),
                        allocation(entry.getValue(), total)
                ))
                .toList();

        return new NetWorthValuation(valueDate, total, accountValuations, assetTypeValuations);
    }

    public PerformanceSummary performance(CalculationData data, LocalDate from, LocalDate to, CurrencyCode reportingCurrency) {
        requirePeriod(from, to);
        NetWorthValuation start = valueAt(data, from, reportingCurrency);
        NetWorthValuation end = valueAt(data, to, reportingCurrency);

        MoneyAmount grossChange = end.total().minus(start.total());
        MoneyAmount netExternalFlow = externalFlow(data, from, to, reportingCurrency);
        MoneyAmount flowAdjustedGain = grossChange.minus(netExternalFlow);

        return new PerformanceSummary(
                from,
                to,
                start.total(),
                end.total(),
                grossChange,
                ratio(grossChange, start.total()),
                netExternalFlow,
                flowAdjustedGain,
                ratio(flowAdjustedGain, start.total())
        );
    }

    public List<NetWorthSeriesPoint> series(
            CalculationData data,
            LocalDate from,
            LocalDate to,
            TimeBucket bucket,
            AggregationMode aggregationMode,
            CurrencyCode reportingCurrency
    ) {
        requirePeriod(from, to);
        Objects.requireNonNull(bucket, "time bucket is required");
        Objects.requireNonNull(aggregationMode, "aggregation mode is required");
        Objects.requireNonNull(reportingCurrency, "reporting currency is required");

        List<NetWorthSeriesPoint> points = new ArrayList<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            LocalDate next = bucket.next(cursor);
            LocalDate bucketEnd = min(to, next.minusDays(1));
            MoneyAmount value = switch (aggregationMode) {
                case LAST_KNOWN -> valueAt(data, bucketEnd, reportingCurrency).total();
                case AVERAGE -> averageDailyValue(data, cursor, bucketEnd, reportingCurrency);
            };
            points.add(new NetWorthSeriesPoint(cursor, value));
            cursor = next;
        }
        return List.copyOf(points);
    }

    public List<NetWorthTemporalSeriesPoint> temporalSeries(
            CalculationData data,
            LocalDateTime from,
            LocalDateTime to,
            TimeBucket bucket,
            AggregationMode aggregationMode,
            CurrencyCode reportingCurrency,
            int maxPoints
    ) {
        requirePeriod(from, to);
        Objects.requireNonNull(bucket, "time bucket is required");
        Objects.requireNonNull(aggregationMode, "aggregation mode is required");
        Objects.requireNonNull(reportingCurrency, "reporting currency is required");
        if (maxPoints <= 0) {
            throw new IllegalArgumentException("maximum point count must be positive");
        }

        List<NetWorthTemporalSeriesPoint> points = new ArrayList<>();
        LocalDateTime cursor = from;
        while (!cursor.isAfter(to)) {
            if (points.size() >= maxPoints) {
                throw new IllegalArgumentException("Requested series is too dense. Increase the time step or reduce the period. Maximum points: " + maxPoints);
            }
            LocalDateTime next = bucket.next(cursor);
            LocalDateTime bucketEnd = min(to, next.minusNanos(1));
            MoneyAmount value = switch (aggregationMode) {
                case LAST_KNOWN -> valueAt(data, bucketEnd, reportingCurrency);
                case AVERAGE -> averageDailyValue(data, cursor.toLocalDate(), bucketEnd.toLocalDate(), reportingCurrency);
            };
            points.add(new NetWorthTemporalSeriesPoint(cursor, value));
            cursor = next;
        }
        return List.copyOf(points);
    }

    private MoneyAmount valueAt(CalculationData data, LocalDateTime valueDateTime, CurrencyCode reportingCurrency) {
        Objects.requireNonNull(data, "calculation data is required");
        Objects.requireNonNull(valueDateTime, "value date time is required");
        Objects.requireNonNull(reportingCurrency, "reporting currency is required");

        LocalDate valueDate = valueDateTime.toLocalDate();
        MoneyAmount total = MoneyAmount.zero(reportingCurrency);

        for (Account account : data.accounts()) {
            Optional<AccountBalanceSnapshot> snapshot = latestAccountSnapshot(data, account.id(), valueDateTime);
            if (snapshot.isPresent()) {
                total = total.plus(convert(snapshot.get().balance(), reportingCurrency, valueDate, data.fxRates()));
            }
        }

        for (Position position : data.positions()) {
            Optional<PositionSnapshot> snapshot = latestPositionSnapshot(data, position.id(), valueDateTime);
            if (snapshot.isPresent()) {
                total = total.plus(convert(snapshot.get().marketValue(), reportingCurrency, valueDate, data.fxRates()));
            }
        }

        return total;
    }

    private MoneyAmount externalFlow(CalculationData data, LocalDate from, LocalDate to, CurrencyCode reportingCurrency) {
        MoneyAmount total = MoneyAmount.zero(reportingCurrency);
        for (CashFlow cashFlow : data.cashFlows()) {
            if (cashFlow.valueDate().isAfter(from) && !cashFlow.valueDate().isAfter(to) && isExternalCapitalFlow(cashFlow.type())) {
                MoneyAmount converted = convert(abs(cashFlow.amount()), reportingCurrency, cashFlow.valueDate(), data.fxRates());
                total = switch (cashFlow.type()) {
                    case DEPOSIT, TRANSFER_IN -> total.plus(converted);
                    case WITHDRAWAL, TRANSFER_OUT -> total.minus(converted);
                    default -> total;
                };
            }
        }
        return total;
    }

    private MoneyAmount averageDailyValue(CalculationData data, LocalDate from, LocalDate to, CurrencyCode reportingCurrency) {
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            sum = sum.add(valueAt(data, cursor, reportingCurrency).total().amount());
            count++;
            cursor = cursor.plusDays(1);
        }
        return MoneyAmount.of(sum.divide(BigDecimal.valueOf(count), MATH_CONTEXT), reportingCurrency);
    }

    private Optional<AccountBalanceSnapshot> latestAccountSnapshot(CalculationData data, UUID accountId, LocalDate valueDate) {
        return data.accountSnapshots().stream()
                .filter(snapshot -> snapshot.accountId().equals(accountId))
                .filter(snapshot -> !snapshot.valueDate().isAfter(valueDate))
                .max(Comparator.comparing(AccountBalanceSnapshot::valueDate)
                        .thenComparing(AccountBalanceSnapshot::recordedAt));
    }

    private Optional<AccountBalanceSnapshot> latestAccountSnapshot(CalculationData data, UUID accountId, LocalDateTime valueDateTime) {
        return data.accountSnapshots().stream()
                .filter(snapshot -> snapshot.accountId().equals(accountId))
                .filter(snapshot -> isVisibleAt(snapshot.valueDate(), snapshot.recordedAt(), valueDateTime))
                .max(Comparator.comparing(AccountBalanceSnapshot::valueDate)
                        .thenComparing(AccountBalanceSnapshot::recordedAt));
    }

    private Optional<PositionSnapshot> latestPositionSnapshot(CalculationData data, UUID positionId, LocalDate valueDate) {
        return data.positionSnapshots().stream()
                .filter(snapshot -> snapshot.positionId().equals(positionId))
                .filter(snapshot -> !snapshot.valueDate().isAfter(valueDate))
                .max(Comparator.comparing(PositionSnapshot::valueDate)
                        .thenComparing(PositionSnapshot::recordedAt));
    }

    private Optional<PositionSnapshot> latestPositionSnapshot(CalculationData data, UUID positionId, LocalDateTime valueDateTime) {
        return data.positionSnapshots().stream()
                .filter(snapshot -> snapshot.positionId().equals(positionId))
                .filter(snapshot -> isVisibleAt(snapshot.valueDate(), snapshot.recordedAt(), valueDateTime))
                .max(Comparator.comparing(PositionSnapshot::valueDate)
                        .thenComparing(PositionSnapshot::recordedAt));
    }

    private boolean isVisibleAt(LocalDate valueDate, Instant recordedAt, LocalDateTime valueDateTime) {
        LocalDate targetDate = valueDateTime.toLocalDate();
        if (valueDate.isBefore(targetDate)) {
            return true;
        }
        if (valueDate.isAfter(targetDate)) {
            return false;
        }
        Instant cutoff = valueDateTime.atZone(TEMPORAL_SERIES_ZONE).toInstant();
        return !recordedAt.isAfter(cutoff);
    }

    private MoneyAmount convert(MoneyAmount amount, CurrencyCode reportingCurrency, LocalDate valueDate, List<FxRate> fxRates) {
        if (amount.currency().equals(reportingCurrency)) {
            return MoneyAmount.of(amount.amount(), reportingCurrency);
        }

        Optional<FxRate> direct = latestFxRate(fxRates, amount.currency(), reportingCurrency, valueDate);
        if (direct.isPresent()) {
            return MoneyAmount.of(amount.amount().multiply(direct.get().rate(), MATH_CONTEXT), reportingCurrency);
        }

        Optional<FxRate> inverse = latestFxRate(fxRates, reportingCurrency, amount.currency(), valueDate);
        if (inverse.isPresent()) {
            return MoneyAmount.of(amount.amount().divide(inverse.get().rate(), MATH_CONTEXT), reportingCurrency);
        }

        throw new IllegalArgumentException("Missing FX rate from " + amount.currency().value()
                + " to " + reportingCurrency.value() + " at " + valueDate);
    }

    private Optional<FxRate> latestFxRate(List<FxRate> fxRates, CurrencyCode baseCurrency, CurrencyCode quoteCurrency, LocalDate valueDate) {
        return fxRates.stream()
                .filter(rate -> rate.baseCurrency().equals(baseCurrency))
                .filter(rate -> rate.quoteCurrency().equals(quoteCurrency))
                .filter(rate -> !rate.rateDate().isAfter(valueDate))
                .max(Comparator.comparing(FxRate::rateDate).thenComparing(FxRate::recordedAt));
    }

    private MoneyAmount abs(MoneyAmount amount) {
        return MoneyAmount.of(amount.amount().abs(), amount.currency());
    }

    private boolean isExternalCapitalFlow(CashFlowType type) {
        return switch (type) {
            case DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT -> true;
            case INTEREST, DIVIDEND, FEE, TAX, CASHBACK, CORRECTION -> false;
        };
    }

    private Percentage allocation(MoneyAmount value, MoneyAmount total) {
        if (total.isZero()) {
            return Percentage.ofRatio(BigDecimal.ZERO);
        }
        return Percentage.ofRatio(value.amount().divide(total.amount(), MATH_CONTEXT));
    }

    private Optional<Percentage> ratio(MoneyAmount value, MoneyAmount denominator) {
        if (denominator.isZero()) {
            return Optional.empty();
        }
        return Optional.of(Percentage.ofRatio(value.amount().divide(denominator.amount(), MATH_CONTEXT)));
    }

    private <K> void add(Map<K, MoneyAmount> values, K key, MoneyAmount amount, CurrencyCode reportingCurrency) {
        values.merge(key, amount, MoneyAmount::plus);
        values.putIfAbsent(key, MoneyAmount.zero(reportingCurrency));
    }

    private String accountName(Map<UUID, Account> accountsById, UUID accountId) {
        return Optional.ofNullable(accountsById.get(accountId))
                .map(Account::name)
                .orElse(null);
    }

    private LocalDate min(LocalDate first, LocalDate second) {
        return first.isBefore(second) ? first : second;
    }

    private LocalDateTime min(LocalDateTime first, LocalDateTime second) {
        return first.isBefore(second) ? first : second;
    }

    private void requirePeriod(LocalDate from, LocalDate to) {
        Objects.requireNonNull(from, "period start date is required");
        Objects.requireNonNull(to, "period end date is required");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("period start date must be before or equal to end date");
        }
    }

    private void requirePeriod(LocalDateTime from, LocalDateTime to) {
        Objects.requireNonNull(from, "period start date is required");
        Objects.requireNonNull(to, "period end date is required");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("period start date must be before or equal to end date");
        }
    }

    public enum AggregationMode {
        LAST_KNOWN,
        AVERAGE
    }

    public enum TimeBucketUnit {
        SECONDS,
        MINUTES,
        HOURS,
        DAYS,
        WEEKS,
        MONTHS
    }

    public record TimeBucket(int amount, TimeBucketUnit unit) {

        public TimeBucket {
            if (amount <= 0) {
                throw new IllegalArgumentException("time bucket amount must be positive");
            }
            unit = Objects.requireNonNull(unit, "time bucket unit is required");
        }

        public static TimeBucket seconds(int amount) {
            return new TimeBucket(amount, TimeBucketUnit.SECONDS);
        }

        public static TimeBucket minutes(int amount) {
            return new TimeBucket(amount, TimeBucketUnit.MINUTES);
        }

        public static TimeBucket hours(int amount) {
            return new TimeBucket(amount, TimeBucketUnit.HOURS);
        }

        public static TimeBucket days(int amount) {
            return new TimeBucket(amount, TimeBucketUnit.DAYS);
        }

        public static TimeBucket weeks(int amount) {
            return new TimeBucket(amount, TimeBucketUnit.WEEKS);
        }

        public static TimeBucket months(int amount) {
            return new TimeBucket(amount, TimeBucketUnit.MONTHS);
        }

        LocalDate next(LocalDate date) {
            return switch (unit) {
                case SECONDS, MINUTES, HOURS, DAYS -> date.plusDays(amount);
                case WEEKS -> date.plus(amount, ChronoUnit.WEEKS);
                case MONTHS -> date.plusMonths(amount);
            };
        }

        LocalDateTime next(LocalDateTime dateTime) {
            return switch (unit) {
                case SECONDS -> dateTime.plusSeconds(amount);
                case MINUTES -> dateTime.plusMinutes(amount);
                case HOURS -> dateTime.plusHours(amount);
                case DAYS -> dateTime.plusDays(amount);
                case WEEKS -> dateTime.plusWeeks(amount);
                case MONTHS -> dateTime.plusMonths(amount);
            };
        }
    }

    public record CalculationData(
            List<Account> accounts,
            List<Asset> assets,
            List<Position> positions,
            List<AccountBalanceSnapshot> accountSnapshots,
            List<PositionSnapshot> positionSnapshots,
            List<CashFlow> cashFlows,
            List<FxRate> fxRates
    ) {

        public CalculationData {
            accounts = List.copyOf(Objects.requireNonNull(accounts, "accounts are required"));
            assets = List.copyOf(Objects.requireNonNull(assets, "assets are required"));
            positions = List.copyOf(Objects.requireNonNull(positions, "positions are required"));
            accountSnapshots = List.copyOf(Objects.requireNonNull(accountSnapshots, "account snapshots are required"));
            positionSnapshots = List.copyOf(Objects.requireNonNull(positionSnapshots, "position snapshots are required"));
            cashFlows = List.copyOf(Objects.requireNonNull(cashFlows, "cash flows are required"));
            fxRates = List.copyOf(Objects.requireNonNull(fxRates, "FX rates are required"));
        }
    }

    public record NetWorthValuation(
            LocalDate valueDate,
            MoneyAmount total,
            List<AccountValuation> accountValues,
            List<AssetTypeValuation> assetTypeValues
    ) {

        public NetWorthValuation {
            valueDate = Objects.requireNonNull(valueDate, "valuation date is required");
            total = Objects.requireNonNull(total, "valuation total is required");
            accountValues = List.copyOf(Objects.requireNonNull(accountValues, "account values are required"));
            assetTypeValues = List.copyOf(Objects.requireNonNull(assetTypeValues, "asset type values are required"));
        }
    }

    public record AccountValuation(UUID accountId, String accountName, MoneyAmount value) {
    }

    public record AssetTypeValuation(AssetType assetType, MoneyAmount value, Percentage allocation) {
    }

    public record NetWorthSeriesPoint(LocalDate valueDate, MoneyAmount value) {
    }

    public record NetWorthTemporalSeriesPoint(LocalDateTime valueDate, MoneyAmount value) {
    }

    public record PerformanceSummary(
            LocalDate from,
            LocalDate to,
            MoneyAmount startValue,
            MoneyAmount endValue,
            MoneyAmount grossChange,
            Optional<Percentage> grossChangePercentage,
            MoneyAmount netExternalFlow,
            MoneyAmount flowAdjustedGain,
            Optional<Percentage> flowAdjustedGainPercentage
    ) {
    }
}
