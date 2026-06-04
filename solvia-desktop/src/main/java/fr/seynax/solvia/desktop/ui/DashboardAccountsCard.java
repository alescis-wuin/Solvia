package fr.seynax.solvia.desktop.ui;

import java.util.Comparator;
import java.util.List;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import fr.seynax.solvia.desktop.api.ApiDtos.AccountValueDto;

public final class DashboardAccountsCard extends SectionCard {

    private final TableView<AccountRow> table = new TableView<>();
    private final EmptyState empty = new EmptyState();

    public DashboardAccountsCard() {
        super("Comptes valorisés", "Vue rapide des comptes qui contribuent au patrimoine affiché.");
        table.setAccessibleText("Tableau des comptes valorisés dans le patrimoine.");
        TableColumn<AccountRow, String> name = new TableColumn<>("Compte");
        name.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<AccountRow, String> value = new TableColumn<>("Valeur");
        value.setCellValueFactory(new PropertyValueFactory<>("value"));
        table.getColumns().setAll(name, value);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPrefHeight(180);
        getChildren().addAll(empty, table);
        empty.show("Aucun compte valorisé", "Les comptes apparaîtront après la saisie de valeurs.");
    }

    public void update(List<AccountValueDto> accounts) {
        List<AccountRow> rows = accounts == null ? List.of() : accounts.stream()
                .map(AccountRow::from)
                .sorted(Comparator.comparing(AccountRow::sortKey).reversed())
                .toList();
        table.getItems().setAll(rows);
        if (rows.isEmpty()) {
            empty.show("Aucun compte valorisé", "Aucun compte n'a de valeur disponible sur cette période.");
        } else {
            empty.hide();
        }
    }

    public void clear() {
        table.getItems().clear();
        empty.show("Aucun compte valorisé", "Les comptes apparaîtront après la saisie de valeurs.");
    }

    public static final class AccountRow {
        private final String name;
        private final String value;
        private final java.math.BigDecimal sortKey;

        private AccountRow(String name, String value, java.math.BigDecimal sortKey) {
            this.name = name;
            this.value = value;
            this.sortKey = sortKey == null ? java.math.BigDecimal.ZERO : sortKey;
        }

        static AccountRow from(AccountValueDto account) {
            String name = account.accountName() == null || account.accountName().isBlank() ? "Compte sans nom" : account.accountName();
            return new AccountRow(name, DesktopFormatters.money(account.value()), account.value().amount());
        }

        java.math.BigDecimal sortKey() {
            return sortKey;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }
}
