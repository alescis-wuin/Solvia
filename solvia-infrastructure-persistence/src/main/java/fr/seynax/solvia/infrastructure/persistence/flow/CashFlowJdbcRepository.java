package fr.seynax.solvia.infrastructure.persistence.flow;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import fr.seynax.solvia.domain.model.CashFlow;
import fr.seynax.solvia.domain.model.CashFlowType;
import fr.seynax.solvia.infrastructure.persistence.common.JdbcMappers;

@Repository
public class CashFlowJdbcRepository {

    private final JdbcClient jdbcClient;

    public CashFlowJdbcRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void save(CashFlow cashFlow) {
        jdbcClient.sql("""
                insert into cash_flows
                    (id, account_id, type, value_date, amount, currency_code, label, recorded_at)
                values
                    (:id, :accountId, :type, :valueDate, :amount, :currencyCode, :label, :recordedAt)
                """)
                .param("id", cashFlow.id())
                .param("accountId", cashFlow.accountId())
                .param("type", cashFlow.type().name())
                .param("valueDate", cashFlow.valueDate())
                .param("amount", cashFlow.amount().amount())
                .param("currencyCode", cashFlow.amount().currency().value())
                .param("label", cashFlow.label())
                .param("recordedAt", JdbcMappers.toOffsetDateTime(cashFlow.recordedAt()))
                .update();
    }

    public List<CashFlow> findByAccountId(UUID accountId) {
        return jdbcClient.sql("""
                select id, account_id, type, value_date, amount, currency_code, label, recorded_at
                from cash_flows
                where account_id = :accountId
                order by value_date asc, recorded_at asc
                """)
                .param("accountId", accountId)
                .query(this::mapCashFlow)
                .list();
    }

    private CashFlow mapCashFlow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new CashFlow(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("account_id", UUID.class),
                CashFlowType.valueOf(resultSet.getString("type")),
                resultSet.getObject("value_date", java.time.LocalDate.class),
                JdbcMappers.money(resultSet, "amount", "currency_code"),
                resultSet.getString("label"),
                JdbcMappers.instant(resultSet, "recorded_at")
        );
    }
}
