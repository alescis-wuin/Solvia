package fr.seynax.solvia.desktop.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class InputValidationTest {

    @Test
    void acceptsDecimalAmountsWithDotOrComma() {
        InputValidation.ValidationResult<BigDecimal> dot = InputValidation.amount("1234.56", "Montant");
        InputValidation.ValidationResult<BigDecimal> comma = InputValidation.amount("1234,56", "Montant");

        assertTrue(dot.valid());
        assertTrue(comma.valid());
        assertEquals(0, new BigDecimal("1234.56").compareTo(dot.value()));
        assertEquals(0, new BigDecimal("1234.56").compareTo(comma.value()));
    }

    @Test
    void rejectsInvalidAmount() {
        InputValidation.ValidationResult<BigDecimal> result = InputValidation.amount("abc", "Montant");

        assertFalse(result.valid());
    }

    @Test
    void normalizesCurrencyCode() {
        InputValidation.ValidationResult<String> result = InputValidation.currencyCode(" eur ", "Devise");

        assertTrue(result.valid());
        assertEquals("EUR", result.value());
    }

    @Test
    void rejectsInvalidCurrencyCode() {
        InputValidation.ValidationResult<String> result = InputValidation.currencyCode("EURO", "Devise");

        assertFalse(result.valid());
    }
}
