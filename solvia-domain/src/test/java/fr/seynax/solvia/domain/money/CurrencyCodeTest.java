package fr.seynax.solvia.domain.money;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CurrencyCodeTest {

    @Test
    void normalizesCurrencyCode() {
        CurrencyCode code = CurrencyCode.of(" eur ");

        assertEquals("EUR", code.value());
    }

    @Test
    void rejectsInvalidCurrencyCode() {
        assertThrows(IllegalArgumentException.class, () -> CurrencyCode.of("EURO"));
        assertThrows(IllegalArgumentException.class, () -> CurrencyCode.of("12€"));
    }
}
