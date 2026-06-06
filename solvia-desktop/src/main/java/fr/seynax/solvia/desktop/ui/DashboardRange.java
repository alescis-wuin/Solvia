package fr.seynax.solvia.desktop.ui;

import java.time.LocalDate;

public enum DashboardRange {
    WEEK("1S", 7, "1d"),
    MONTH("1M", 30, "2d"),
    QUARTER("3M", 90, "1w"),
    YEAR("1A", 365, "1m"),
    MAX("Max", 3650, "3m");

    private final String label;
    private final int days;
    private final String stepCode;

    DashboardRange(String label, int days, String stepCode) {
        this.label = label;
        this.days = days;
        this.stepCode = stepCode;
    }

    public String label() {
        return label;
    }

    public String stepCode() {
        return stepCode;
    }

    public LocalDate startDate(LocalDate endDate) {
        LocalDate safeEnd = endDate == null ? LocalDate.now() : endDate;
        return safeEnd.minusDays(Math.max(1, days) - 1L);
    }
}
