package fr.seynax.solvia.infrastructure.persistence.importbatch;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import fr.seynax.solvia.infrastructure.persistence.common.JdbcMappers;

@Repository
public class ImportBatchJdbcRepository {

    private final JdbcClient jdbcClient;

    public ImportBatchJdbcRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void save(ImportBatch batch) {
        jdbcClient.sql("""
                insert into import_batches
                    (id, format, source_name, imported_at, item_count, status, error_message)
                values
                    (:id, :format, :sourceName, :importedAt, :itemCount, :status, :errorMessage)
                """)
                .param("id", batch.id())
                .param("format", batch.format().name())
                .param("sourceName", batch.sourceName())
                .param("importedAt", JdbcMappers.toOffsetDateTime(batch.importedAt()))
                .param("itemCount", batch.itemCount())
                .param("status", batch.status().name())
                .param("errorMessage", batch.errorMessage())
                .update();
    }

    public Optional<ImportBatch> findById(UUID id) {
        return jdbcClient.sql("""
                select id, format, source_name, imported_at, item_count, status, error_message
                from import_batches
                where id = :id
                """)
                .param("id", id)
                .query(this::mapBatch)
                .optional();
    }

    public List<ImportBatch> findLatest(int limit) {
        return jdbcClient.sql("""
                select id, format, source_name, imported_at, item_count, status, error_message
                from import_batches
                order by imported_at desc
                limit :limit
                """)
                .param("limit", limit)
                .query(this::mapBatch)
                .list();
    }

    private ImportBatch mapBatch(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ImportBatch(
                resultSet.getObject("id", UUID.class),
                ImportFormat.valueOf(resultSet.getString("format")),
                resultSet.getString("source_name"),
                JdbcMappers.instant(resultSet, "imported_at"),
                resultSet.getInt("item_count"),
                ImportStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("error_message")
        );
    }
}
