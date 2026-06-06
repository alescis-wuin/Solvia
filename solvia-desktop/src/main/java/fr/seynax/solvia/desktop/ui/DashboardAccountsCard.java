package fr.seynax.solvia.desktop.ui;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import fr.seynax.solvia.desktop.api.ApiDtos.AccountValueDto;
import fr.seynax.solvia.desktop.api.ApiDtos.MoneyDto;

public final class DashboardAccountsCard extends SectionCard {

    private final TilePane tiles = new TilePane();
    private final EmptyState empty = new EmptyState();

    public DashboardAccountsCard() {
        super("Mes comptes", "Comptes qui contribuent au patrimoine affiché.");
        tiles.getStyleClass().add("dashboard-account-grid");
        tiles.setHgap(14);
        tiles.setVgap(14);
        tiles.setPrefColumns(3);
        getChildren().addAll(empty, tiles);
        empty.show("Aucun compte valorisé", "Les comptes apparaîtront après la saisie de valeurs.");
    }

    public void update(List<AccountValueDto> accounts) {
        List<AccountRow> rows = accounts == null ? List.of() : accounts.stream()
                .map(AccountRow::from)
                .sorted(Comparator.comparing(AccountRow::sortKey).reversed())
                .toList();
        tiles.getChildren().setAll(rows.stream().map(this::tile).toList());
        if (rows.isEmpty()) {
            empty.show("Aucun compte valorisé", "Aucun compte n'a de valeur disponible sur cette période.");
            tiles.setVisible(false);
            tiles.setManaged(false);
        } else {
            empty.hide();
            tiles.setVisible(true);
            tiles.setManaged(true);
        }
    }

    public void clear() {
        tiles.getChildren().clear();
        tiles.setVisible(false);
        tiles.setManaged(false);
        empty.show("Aucun compte valorisé", "Les comptes apparaîtront après la saisie de valeurs.");
    }

    private VBox tile(AccountRow row) {
        Label icon = Ui.label(row.initials(), "dashboard-account-icon");
        Label name = Ui.label(row.name(), "dashboard-account-name");
        Label value = Ui.style(Ui.label(row.value(), "dashboard-account-value"), "monospace");
        Label detail = Ui.help("Valorisation actuelle");
        detail.getStyleClass().add("dashboard-account-detail");

        HBox top = new HBox(icon);
        top.setAlignment(Pos.CENTER_LEFT);
        VBox tile = Ui.style(new VBox(10, top, name, value, detail), "dashboard-account-tile");
        tile.setMinWidth(210);
        tile.setPrefWidth(260);
        HBox.setHgrow(tile, Priority.ALWAYS);
        return tile;
    }

    public static final class AccountRow {
        private final String name;
        private final String value;
        private final BigDecimal sortKey;

        private AccountRow(String name, String value, BigDecimal sortKey) {
            this.name = name;
            this.value = value;
            this.sortKey = sortKey == null ? BigDecimal.ZERO : sortKey;
        }

        static AccountRow from(AccountValueDto account) {
            String name = account.accountName() == null || account.accountName().isBlank() ? "Compte sans nom" : account.accountName();
            MoneyDto value = account.value();
            BigDecimal amount = value == null ? BigDecimal.ZERO : value.amount();
            return new AccountRow(name, DesktopFormatters.money(value), amount);
        }

        BigDecimal sortKey() {
            return sortKey;
        }

        String initials() {
            String trimmed = name == null ? "?" : name.strip();
            if (trimmed.isEmpty()) {
                return "?";
            }
            StringBuilder result = new StringBuilder();
            for (String part : trimmed.split("\\s+")) {
                if (!part.isBlank()) {
                    result.append(Character.toUpperCase(part.charAt(0)));
                }
                if (result.length() == 2) {
                    break;
                }
            }
            return result.isEmpty() ? "?" : result.toString();
        }

        public String name() {
            return name;
        }

        public String value() {
            return value;
        }
    }
}
