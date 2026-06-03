package fr.seynax.solvia.infrastructure.persistence.market;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import fr.seynax.solvia.domain.model.FxRate;
import fr.seynax.solvia.domain.money.CurrencyCode;
import fr.seynax.solvia.infrastructure.persistence.common.JdbcMappers;

@Repository
public class FxRateJdbcRepository {

    private final JdbcClient jdbcClient;

    public FxRateJdbcRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void save(FxRate fxRate) {
        jdbcClient.sql("""
                insert into fx_rates
                    (id, base_currency_code, quote_currency_code, rate_date, rate, source, recorded_at)
                values
                    (:id, :baseCurrencyCode, :quoteCurrencyCode, :rateDate, :rate, :source, :recordedAt)
                """)
                .param("id", fxRate.id())
                .param("baseCurrencyCode", fxRate.baseCurrency().value())
                .param("quoteCurrencyCode", fxRate.quoteCurrency().value())
                .param("rateDate", fxRate.rateDate())
                .param("rate", fxRate.rate())
                .param("source", fxRate.source())
                .param("recordedAt", JdbcMappers.toOffsetDateTime(fxRate.recordedAt()))
                .update();
    }

    public Optional<FxRate> findLatest(CurrencyCode baseCurrency, CurrencyCode quoteCurrency) {
        return jdbcClient.sql("""
                select id, base_currency_code, quote_currency_code, rate_date, rate, source, recorded_at
                from fx_rates
                where base_currency_code = :baseCurrencyCode
                  and quote_currency_code = :quoteCurrencyCode
                order by rate_date desc, recorded_at desc
                limit 1
                """)
                .param("baseCurrencyCode", baseCurrency.value())
                .param("quoteCurrencyCode", quoteCurrency.value())
                .query(this::mapFxRate)
                .optional();
    }

    public List<FxRate> findAll() {
        return jdbcClient.sql("""
                select id, base_currency_code, quote_currency_code, rate_date, rate, source, recorded_at
                from fx_rates
                order by rate_date desc, recorded_at desc
                """)
                .query(this::mapFxRate)
                .list();
    }

    private FxRate mapFxRate(ResultSet resultSet, int rowNumber) throws SQLException {
        return new FxRate(
                resultSet.getObject("id", java.util.UUID.class),
                CurrencyCode.of(resultSet.getString("base_currency_code")),
                CurrencyCode.of(resultSet.getString("quote_currency_code")),
                resultSet.getObject("rate_date", java.time.LocalDate.class),
                resultSet.getBigDecimal("rate"),
                resultSet.getString("source"),
                JdbcMappers.instant(resultSet, "recorded_at")
        );
    }
}
