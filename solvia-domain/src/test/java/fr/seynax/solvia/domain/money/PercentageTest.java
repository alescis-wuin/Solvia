package fr.seynax.solvia.domain.money;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class PercentageTest {

    @Test
    void convertsPercentToRatio() {
        Percentage percentage = Percentage.ofPercent(new BigDecimal("12.5"));

        assertEquals(0, new BigDecimal("0.125").compareTo(percentage.ratio()));
        assertEquals(0, new BigDecimal("12.5").compareTo(percentage.asPercent()));
    }
}
