package fr.seynax.solvia.desktop.ui;

public record DashboardTimeStep(String code, String label, String apiBucket, String help, boolean finerThanDay) {

    public static DashboardTimeStep of(String code, String label, String apiBucket, String help, boolean finerThanDay) {
        return new DashboardTimeStep(code, label, apiBucket, help, finerThanDay);
    }

    @Override
    public String toString() {
        return label;
    }
}
