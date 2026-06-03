package fr.seynax.solvia.infrastructure.persistence.position;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import fr.seynax.solvia.domain.model.Position;
import fr.seynax.solvia.infrastructure.persistence.common.JdbcMappers;

@Repository
public class PositionJdbcRepository {

    private final JdbcClient jdbcClient;

    public PositionJdbcRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void save(Position position) {
        jdbcClient.sql("""
                insert into positions (id, account_id, asset_id, quantity, active, created_at)
                values (:id, :accountId, :assetId, :quantity, :active, :createdAt)
                on conflict (id) do update set
                    quantity = excluded.quantity,
                    active = excluded.active
                """)
                .param("id", position.id())
                .param("accountId", position.accountId())
                .param("assetId", position.assetId())
                .param("quantity", position.quantity())
                .param("active", position.active())
                .param("createdAt", JdbcMappers.toOffsetDateTime(position.createdAt()))
                .update();
    }

    public Optional<Position> findById(UUID id) {
        return jdbcClient.sql("""
                select id, account_id, asset_id, quantity, active, created_at
                from positions
                where id = :id
                """)
                .param("id", id)
                .query(this::mapPosition)
                .optional();
    }

    public List<Position> findByAccountId(UUID accountId) {
        return jdbcClient.sql("""
                select id, account_id, asset_id, quantity, active, created_at
                from positions
                where account_id = :accountId
                order by created_at asc
                """)
                .param("accountId", accountId)
                .query(this::mapPosition)
                .list();
    }

    private Position mapPosition(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Position(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("account_id", UUID.class),
                resultSet.getObject("asset_id", UUID.class),
                resultSet.getBigDecimal("quantity"),
                resultSet.getBoolean("active"),
                JdbcMappers.instant(resultSet, "created_at")
        );
    }
}
