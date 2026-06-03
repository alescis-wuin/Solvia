package fr.seynax.solvia.infrastructure.persistence.market;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import fr.seynax.solvia.domain.model.MarketPrice;
import fr.seynax.solvia.infrastructure.persistence.common.JdbcMappers;

@Repository
public class MarketPriceJdbcRepository {

    private final JdbcClient jdbcClient;

    public MarketPriceJdbcRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void save(MarketPrice marketPrice) {
        jdbcClient.sql("""
                insert into market_prices
                    (id, asset_id, price_date, price_amount, price_currency_code, source, recorded_at)
                values
                    (:id, :assetId, :priceDate, :priceAmount, :priceCurrencyCode, :source, :recordedAt)
                """)
                .param("id", marketPrice.id())
                .param("assetId", marketPrice.assetId())
                .param("priceDate", marketPrice.priceDate())
                .param("priceAmount", marketPrice.price().amount())
                .param("priceCurrencyCode", marketPrice.price().currency().value())
                .param("source", marketPrice.source())
                .param("recordedAt", JdbcMappers.toOffsetDateTime(marketPrice.recordedAt()))
                .update();
    }

    public Optional<MarketPrice> findLatestByAssetId(UUID assetId) {
        return jdbcClient.sql("""
                select id, asset_id, price_date, price_amount, price_currency_code, source, recorded_at
                from market_prices
                where asset_id = :assetId
                order by price_date desc, recorded_at desc
                limit 1
                """)
                .param("assetId", assetId)
                .query(this::mapMarketPrice)
                .optional();
    }

    public List<MarketPrice> findByAssetId(UUID assetId) {
        return jdbcClient.sql("""
                select id, asset_id, price_date, price_amount, price_currency_code, source, recorded_at
                from market_prices
                where asset_id = :assetId
                order by price_date asc, recorded_at asc
                """)
                .param("assetId", assetId)
                .query(this::mapMarketPrice)
                .list();
    }

    private MarketPrice mapMarketPrice(ResultSet resultSet, int rowNumber) throws SQLException {
        return new MarketPrice(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("asset_id", UUID.class),
                resultSet.getObject("price_date", java.time.LocalDate.class),
                JdbcMappers.money(resultSet, "price_amount", "price_currency_code"),
                resultSet.getString("source"),
                JdbcMappers.instant(resultSet, "recorded_at")
        );
    }
}
