package fr.seynax.solvia.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import fr.seynax.solvia.domain.model.Account;
import fr.seynax.solvia.domain.model.AccountBalanceSnapshot;
import fr.seynax.solvia.domain.model.AccountType;
import fr.seynax.solvia.domain.model.Asset;
import fr.seynax.solvia.domain.model.AssetType;
import fr.seynax.solvia.domain.model.CashFlow;
import fr.seynax.solvia.domain.model.CashFlowType;
import fr.seynax.solvia.domain.model.EnvelopeType;
import fr.seynax.solvia.domain.model.FxRate;
import fr.seynax.solvia.domain.model.MarketPrice;
import fr.seynax.solvia.domain.model.Position;
import fr.seynax.solvia.domain.model.PositionSnapshot;
import fr.seynax.solvia.domain.money.CurrencyCode;
import fr.seynax.solvia.domain.money.MoneyAmount;
import fr.seynax.solvia.infrastructure.persistence.account.AccountJdbcRepository;
import fr.seynax.solvia.infrastructure.persistence.account.AccountSnapshotJdbcRepository;
import fr.seynax.solvia.infrastructure.persistence.asset.AssetJdbcRepository;
import fr.seynax.solvia.infrastructure.persistence.audit.AuditLogEntry;
import fr.seynax.solvia.infrastructure.persistence.audit.AuditLogJdbcRepository;
import fr.seynax.solvia.infrastructure.persistence.flow.CashFlowJdbcRepository;
import fr.seynax.solvia.infrastructure.persistence.importbatch.ImportBatch;
import fr.seynax.solvia.infrastructure.persistence.importbatch.ImportBatchJdbcRepository;
import fr.seynax.solvia.infrastructure.persistence.importbatch.ImportFormat;
import fr.seynax.solvia.infrastructure.persistence.market.FxRateJdbcRepository;
import fr.seynax.solvia.infrastructure.persistence.market.MarketPriceJdbcRepository;
import fr.seynax.solvia.infrastructure.persistence.position.PositionJdbcRepository;
import fr.seynax.solvia.infrastructure.persistence.position.PositionSnapshotJdbcRepository;

@Testcontainers
class PersistenceIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.4-alpine")
            .withDatabaseName("solvia")
            .withUsername("solvia")
            .withPassword("solvia-local");

    private static HikariDataSource dataSource;
    private static JdbcClient jdbcClient;

    @BeforeAll
    static void setUpDatabase() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(POSTGRES.getJdbcUrl());
        config.setUsername(POSTGRES.getUsername());
        config.setPassword(POSTGRES.getPassword());
        dataSource = new HikariDataSource(config);

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        jdbcClient = JdbcClient.create(dataSource);
    }

    @AfterAll
    static void closeDataSource() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    void flywayCreatesExpectedTables() {
        List<String> tables = jdbcClient.sql("""
                select table_name
                from information_schema.tables
                where table_schema = 'public'
                order by table_name
                """)
                .query(String.class)
                .list();

        assertTrue(tables.contains("accounts"));
        assertTrue(tables.contains("assets"));
        assertTrue(tables.contains("positions"));
        assertTrue(tables.contains("account_balance_snapshots"));
        assertTrue(tables.contains("position_snapshots"));
        assertTrue(tables.contains("cash_flows"));
        assertTrue(tables.contains("market_prices"));
        assertTrue(tables.contains("fx_rates"));
        assertTrue(tables.contains("import_batches"));
        assertTrue(tables.contains("audit_logs"));
        assertTrue(tables.contains("flyway_schema_history"));
    }

    @Test
    void storesAndReadsAccountsAssetsPositionsSnapshotsAndFlows() {
        AccountJdbcRepository accountRepository = new AccountJdbcRepository(jdbcClient);
        AssetJdbcRepository assetRepository = new AssetJdbcRepository(jdbcClient);
        PositionJdbcRepository positionRepository = new PositionJdbcRepository(jdbcClient);
        AccountSnapshotJdbcRepository accountSnapshotRepository = new AccountSnapshotJdbcRepository(jdbcClient);
        PositionSnapshotJdbcRepository positionSnapshotRepository = new PositionSnapshotJdbcRepository(jdbcClient);
        CashFlowJdbcRepository cashFlowRepository = new CashFlowJdbcRepository(jdbcClient);

        Account account = Account.create("PEA", AccountType.PEA, EnvelopeType.PEA, CurrencyCode.eur());
        Asset asset = Asset.create("MSCI World ETF", AssetType.ETF, CurrencyCode.eur(), "CW8");
        Position position = Position.create(account.id(), asset.id(), new BigDecimal("3.5"));
        AccountBalanceSnapshot accountSnapshot = AccountBalanceSnapshot.observed(
                account.id(),
                LocalDate.of(2026, 6, 1),
                MoneyAmount.of(new BigDecimal("1200.75"), CurrencyCode.eur())
        );
        PositionSnapshot positionSnapshot = PositionSnapshot.observed(
                position.id(),
                LocalDate.of(2026, 6, 1),
                new BigDecimal("3.5"),
                MoneyAmount.of(new BigDecimal("1800.50"), CurrencyCode.eur())
        );
        CashFlow cashFlow = CashFlow.of(
                account.id(),
                CashFlowType.DEPOSIT,
                LocalDate.of(2026, 6, 1),
                MoneyAmount.of(new BigDecimal("500"), CurrencyCode.eur()),
                "Initial deposit"
        );

        accountRepository.save(account);
        assetRepository.save(asset);
        positionRepository.save(position);
        accountSnapshotRepository.save(accountSnapshot);
        positionSnapshotRepository.save(positionSnapshot);
        cashFlowRepository.save(cashFlow);

        Account persistedAccount = accountRepository.findById(account.id()).orElseThrow();
        Asset persistedAsset = assetRepository.findById(asset.id()).orElseThrow();
        Position persistedPosition = positionRepository.findById(position.id()).orElseThrow();

        assertEquals(account.id(), persistedAccount.id());
        assertEquals(account.name(), persistedAccount.name());
        assertEquals(asset.id(), persistedAsset.id());
        assertEquals(asset.symbol(), persistedAsset.symbol());
        assertEquals(position.id(), persistedPosition.id());
        assertEquals(0, position.quantity().compareTo(persistedPosition.quantity()));
        assertEquals(1, accountSnapshotRepository.findByAccountId(account.id()).size());
        assertEquals(1, positionSnapshotRepository.findByPositionId(position.id()).size());
        assertEquals(1, cashFlowRepository.findByAccountId(account.id()).size());
    }

    @Test
    void storesAndReadsMarketDataImportsAndAuditEntries() {
        AccountJdbcRepository accountRepository = new AccountJdbcRepository(jdbcClient);
        AssetJdbcRepository assetRepository = new AssetJdbcRepository(jdbcClient);
        MarketPriceJdbcRepository marketPriceRepository = new MarketPriceJdbcRepository(jdbcClient);
        FxRateJdbcRepository fxRateRepository = new FxRateJdbcRepository(jdbcClient);
        ImportBatchJdbcRepository importBatchRepository = new ImportBatchJdbcRepository(jdbcClient);
        AuditLogJdbcRepository auditLogRepository = new AuditLogJdbcRepository(jdbcClient);

        Account account = Account.create("Crypto wallet", AccountType.CRYPTO_WALLET, EnvelopeType.CRYPTO, CurrencyCode.eur());
        Asset asset = Asset.create("Bitcoin", AssetType.CRYPTO_ASSET, CurrencyCode.usd(), "BTC");
        MarketPrice price = MarketPrice.of(
                asset.id(),
                LocalDate.of(2026, 6, 1),
                MoneyAmount.of(new BigDecimal("68000"), CurrencyCode.usd()),
                "manual"
        );
        FxRate fxRate = FxRate.of(
                CurrencyCode.usd(),
                CurrencyCode.eur(),
                LocalDate.of(2026, 6, 1),
                new BigDecimal("0.92"),
                "manual"
        );
        ImportBatch importBatch = ImportBatch.imported(ImportFormat.CSV, "manual.csv", 12);
        AuditLogEntry auditLogEntry = AuditLogEntry.create(
                "local-user",
                "ASSET_CREATED",
                "Asset",
                asset.id(),
                "{\"source\":\"test\"}"
        );

        accountRepository.save(account);
        assetRepository.save(asset);
        marketPriceRepository.save(price);
        fxRateRepository.save(fxRate);
        importBatchRepository.save(importBatch);
        auditLogRepository.save(auditLogEntry);

        MarketPrice persistedPrice = marketPriceRepository.findLatestByAssetId(asset.id()).orElseThrow();
        FxRate persistedFxRate = fxRateRepository.findLatest(CurrencyCode.usd(), CurrencyCode.eur()).orElseThrow();
        ImportBatch persistedImportBatch = importBatchRepository.findById(importBatch.id()).orElseThrow();

        assertEquals(price.id(), persistedPrice.id());
        assertEquals(0, price.price().amount().compareTo(persistedPrice.price().amount()));
        assertEquals(fxRate.id(), persistedFxRate.id());
        assertEquals(0, fxRate.rate().compareTo(persistedFxRate.rate()));
        assertEquals(importBatch.id(), persistedImportBatch.id());
        assertEquals(importBatch.itemCount(), persistedImportBatch.itemCount());
        assertFalse(auditLogRepository.findLatest(5).isEmpty());
    }
}
