package fr.seynax.solvia.desktop.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DashboardTimeStepTest {

    @Test
    void storesDisplayCodeAndBackendBucketSeparatelyWhenNeeded() {
        DashboardTimeStep step = DashboardTimeStep.of("1y", "Année", "1y", "Annual display step", false);

        assertEquals("1y", step.code());
        assertEquals("1y", step.apiBucket());
        assertEquals("Année", step.toString());
    }

    @Test
    void supportsFinerThanDayPresetsAsRealBackendBuckets() {
        DashboardTimeStep step = DashboardTimeStep.of("1s", "Seconde", "1s", "Second-level bucket", true);

        assertTrue(step.finerThanDay());
        assertEquals("1s", step.apiBucket());
    }
}
