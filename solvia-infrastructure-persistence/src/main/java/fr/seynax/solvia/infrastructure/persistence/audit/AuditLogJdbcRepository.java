package fr.seynax.solvia.infrastructure.persistence.audit;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import fr.seynax.solvia.infrastructure.persistence.common.JdbcMappers;

@Repository
public class AuditLogJdbcRepository {

    private final JdbcClient jdbcClient;

    public AuditLogJdbcRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void save(AuditLogEntry entry) {
        jdbcClient.sql("""
                insert into audit_logs
                    (id, occurred_at, actor, action, entity_type, entity_id, details_json)
                values
                    (:id, :occurredAt, :actor, :eventName, :entityType, :entityId, cast(:detailsJson as jsonb))
                """)
                .param("id", entry.id())
                .param("occurredAt", JdbcMappers.toOffsetDateTime(entry.occurredAt()))
                .param("actor", entry.actor())
                .param("eventName", entry.eventName())
                .param("entityType", entry.entityType())
                .param("entityId", entry.entityId())
                .param("detailsJson", entry.detailsJson())
                .update();
    }

    public List<AuditLogEntry> findLatest(int limit) {
        return jdbcClient.sql("""
                select id, occurred_at, actor, action, entity_type, entity_id, details_json::text as details_json
                from audit_logs
                order by occurred_at desc
                limit :limit
                """)
                .param("limit", limit)
                .query(this::mapEntry)
                .list();
    }

    private AuditLogEntry mapEntry(ResultSet resultSet, int rowNumber) throws SQLException {
        return new AuditLogEntry(
                resultSet.getObject("id", UUID.class),
                JdbcMappers.instant(resultSet, "occurred_at"),
                resultSet.getString("actor"),
                resultSet.getString("action"),
                resultSet.getString("entity_type"),
                resultSet.getObject("entity_id", UUID.class),
                resultSet.getString("details_json")
        );
    }
}
