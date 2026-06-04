package fr.seynax.solvia.backend.db;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigrationInitializer implements InitializingBean {

    private final DataSource dataSource;
    private volatile boolean migrated;
    private volatile String lastError;

    public DatabaseMigrationInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() {
        migrateIfPossible();
    }

    public synchronized boolean migrateIfPossible() {
        if (migrated) {
            return true;
        }
        try {
            Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .load()
                    .migrate();
            migrated = true;
            lastError = null;
            return true;
        } catch (RuntimeException exception) {
            lastError = compactMessage(exception);
            return false;
        }
    }

    public boolean migrated() {
        return migrated;
    }

    public String lastError() {
        return lastError;
    }

    private String compactMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        String firstLine = message.lines().findFirst().orElse(exception.getClass().getSimpleName()).strip();
        return firstLine.length() > 240 ? firstLine.substring(0, 237) + "..." : firstLine;
    }
}
