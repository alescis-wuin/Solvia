package fr.seynax.solvia.desktop.ui;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
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
import fr.seynax.solvia.desktop.api.ApiDtos.AccountSnapshotDto;
import fr.seynax.solvia.desktop.api.ApiDtos.AccountUpdateDto;
import fr.seynax.solvia.desktop.api.BackendConnectionState;
import fr.seynax.solvia.desktop.api.BackendStatusSnapshot;
import fr.seynax.solvia.desktop.api.SolviaApiClient;
import fr.seynax.solvia.desktop.ui.InputValidation.ValidationResult;

public final class AccountsView extends VBox {

    private final SolviaApiClient apiClient;
    private final StateMessage state = new StateMessage();
    private final EmptyState empty = new EmptyState();
    private final TableView<AccountRow> table = new TableView<>();
    private final TableView<AccountSnapshotRow> snapshotTable = new TableView<>();
    private final Label accountDetail = Ui.help("Sélectionne un compte pour afficher son détail et son dernier solde connu.");

    private final TextField filter = Ui.tooltip(new TextField(), "Filtre les comptes par nom, type, enveloppe, devise ou état.");
    private final TextField name = Ui.tooltip(new TextField(), "Nom lisible du compte, par exemple Compte courant ou PEA.");
    private final ComboBox<String> type = Ui.tooltip(new ComboBox<>(), "Catégorie technique du compte.");
    private final ComboBox<String> envelopeType = Ui.tooltip(new ComboBox<>(), "Enveloppe patrimoniale utilisée pour l'allocation.");
    private final TextField currency = Ui.tooltip(new TextField("EUR"), "Devise ISO sur trois lettres, par exemple EUR ou USD.");

    private final Button create = Ui.tooltip(new Button("Créer le compte"), "Crée le compte dans le backend local.");
    private final Button update = Ui.tooltip(new Button("Mettre à jour"), "Modifie le compte sélectionné.");
    private final Button deactivate = Ui.tooltip(new Button("Désactiver"), "Désactive le compte sélectionné sans supprimer son historique.");
    private final Button clearSelection = Ui.tooltip(new Button("Nouveau"), "Vide la sélection pour créer un nouveau compte.");

    private final AtomicLong refreshGeneration = new AtomicLong();
    private final AtomicLong snapshotGeneration = new AtomicLong();
    private volatile boolean backendReady;
    private AccountDto selectedAccount;
    private List<AccountDto> currentAccounts = List.of();
    private Runnable onAccountsChanged = () -> { };

    public AccountsView(SolviaApiClient apiClient) {
        this.apiClient = apiClient;
        getStyleClass().add("content-view");
        setSpacing(18);
        setPadding(new Insets(20));
        configureCombos();
        configureTables();
        filter.textProperty().addListener((observable, previous, value) -> renderAccounts());
        getChildren().addAll(content(), state, empty);
        setInputsDisabled(true);
        state.show("Vérification", "Vérification du backend local...", "state-info");
    }

    public void setOnAccountsChanged(Runnable onAccountsChanged) {
        this.onAccountsChanged = onAccountsChanged == null ? () -> { } : onAccountsChanged;
    }

    public void backendStatusChanged(BackendStatusSnapshot snapshot) {
        if (snapshot.state() == BackendConnectionState.CHECKING) {
            if (!backendReady && currentAccounts.isEmpty()) {
                setInputsDisabled(true);
                state.show("Vérification", "Vérification du backend local...", "state-info");
            }
            return;
        }
        backendReady = snapshot.canLoadData();
        if (!backendReady) {
            refreshGeneration.incrementAndGet();
            snapshotGeneration.incrementAndGet();
            setInputsDisabled(true);
            clearData();
            empty.show("Backend indisponible", "Les comptes seront affichés quand le backend local sera connecté.");
            state.show("Backend non prêt", snapshot.message(), "state-warning");
            return;
        }
        setInputsDisabled(false);
        refresh();
    }

    public void refresh() {
        if (!backendReady) {
            state.show("Backend non prêt", "Backend local non prêt.", "state-warning");
            return;
        }
        long generation = refreshGeneration.incrementAndGet();
        UUID previousSelection = selectedAccount == null ? null : selectedAccount.id();
        FocusSnapshot focus = FocusSnapshot.capture(this);
        empty.hide();
        state.show("Chargement", "Chargement des comptes...", "state-info");
        apiClient.accounts().whenComplete((accounts, error) -> Platform.runLater(() -> {
            if (generation != refreshGeneration.get()) {
                focus.restoreLater();
                return;
            }
            if (error != null) {
                state.show("Erreur", DesktopFormatters.errorMessage(error), "state-error");
                focus.restoreLater();
                return;
            }
            currentAccounts = List.copyOf(accounts);
            renderAccounts();
            reselect(previousSelection);
            if (accounts.isEmpty()) {
                empty.show("Aucun compte", "Crée un premier compte pour commencer la saisie patrimoniale.");
                state.show("Aucun compte", "Aucun compte n'est encore enregistré.", "state-warning");
            } else {
                empty.hide();
                state.show("Comptes chargés", DesktopFormatters.count(accounts.size(), "compte", "comptes"), "state-success");
            }
            focus.restoreLater();
        }));
    }

    private HBox content() {
        VBox left = new VBox(18, form(), tableSection());
        VBox right = new VBox(18, detailSection(), snapshotSection());
        HBox row = new HBox(18, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(snapshotTable, Priority.ALWAYS);
        return row;
    }

    private SectionCard form() {
        create.setOnAction(event -> createAccount());
        update.setOnAction(event -> updateAccount());
        deactivate.setOnAction(event -> deactivateAccount());
        clearSelection.setOnAction(event -> clearSelectedAccount());

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.add(Ui.fieldLabel("Nom", name), 0, 0);
        grid.add(name, 1, 0);
        grid.add(Ui.fieldLabel("Devise", currency), 2, 0);
        grid.add(currency, 3, 0);
        grid.add(Ui.fieldLabel("Type", type), 0, 1);
        grid.add(type, 1, 1);
        grid.add(Ui.fieldLabel("Enveloppe", envelopeType), 2, 1);
        grid.add(envelopeType, 3, 1);
        grid.add(new HBox(8, create, update, deactivate, clearSelection), 1, 2, 3, 1);

        return new SectionCard("Compte", "Crée, modifie ou désactive un compte sans supprimer son historique.", grid);
    }

    private SectionCard tableSection() {
        VBox content = new VBox(10, filter, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return new SectionCard("Comptes", "Sélectionne une ligne ou utilise les actions inline.", content);
    }

    private SectionCard detailSection() {
        return new SectionCard("Détail compte", accountDetail);
    }

    private SectionCard snapshotSection() {
        return new SectionCard("Historique des soldes", "Snapshots connus pour le compte sélectionné.", snapshotTable);
    }

    private void configureCombos() {
        type.getItems().setAll("CHECKING", "SAVINGS", "INVESTMENT", "PEA", "CTO", "CRYPTO_EXCHANGE", "CRYPTO_WALLET", "CASHBACK", "OTHER");
        type.setValue("CHECKING");
        envelopeType.getItems().setAll("CURRENT_ACCOUNT", "REGULATED_SAVINGS", "PEA", "CTO", "CRYPTO", "PRIVATE_ASSET", "CASHBACK", "OTHER");
        envelopeType.setValue("CURRENT_ACCOUNT");
    }

    private void configureTables() {
        table.setAccessibleText("Tableau des comptes Solvia.");
        TableColumn<AccountRow, String> nameColumn = new TableColumn<>("Nom");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<AccountRow, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        TableColumn<AccountRow, String> envelopeColumn = new TableColumn<>("Enveloppe");
        envelopeColumn.setCellValueFactory(new PropertyValueFactory<>("envelopeType"));
        TableColumn<AccountRow, String> currencyColumn = new TableColumn<>("Devise");
        currencyColumn.setCellValueFactory(new PropertyValueFactory<>("currencyCode"));
        TableColumn<AccountRow, String> activeColumn = new TableColumn<>("État");
        activeColumn.setCellValueFactory(new PropertyValueFactory<>("active"));
        TableColumn<AccountRow, HBox> actionsColumn = new TableColumn<>("Actions");
        actionsColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(accountActions(data.getValue())));
        table.getColumns().setAll(nameColumn, typeColumn, envelopeColumn, currencyColumn, activeColumn, actionsColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPrefHeight(360);
        table.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) -> selectAccount(selected == null ? null : selected.account()));

        TableColumn<AccountSnapshotRow, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        TableColumn<AccountSnapshotRow, String> balanceColumn = new TableColumn<>("Solde");
        balanceColumn.setCellValueFactory(new PropertyValueFactory<>("balance"));
        TableColumn<AccountSnapshotRow, String> confidenceColumn = new TableColumn<>("Confiance");
        confidenceColumn.setCellValueFactory(new PropertyValueFactory<>("confidence"));
        TableColumn<AccountSnapshotRow, String> recordedColumn = new TableColumn<>("Saisie");
        recordedColumn.setCellValueFactory(new PropertyValueFactory<>("recordedAt"));
        snapshotTable.getColumns().setAll(dateColumn, balanceColumn, confidenceColumn, recordedColumn);
        snapshotTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        snapshotTable.setPrefHeight(260);
    }

    private HBox accountActions(AccountRow row) {
        Button open = Ui.tooltip(new Button("Ouvrir"), "Sélectionne ce compte.");
        open.setOnAction(event -> table.getSelectionModel().select(row));
        Button disable = Ui.tooltip(new Button("Désactiver"), "Désactive ce compte après confirmation.");
        disable.setDisable(!row.account().active());
        disable.setOnAction(event -> {
            table.getSelectionModel().select(row);
            deactivateAccount();
        });
        return new HBox(6, open, disable);
    }

    private void renderAccounts() {
        String query = normalized(filter.getText());
        table.getItems().setAll(currentAccounts.stream()
                .filter(account -> query.isBlank() || normalized(account.name()).contains(query)
                        || normalized(account.type()).contains(query)
                        || normalized(account.envelopeType()).contains(query)
                        || normalized(account.currencyCode()).contains(query)
                        || normalized(status(account.active())).contains(query))
                .map(AccountRow::from)
                .toList());
    }

    private void createAccount() {
        if (!backendReady) {
            state.show("Backend non prêt", "Backend local non prêt.", "state-warning");
            return;
        }
        AccountUpdateDto updateRequest = readAccountUpdateRequest(true);
        if (updateRequest == null) {
            return;
        }
        AccountCreateDto request = new AccountCreateDto(updateRequest.name(), updateRequest.type(), updateRequest.envelopeType(), updateRequest.currencyCode());
        create.setDisable(true);
        state.show("Création", "Création du compte...", "state-info");
        apiClient.createAccount(request).whenComplete((account, error) -> Platform.runLater(() -> {
            create.setDisable(false);
            if (error != null) {
                state.show("Erreur", DesktopFormatters.errorMessage(error), "state-error");
                return;
            }
            selectedAccount = account;
            state.show("Compte créé", "Compte créé: " + account.name(), "state-success");
            refresh();
            onAccountsChanged.run();
        }));
    }

    private void updateAccount() {
        if (selectedAccount == null) {
            state.show("Sélection requise", "Sélectionner un compte avant modification.", "state-warning");
            return;
        }
        AccountUpdateDto request = readAccountUpdateRequest(selectedAccount.active());
        if (request == null) {
            return;
        }
        update.setDisable(true);
        state.show("Mise à jour", "Mise à jour du compte...", "state-info");
        apiClient.updateAccount(selectedAccount.id(), request).whenComplete((account, error) -> Platform.runLater(() -> {
            update.setDisable(false);
            if (error != null) {
                state.show("Erreur", DesktopFormatters.errorMessage(error), "state-error");
                return;
            }
            selectedAccount = account;
            state.show("Compte mis à jour", account.name() + " a été modifié.", "state-success");
            refresh();
            onAccountsChanged.run();
        }));
    }

    private void deactivateAccount() {
        if (selectedAccount == null) {
            state.show("Sélection requise", "Sélectionner un compte avant désactivation.", "state-warning");
            return;
        }
        if (!Ui.confirm(
                "Désactiver le compte",
                "Désactiver « " + selectedAccount.name() + " » ?",
                "Le compte ne sera pas supprimé. Son historique restera disponible et pourra encore être utilisé pour les anciens calculs."
        )) {
            return;
        }
        deactivate.setDisable(true);
        state.show("Désactivation", "Désactivation du compte...", "state-info");
        apiClient.deactivateAccount(selectedAccount.id()).whenComplete((ignored, error) -> Platform.runLater(() -> {
            deactivate.setDisable(false);
            if (error != null) {
                state.show("Erreur", DesktopFormatters.errorMessage(error), "state-error");
                return;
            }
            clearSelectedAccount();
            state.show("Compte désactivé", "Le compte reste conservé dans l'historique.", "state-success");
            refresh();
            onAccountsChanged.run();
        }));
    }

    private AccountUpdateDto readAccountUpdateRequest(boolean active) {
        String accountName = name.getText() == null ? "" : name.getText().strip();
        if (accountName.isBlank()) {
            state.show("Nom requis", "Le nom du compte est obligatoire.", "state-warning");
            return null;
        }
        ValidationResult<String> currencyResult = InputValidation.currencyCode(currency.getText(), "Devise");
        if (!currencyResult.valid()) {
            state.show("Devise invalide", currencyResult.message(), "state-warning");
            return null;
        }
        currency.setText(currencyResult.value());
        return new AccountUpdateDto(accountName, type.getValue(), envelopeType.getValue(), currencyResult.value(), active);
    }

    private void selectAccount(AccountDto account) {
        selectedAccount = account;
        if (account == null) {
            clearAccountForm();
            snapshotGeneration.incrementAndGet();
            snapshotTable.getItems().clear();
            accountDetail.setText("Sélectionne un compte pour afficher son détail et son dernier solde connu.");
            setInputsDisabled(!backendReady);
            return;
        }
        name.setText(account.name());
        type.setValue(account.type());
        envelopeType.setValue(account.envelopeType());
        currency.setText(account.currencyCode());
        accountDetail.setText("Chargement de l'historique du compte...");
        setInputsDisabled(!backendReady);
        loadSnapshotHistory(account.id());
    }

    private void loadSnapshotHistory(UUID accountId) {
        long generation = snapshotGeneration.incrementAndGet();
        FocusSnapshot focus = FocusSnapshot.capture(this);
        apiClient.accountSnapshots(accountId).whenComplete((snapshots, error) -> Platform.runLater(() -> {
            if (generation != snapshotGeneration.get() || selectedAccount == null || !selectedAccount.id().equals(accountId)) {
                focus.restoreLater();
                return;
            }
            if (error != null) {
                state.show("Erreur", DesktopFormatters.errorMessage(error), "state-error");
                focus.restoreLater();
                return;
            }
            List<AccountSnapshotDto> sorted = snapshots.stream().sorted(snapshotComparator()).toList();
            snapshotTable.getItems().setAll(sorted.stream().map(AccountSnapshotRow::from).toList());
            renderAccountDetail(selectedAccount, sorted);
            focus.restoreLater();
        }));
    }

    private void renderAccountDetail(AccountDto account, List<AccountSnapshotDto> snapshots) {
        AccountSnapshotDto latest = snapshots.isEmpty() ? null : snapshots.get(0);
        accountDetail.setText("Nom: " + account.name()
                + "\nType: " + accountType(account.type())
                + "\nEnveloppe: " + envelope(account.envelopeType())
                + "\nDevise: " + account.currencyCode()
                + "\nÉtat: " + status(account.active())
                + "\nDernier solde connu: " + (latest == null ? "—" : DesktopFormatters.money(latest.balance()))
                + "\nDate du dernier solde: " + (latest == null ? "—" : DesktopFormatters.date(latest.valueDate()))
                + "\nSnapshots: " + DesktopFormatters.count(snapshots.size(), "snapshot", "snapshots")
                + "\nCréation: " + DesktopFormatters.time(account.createdAt()));
    }

    private Comparator<AccountSnapshotDto> snapshotComparator() {
        return Comparator.comparing(AccountSnapshotDto::valueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(AccountSnapshotDto::recordedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed();
    }

    private void clearSelectedAccount() {
        selectedAccount = null;
        snapshotGeneration.incrementAndGet();
        table.getSelectionModel().clearSelection();
        clearAccountForm();
        snapshotTable.getItems().clear();
        accountDetail.setText("Sélectionne un compte pour afficher son détail et son dernier solde connu.");
        setInputsDisabled(!backendReady);
    }

    private void clearAccountForm() {
        name.clear();
        type.setValue("CHECKING");
        envelopeType.setValue("CURRENT_ACCOUNT");
        currency.setText("EUR");
    }

    private void reselect(UUID accountId) {
        if (accountId == null) {
            return;
        }
        table.getItems().stream()
                .filter(row -> row.account().id().equals(accountId))
                .findFirst()
                .ifPresent(row -> table.getSelectionModel().select(row));
    }

    private void setInputsDisabled(boolean disabled) {
        filter.setDisable(disabled);
        name.setDisable(disabled);
        type.setDisable(disabled);
        envelopeType.setDisable(disabled);
        currency.setDisable(disabled);
        create.setDisable(disabled);
        update.setDisable(disabled || selectedAccount == null);
        deactivate.setDisable(disabled || selectedAccount == null || !selectedAccount.active());
        clearSelection.setDisable(disabled);
    }

    private void clearData() {
        selectedAccount = null;
        currentAccounts = List.of();
        table.getItems().clear();
        snapshotTable.getItems().clear();
        clearAccountForm();
        accountDetail.setText("Sélectionne un compte pour afficher son détail et son dernier solde connu.");
    }

    private String normalized(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }

    private String status(boolean active) {
        return active ? "Actif" : "Inactif";
    }

    private String accountType(String value) {
        if (value == null || value.isBlank()) {
            return "Non classé";
        }
        return switch (value) {
            case "CHECKING" -> "Compte courant";
            case "SAVINGS" -> "Épargne";
            case "INVESTMENT" -> "Investissement";
            case "PEA" -> "PEA";
            case "CTO" -> "CTO";
            case "CRYPTO_EXCHANGE" -> "Plateforme crypto";
            case "CRYPTO_WALLET" -> "Wallet crypto";
            case "CASHBACK" -> "Cashback";
            case "OTHER" -> "Autre";
            default -> value;
        };
    }

    private String envelope(String value) {
        if (value == null || value.isBlank()) {
            return "Non classée";
        }
        return switch (value) {
            case "CURRENT_ACCOUNT" -> "Compte courant";
            case "REGULATED_SAVINGS" -> "Épargne réglementée";
            case "PEA" -> "PEA";
            case "CTO" -> "CTO";
            case "CRYPTO" -> "Crypto";
            case "PRIVATE_ASSET" -> "Actif privé";
            case "CASHBACK" -> "Cashback";
            case "OTHER" -> "Autre";
            default -> value;
        };
    }

    public static final class AccountRow {
        private final AccountDto account;
        private final String name;
        private final String type;
        private final String envelopeType;
        private final String currencyCode;
        private final String active;

        private AccountRow(AccountDto account, String name, String type, String envelopeType, String currencyCode, String active) {
            this.account = account;
            this.name = name;
            this.type = type;
            this.envelopeType = envelopeType;
            this.currencyCode = currencyCode;
            this.active = active;
        }

        static AccountRow from(AccountDto account) {
            return new AccountRow(account, account.name(), account.type(), account.envelopeType(), account.currencyCode(), account.active() ? "Actif" : "Inactif");
        }

        AccountDto account() {
            return account;
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

    public static final class AccountSnapshotRow {
        private final String date;
        private final String balance;
        private final String confidence;
        private final String recordedAt;

        private AccountSnapshotRow(String date, String balance, String confidence, String recordedAt) {
            this.date = date;
            this.balance = balance;
            this.confidence = confidence;
            this.recordedAt = recordedAt;
        }

        static AccountSnapshotRow from(AccountSnapshotDto snapshot) {
            return new AccountSnapshotRow(
                    DesktopFormatters.date(snapshot.valueDate()),
                    DesktopFormatters.money(snapshot.balance()),
                    snapshot.confidence(),
                    DesktopFormatters.time(snapshot.recordedAt())
            );
        }

        public String getDate() {
            return date;
        }

        public String getBalance() {
            return balance;
        }

        public String getConfidence() {
            return confidence;
        }

        public String getRecordedAt() {
            return recordedAt;
        }
    }
}
