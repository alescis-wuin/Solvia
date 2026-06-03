package fr.seynax.solvia.infrastructure.persistence.asset;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import fr.seynax.solvia.domain.model.Asset;
import fr.seynax.solvia.domain.model.AssetType;
import fr.seynax.solvia.domain.money.CurrencyCode;
import fr.seynax.solvia.infrastructure.persistence.common.JdbcMappers;

@Repository
public class AssetJdbcRepository {

    private final JdbcClient jdbcClient;

    public AssetJdbcRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void save(Asset asset) {
        jdbcClient.sql("""
                insert into assets (id, name, type, currency_code, symbol, active, created_at)
                values (:id, :name, :type, :currencyCode, :symbol, :active, :createdAt)
                on conflict (id) do update set
                    name = excluded.name,
                    type = excluded.type,
                    currency_code = excluded.currency_code,
                    symbol = excluded.symbol,
                    active = excluded.active
                """)
                .param("id", asset.id())
                .param("name", asset.name())
                .param("type", asset.type().name())
                .param("currencyCode", asset.currency().value())
                .param("symbol", asset.symbol())
                .param("active", asset.active())
                .param("createdAt", JdbcMappers.toOffsetDateTime(asset.createdAt()))
                .update();
    }

    public Optional<Asset> findById(UUID id) {
        return jdbcClient.sql("""
                select id, name, type, currency_code, symbol, active, created_at
                from assets
                where id = :id
                """)
                .param("id", id)
                .query(this::mapAsset)
                .optional();
    }

    public List<Asset> findAll() {
        return jdbcClient.sql("""
                select id, name, type, currency_code, symbol, active, created_at
                from assets
                order by name asc, created_at asc
                """)
                .query(this::mapAsset)
                .list();
    }

    private Asset mapAsset(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Asset(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("name"),
                AssetType.valueOf(resultSet.getString("type")),
                CurrencyCode.of(resultSet.getString("currency_code")),
                resultSet.getString("symbol"),
                resultSet.getBoolean("active"),
                JdbcMappers.instant(resultSet, "created_at")
        );
    }
}
