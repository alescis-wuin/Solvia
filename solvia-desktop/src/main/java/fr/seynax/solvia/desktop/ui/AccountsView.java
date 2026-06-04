package fr.seynax.solvia.desktop.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import fr.seynax.solvia.desktop.api.ApiDtos.AccountCreateDto;
import fr.seynax.solvia.desktop.api.ApiDtos.AccountDto;
import fr.seynax.solvia.desktop.api.BackendConnectionState;
import fr.seynax.solvia.desktop.api.BackendStatusSnapshot;
import fr.seynax.solvia.desktop.api.SolviaApiClient;

public final class AccountsView extends VBox {

    private final SolviaApiClient apiClient;
    private final StateMessage state = new StateMessage();
    private final TableView<AccountRow> table = new TableView<>();
    private final TextField name = Ui.tooltip(new TextField(), "Nom lisible du compte, par exemple Compte courant ou PEA.");
    private final ComboBox<String> type = Ui.tooltip(new ComboBox<>(), "Categorie technique du compte.");
    private final ComboBox<String> envelopeType = Ui.tooltip(new ComboBox<>(), "Enveloppe patrimoniale utilisee pour l'allocation.");
    private final TextField currency = Ui.tooltip(new TextField("EUR"), "Devise ISO sur trois lettres, par exemple EUR ou USD.");
    private final Button create = Ui.tooltip(new Button("Creer le compte"), "Cree le compte dans le backend local.");
    private volatile boolean backendReady;

    public AccountsView(SolviaApiClient apiClient) {
        this.apiClient = apiClient;
        getStyleClass().add("content-view");
        setSpacing(18);
        setPadding(new Insets(20));
        type.getItems().setAll("CHECKING", "SAVINGS", "INVESTMENT", "PEA", "CTO", "CRYPTO_EXCHANGE", "CRYPTO_WALLET", "CASHBACK", "OTHER");
        type.setValue("CHECKING");
        envelopeType.getItems().setAll("CURRENT_ACCOUNT", "REGULATED_SAVINGS", "PEA", "CTO", "CRYPTO", "PRIVATE_ASSET", "CASHBACK", "OTHER");
        envelopeType.setValue("CURRENT_ACCOUNT");
        getChildren().addAll(form(), state, tableSection());
        VBox.setVgrow(table, Priority.ALWAYS);
        setInputsDisabled(true);
        state.show("Verification", "Verification du backend local...", "state-info");
    }

    public void backendStatusChanged(BackendStatusSnapshot snapshot) {
        backendReady = snapshot.canLoadData();
        if (snapshot.state() == BackendConnectionState.CHECKING) {
            setInputsDisabled(true);
            state.show("Verification", "Verification du backend local...", "state-info");
            return;
        }
        if (!backendReady) {
            setInputsDisabled(true);
            table.getItems().clear();
            state.show("Backend non pret", snapshot.message(), "state-warning");
            return;
        }
        setInputsDisabled(false);
        refresh();
    }

    public void refresh() {
        if (!backendReady) {
            state.show("Backend non pret", "Backend local non pret.", "state-warning");
            return;
        }
        state.show("Chargement", "Chargement des comptes...", "state-info");
        apiClient.accounts().whenComplete((accounts, error) -> Platform.runLater(() -> {
            if (error != null) {
                state.show("Erreur", DesktopFormatters.errorMessage(error), "state-error");
                return;
            }
            table.getItems().setAll(accounts.stream().map(AccountRow::from).toList());
            state.show(
                    accounts.isEmpty() ? "Aucun compte" : "Comptes charges",
                    accounts.isEmpty() ? "Cree un premier compte pour commencer la saisie patrimoniale." : accounts.size() + " compte(s)",
                    accounts.isEmpty() ? "state-warning" : "state-success"
            );
        }));
    }

    private SectionCard form() {
        create.setOnAction(event -> createAccount());
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.add(Ui.fieldLabel("Nom", name), 0, 0);
        grid.add(name, 1, 0);
        grid.add(Ui.fieldLabel("Type", type), 0, 1);
        grid.add(type, 1, 1);
        grid.add(Ui.fieldLabel("Enveloppe", envelopeType), 2, 1);
        grid.add(envelopeType, 3, 1);
        grid.add(Ui.fieldLabel("Devise", currency), 2, 0);
        grid.add(currency, 3, 0);
        grid.add(new HBox(8, create), 1, 2, 3, 1);
        return new SectionCard("Nouveau compte", "Cree les comptes courants, livrets, enveloppes d'investissement ou wallets suivis par Solvia.", grid);
    }

    private SectionCard tableSection() {
        return new SectionCard("Comptes", "Liste des comptes enregistres dans le backend local.", table());
    }

    private TableView<AccountRow> table() {
        table.setAccessibleText("Tableau des comptes Solvia.");
        TableColumn<AccountRow, String> nameColumn = new TableColumn<>("Nom");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<AccountRow, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        TableColumn<AccountRow, String> envelopeColumn = new TableColumn<>("Enveloppe");
        envelopeColumn.setCellValueFactory(new PropertyValueFactory<>("envelopeType"));
        TableColumn<AccountRow, String> currencyColumn = new TableColumn<>("Devise");
        currencyColumn.setCellValueFactory(new PropertyValueFactory<>("currencyCode"));
        TableColumn<AccountRow, String> activeColumn = new TableColumn<>("Actif");
        activeColumn.setCellValueFactory(new PropertyValueFactory<>("active"));
        table.getColumns().setAll(nameColumn, typeColumn, envelopeColumn, currencyColumn, activeColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        return table;
    }

    private void createAccount() {
        if (!backendReady) {
            state.show("Backend non pret", "Backend local non pret.", "state-warning");
            return;
        }
        AccountCreateDto request = new AccountCreateDto(name.getText(), type.getValue(), envelopeType.getValue(), currency.getText());
        create.setDisable(true);
        state.show("Creation", "Creation du compte...", "state-info");
        apiClient.createAccount(request).whenComplete((account, error) -> Platform.runLater(() -> {
            create.setDisable(false);
            if (error != null) {
                state.show("Erreur", DesktopFormatters.errorMessage(error), "state-error");
                return;
            }
            name.clear();
            state.show("Compte cree", "Compte cree: " + account.name(), "state-success");
            refresh();
        }));
    }

    private void setInputsDisabled(boolean disabled) {
        name.setDisable(disabled);
        type.setDisable(disabled);
        envelopeType.setDisable(disabled);
        currency.setDisable(disabled);
        create.setDisable(disabled);
    }

    public static final class AccountRow {
        private final String name;
        private final String type;
        private final String envelopeType;
        private final String currencyCode;
        private final String active;

        private AccountRow(String name, String type, String envelopeType, String currencyCode, String active) {
            this.name = name;
            this.type = type;
            this.envelopeType = envelopeType;
            this.currencyCode = currencyCode;
            this.active = active;
        }

        static AccountRow from(AccountDto account) {
            return new AccountRow(account.name(), account.type(), account.envelopeType(), account.currencyCode(), account.active() ? "Oui" : "Non");
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public String getEnvelopeType() {
            return envelopeType;
        }

        public String getCurrencyCode() {
            return currencyCode;
        }

        public String getActive() {
            return active;
        }
    }
}
