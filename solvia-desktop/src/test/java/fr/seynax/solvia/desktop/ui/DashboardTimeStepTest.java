package fr.seynax.solvia.desktop.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DashboardTimeStepTest {

    @Test
    void storesDisplayCodeAndBackendBucketSeparately() {
        DashboardTimeStep step = DashboardTimeStep.of("1y", "Année", "12m", "Annual display step", false);

        assertEquals("1y", step.code());
        assertEquals("12m", step.apiBucket());
        assertEquals("Année", step.toString());
    }

    @Test
    void supportsFinerThanDayPresetsForFutureGranularity() {
        DashboardTimeStep step = DashboardTimeStep.of("1s", "Seconde", "1d", "Daily fallback", true);

        assertTrue(step.finerThanDay());
        assertEquals("1d", step.apiBucket());
    }
}
