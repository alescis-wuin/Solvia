package fr.seynax.solvia.infrastructure.persistence.account;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import fr.seynax.solvia.domain.model.AccountBalanceSnapshot;
import fr.seynax.solvia.domain.model.ValuationConfidence;
import fr.seynax.solvia.infrastructure.persistence.common.JdbcMappers;

@Repository
public class AccountSnapshotJdbcRepository {

    private final JdbcClient jdbcClient;

    public AccountSnapshotJdbcRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void save(AccountBalanceSnapshot snapshot) {
        jdbcClient.sql("""
                insert into account_balance_snapshots
                    (id, account_id, value_date, amount, currency_code, confidence, note, recorded_at)
                values
                    (:id, :accountId, :valueDate, :amount, :currencyCode, :confidence, :note, :recordedAt)
                """)
                .param("id", snapshot.id())
                .param("accountId", snapshot.accountId())
                .param("valueDate", snapshot.valueDate())
                .param("amount", snapshot.balance().amount())
                .param("currencyCode", snapshot.balance().currency().value())
                .param("confidence", snapshot.confidence().name())
                .param("note", snapshot.note())
                .param("recordedAt", JdbcMappers.toOffsetDateTime(snapshot.recordedAt()))
                .update();
    }

    public List<AccountBalanceSnapshot> findByAccountId(UUID accountId) {
        return jdbcClient.sql("""
                select id, account_id, value_date, amount, currency_code, confidence, note, recorded_at
                from account_balance_snapshots
                where account_id = :accountId
                order by value_date asc, recorded_at asc
                """)
                .param("accountId", accountId)
                .query(this::mapSnapshot)
                .list();
    }

    private AccountBalanceSnapshot mapSnapshot(ResultSet resultSet, int rowNumber) throws SQLException {
        return new AccountBalanceSnapshot(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("account_id", UUID.class),
                resultSet.getObject("value_date", java.time.LocalDate.class),
                JdbcMappers.money(resultSet, "amount", "currency_code"),
                ValuationConfidence.valueOf(resultSet.getString("confidence")),
                resultSet.getString("note"),
                JdbcMappers.instant(resultSet, "recorded_at")
        );
    }
}
