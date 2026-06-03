package fr.seynax.solvia.infrastructure.persistence.account;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import fr.seynax.solvia.domain.model.Account;
import fr.seynax.solvia.domain.model.AccountType;
import fr.seynax.solvia.domain.model.EnvelopeType;
import fr.seynax.solvia.domain.money.CurrencyCode;
import fr.seynax.solvia.infrastructure.persistence.common.JdbcMappers;

@Repository
public class AccountJdbcRepository {

    private final JdbcClient jdbcClient;

    public AccountJdbcRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void save(Account account) {
        jdbcClient.sql("""
                insert into accounts (id, name, type, envelope_type, currency_code, active, created_at)
                values (:id, :name, :type, :envelopeType, :currencyCode, :active, :createdAt)
                on conflict (id) do update set
                    name = excluded.name,
                    type = excluded.type,
                    envelope_type = excluded.envelope_type,
                    currency_code = excluded.currency_code,
                    active = excluded.active
                """)
                .param("id", account.id())
                .param("name", account.name())
                .param("type", account.type().name())
                .param("envelopeType", account.envelopeType().name())
                .param("currencyCode", account.currency().value())
                .param("active", account.active())
                .param("createdAt", JdbcMappers.toOffsetDateTime(account.createdAt()))
                .update();
    }

    public Optional<Account> findById(UUID id) {
        return jdbcClient.sql("""
                select id, name, type, envelope_type, currency_code, active, created_at
                from accounts
                where id = :id
                """)
                .param("id", id)
                .query(this::mapAccount)
                .optional();
    }

    public List<Account> findAll() {
        return jdbcClient.sql("""
                select id, name, type, envelope_type, currency_code, active, created_at
                from accounts
                order by name asc, created_at asc
                """)
                .query(this::mapAccount)
                .list();
    }

    private Account mapAccount(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Account(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("name"),
                AccountType.valueOf(resultSet.getString("type")),
                EnvelopeType.valueOf(resultSet.getString("envelope_type")),
                CurrencyCode.of(resultSet.getString("currency_code")),
                resultSet.getBoolean("active"),
                JdbcMappers.instant(resultSet, "created_at")
        );
    }
}
