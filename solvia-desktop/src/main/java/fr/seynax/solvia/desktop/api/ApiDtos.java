package fr.seynax.solvia.desktop.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ApiDtos {

    private ApiDtos() {
    }

    public record MoneyDto(BigDecimal amount, String currencyCode) {
    }

    public record ReadinessDto(
            String status,
            String service,
            Instant checkedAt,
            String backendStatus,
            String databaseStatus,
            String message
    ) {
    }

    public record AccountDto(
            UUID id,
            String name,
            String type,
            String envelopeType,
            String currencyCode,
            boolean active,
            Instant createdAt
    ) {
        @Override
        public String toString() {
            return name + " (" + currencyCode + ")";
        }
    }

    public record AccountCreateDto(String name, String type, String envelopeType, String currencyCode) {
    }

    public record AccountSnapshotCreateDto(
            UUID accountId,
            LocalDate valueDate,
            MoneyDto balance,
            String confidence,
            String note
    ) {
    }

    public record CashFlowCreateDto(
            UUID accountId,
            String type,
            LocalDate valueDate,
            MoneyDto amount,
            String label
    ) {
    }

    public record PercentageDto(BigDecimal ratio, BigDecimal percent) {
    }

    public record AccountValueDto(UUID accountId, String accountName, MoneyDto value) {
    }

    public record AssetTypeValueDto(String assetType, MoneyDto value, PercentageDto allocation) {
    }

    public record NetWorthDto(
            LocalDate valueDate,
            MoneyDto total,
            List<AccountValueDto> accounts,
            List<AssetTypeValueDto> allocation
    ) {
    }

    public record SeriesPointDto(LocalDate valueDate, MoneyDto value) {
    }

    public record PerformanceDto(
            LocalDate from,
            LocalDate to,
            MoneyDto startValue,
            MoneyDto endValue,
            MoneyDto grossChange,
            PercentageDto grossChangePercentage,
            MoneyDto netExternalFlow,
            MoneyDto flowAdjustedGain,
            PercentageDto flowAdjustedGainPercentage
    ) {
    }
}
