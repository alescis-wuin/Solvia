package fr.seynax.solvia.backend;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class SolviaBackendApplicationTest {

    @Test
    void exposesApplicationEntryPoint() {
        assertNotNull(SolviaBackendApplication.class);
    }
}
