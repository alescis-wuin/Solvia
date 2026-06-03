package fr.seynax.solvia.backend.api.calculation;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.seynax.solvia.backend.api.money.MoneyResponse;
import fr.seynax.solvia.domain.calculation.NetWorthCalculator;
import fr.seynax.solvia.domain.calculation.NetWorthCalculator.AggregationMode;
import fr.seynax.solvia.domain.calculation.NetWorthCalculator.AssetTypeValuation;
import fr.seynax.solvia.domain.calculation.NetWorthCalculator.NetWorthSeriesPoint;
import fr.seynax.solvia.domain.calculation.NetWorthCalculator.NetWorthValuation;
import fr.seynax.solvia.domain.calculation.NetWorthCalculator.PerformanceSummary;
import fr.seynax.solvia.domain.calculation.NetWorthCalculator.TimeBucket;
import fr.seynax.solvia.domain.calculation.NetWorthCalculator.TimeBucketUnit;
import fr.seynax.solvia.domain.model.AssetType;
import fr.seynax.solvia.domain.money.CurrencyCode;

@RestController
@RequestMapping("/api")
public class CalculationController {

    private final CalculationDataLoader dataLoader;
    private final NetWorthCalculator calculator = new NetWorthCalculator();

    public CalculationController(CalculationDataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    @GetMapping("/net-worth")
    public NetWorthResponse netWorth(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "EUR") String currency
    ) {
        NetWorthValuation valuation = calculator.valueAt(dataLoader.load(), date, CurrencyCode.of(currency));
        return NetWorthResponse.from(valuation);
    }

    @GetMapping("/net-worth/series")
    public List<SeriesPointResponse> netWorthSeries(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "1d") String bucket,
            @RequestParam(defaultValue = "last") String aggregation,
            @RequestParam(defaultValue = "EUR") String currency
    ) {
        return calculator.series(
                        dataLoader.load(),
                        from,
                        to,
                        parseBucket(bucket),
                        parseAggregation(aggregation),
                        CurrencyCode.of(currency)
                ).stream()
                .map(SeriesPointResponse::from)
                .toList();
    }

    @GetMapping("/performance")
    public PerformanceResponse performance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "EUR") String currency
    ) {
        PerformanceSummary performance = calculator.performance(dataLoader.load(), from, to, CurrencyCode.of(currency));
        return PerformanceResponse.from(performance);
    }

    @GetMapping("/allocation")
    public List<AssetTypeValueResponse> allocation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "EUR") String currency
    ) {
        return calculator.valueAt(dataLoader.load(), date, CurrencyCode.of(currency))
                .assetTypeValues()
                .stream()
                .map(AssetTypeValueResponse::from)
                .toList();
    }

    private AggregationMode parseAggregation(String aggregation) {
        return switch (aggregation.strip().toLowerCase()) {
            case "last", "last_known" -> AggregationMode.LAST_KNOWN;
            case "average", "avg" -> AggregationMode.AVERAGE;
            default -> throw new IllegalArgumentException("Unsupported aggregation: " + aggregation);
        };
    }

    private TimeBucket parseBucket(String bucket) {
        String normalized = bucket.strip().toLowerCase();
        if (normalized.length() < 2) {
            throw new IllegalArgumentException("Bucket must use a positive amount and unit, for example 2d");
        }
        String amountPart = normalized.substring(0, normalized.length() - 1);
        char unitPart = normalized.charAt(normalized.length() - 1);
        int amount = Integer.parseInt(amountPart);
        TimeBucketUnit unit = switch (unitPart) {
            case 'd' -> TimeBucketUnit.DAYS;
            case 'w' -> TimeBucketUnit.WEEKS;
            case 'm' -> TimeBucketUnit.MONTHS;
            default -> throw new IllegalArgumentException("Unsupported bucket unit: " + unitPart);
        };
        return new TimeBucket(amount, unit);
    }

    public record NetWorthResponse(
            LocalDate valueDate,
            MoneyResponse total,
            List<AccountValueResponse> accounts,
            List<AssetTypeValueResponse> allocation
    ) {
        static NetWorthResponse from(NetWorthValuation valuation) {
            return new NetWorthResponse(
                    valuation.valueDate(),
                    MoneyResponse.from(valuation.total()),
                    valuation.accountValues().stream()
                            .map(value -> new AccountValueResponse(value.accountId(), value.accountName(), MoneyResponse.from(value.value())))
                            .toList(),
                    valuation.assetTypeValues().stream().map(AssetTypeValueResponse::from).toList()
            );
        }
    }

    public record AssetTypeValueResponse(AssetType assetType, MoneyResponse value, PercentageResponse allocation) {
        static AssetTypeValueResponse from(AssetTypeValuation valuation) {
            return new AssetTypeValueResponse(
                    valuation.assetType(),
                    MoneyResponse.from(valuation.value()),
                    PercentageResponse.from(valuation.allocation())
            );
        }
    }

    public record SeriesPointResponse(LocalDate valueDate, MoneyResponse value) {
        static SeriesPointResponse from(NetWorthSeriesPoint point) {
            return new SeriesPointResponse(point.valueDate(), MoneyResponse.from(point.value()));
        }
    }

    public record PerformanceResponse(
            LocalDate from,
            LocalDate to,
            MoneyResponse startValue,
            MoneyResponse endValue,
            MoneyResponse grossChange,
            PercentageResponse grossChangePercentage,
            MoneyResponse netExternalFlow,
            MoneyResponse flowAdjustedGain,
            PercentageResponse flowAdjustedGainPercentage
    ) {
        static PerformanceResponse from(PerformanceSummary performance) {
            return new PerformanceResponse(
                    performance.from(),
                    performance.to(),
                    MoneyResponse.from(performance.startValue()),
                    MoneyResponse.from(performance.endValue()),
                    MoneyResponse.from(performance.grossChange()),
                    performance.grossChangePercentage().map(PercentageResponse::from).orElse(null),
                    MoneyResponse.from(performance.netExternalFlow()),
                    MoneyResponse.from(performance.flowAdjustedGain()),
                    performance.flowAdjustedGainPercentage().map(PercentageResponse::from).orElse(null)
            );
        }
    }
}
