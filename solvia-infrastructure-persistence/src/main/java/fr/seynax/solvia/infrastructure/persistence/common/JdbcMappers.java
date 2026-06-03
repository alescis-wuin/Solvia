package fr.seynax.solvia.infrastructure.persistence.common;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import fr.seynax.solvia.domain.money.CurrencyCode;
import fr.seynax.solvia.domain.money.MoneyAmount;

public final class JdbcMappers {

    private JdbcMappers() {
    }

    public static OffsetDateTime toOffsetDateTime(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    public static Instant instant(ResultSet resultSet, String column) throws SQLException {
        return resultSet.getObject(column, OffsetDateTime.class).toInstant();
    }

    public static MoneyAmount money(ResultSet resultSet, String amountColumn, String currencyColumn) throws SQLException {
        BigDecimal amount = resultSet.getBigDecimal(amountColumn);
        CurrencyCode currency = CurrencyCode.of(resultSet.getString(currencyColumn));
        return MoneyAmount.of(amount, currency);
    }
}
