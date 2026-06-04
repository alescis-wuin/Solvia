package fr.seynax.solvia.backend.health;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import fr.seynax.solvia.backend.api.MockMvcSupport;
import fr.seynax.solvia.backend.db.DatabaseMigrationInitializer;

class ReadinessControllerTest {

    @Test
    void returnsUpWhenDatabaseConnectionAndMigrationAreReady() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMigrationInitializer migrations = mock(DatabaseMigrationInitializer.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);
        when(migrations.migrateIfPossible()).thenReturn(true);

        MockMvc mvc = MockMvcSupport.standaloneMvc(new ReadinessController(dataSource, migrations));

        mvc.perform(get("/api/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.backendStatus").value("UP"))
                .andExpect(jsonPath("$.databaseStatus").value("UP"));
    }

    @Test
    void returnsDegradedWhenMigrationCannotRun() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMigrationInitializer migrations = mock(DatabaseMigrationInitializer.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);
        when(migrations.migrateIfPossible()).thenReturn(false);
        when(migrations.lastError()).thenReturn("migration failed");

        MockMvc mvc = MockMvcSupport.standaloneMvc(new ReadinessController(dataSource, migrations));

        mvc.perform(get("/api/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEGRADED"))
                .andExpect(jsonPath("$.backendStatus").value("UP"))
                .andExpect(jsonPath("$.databaseStatus").value("DOWN"));
    }
}
