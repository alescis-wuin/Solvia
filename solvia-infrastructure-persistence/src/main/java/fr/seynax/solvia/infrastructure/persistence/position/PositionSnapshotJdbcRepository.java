package fr.seynax.solvia.infrastructure.persistence.position;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import fr.seynax.solvia.domain.model.PositionSnapshot;
import fr.seynax.solvia.domain.model.ValuationConfidence;
import fr.seynax.solvia.infrastructure.persistence.common.JdbcMappers;

@Repository
public class PositionSnapshotJdbcRepository {

    private final JdbcClient jdbcClient;

    public PositionSnapshotJdbcRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void save(PositionSnapshot snapshot) {
        jdbcClient.sql("""
                insert into position_snapshots
                    (id, position_id, value_date, quantity, market_value_amount, market_value_currency_code, confidence, recorded_at)
                values
                    (:id, :positionId, :valueDate, :quantity, :marketValueAmount, :marketValueCurrencyCode, :confidence, :recordedAt)
                """)
                .param("id", snapshot.id())
                .param("positionId", snapshot.positionId())
                .param("valueDate", snapshot.valueDate())
                .param("quantity", snapshot.quantity())
                .param("marketValueAmount", snapshot.marketValue().amount())
                .param("marketValueCurrencyCode", snapshot.marketValue().currency().value())
                .param("confidence", snapshot.confidence().name())
                .param("recordedAt", JdbcMappers.toOffsetDateTime(snapshot.recordedAt()))
                .update();
    }

    public List<PositionSnapshot> findByPositionId(UUID positionId) {
        return jdbcClient.sql("""
                select id, position_id, value_date, quantity, market_value_amount,
                       market_value_currency_code, confidence, recorded_at
                from position_snapshots
                where position_id = :positionId
                order by value_date asc, recorded_at asc
                """)
                .param("positionId", positionId)
                .query(this::mapSnapshot)
                .list();
    }

    private PositionSnapshot mapSnapshot(ResultSet resultSet, int rowNumber) throws SQLException {
        return new PositionSnapshot(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("position_id", UUID.class),
                resultSet.getObject("value_date", java.time.LocalDate.class),
                resultSet.getBigDecimal("quantity"),
                JdbcMappers.money(resultSet, "market_value_amount", "market_value_currency_code"),
                ValuationConfidence.valueOf(resultSet.getString("confidence")),
                JdbcMappers.instant(resultSet, "recorded_at")
        );
    }
}
