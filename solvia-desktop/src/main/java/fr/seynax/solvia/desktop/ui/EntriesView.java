package fr.seynax.solvia.desktop.ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import fr.seynax.solvia.desktop.api.ApiDtos.AccountDto;
import fr.seynax.solvia.desktop.api.ApiDtos.AccountSnapshotCreateDto;
import fr.seynax.solvia.desktop.api.ApiDtos.AccountSnapshotDto;
import fr.seynax.solvia.desktop.api.ApiDtos.CashFlowCreateDto;
import fr.seynax.solvia.desktop.api.ApiDtos.CashFlowDto;
import fr.seynax.solvia.desktop.api.ApiDtos.MoneyDto;
import fr.seynax.solvia.desktop.api.BackendConnectionState;
import fr.seynax.solvia.desktop.api.BackendStatusSnapshot;
import fr.seynax.solvia.desktop.api.SolviaApiClient;
import fr.seynax.solvia.desktop.ui.InputValidation.ValidationResult;

public final class EntriesView extends VBox {

    private final SolviaApiClient apiClient;
    private final StateMessage state = new StateMessage();
    private final EmptyState empty = new EmptyState();

    private final ComboBox<AccountDto> snapshotAccount = Ui.tooltip(new ComboBox<>(), "Compte concerné par la valeur observée.");
    private final DatePicker snapshotDate = Ui.tooltip(new DatePicker(LocalDate.now()), "Date de valorisation observée.");
    private final TextField snapshotAmount = Ui.tooltip(new TextField(), "Solde total observé pour le compte. Un solde négatif est accepté pour un découvert.");
    private final TextField snapshotCurrency = Ui.tooltip(new TextField("EUR"), "Devise ISO du montant observé.");
    private final Button saveSnapshot = Ui.tooltip(new Button("Enregistrer le snapshot"), "Ajoute une valeur observée pour le compte sélectionné.");

    private final ComboBox<AccountDto> flowAccount = Ui.tooltip(new ComboBox<>(), "Compte concerné par le flux financier.");
    private final ComboBox<String> flowType = Ui.tooltip(new ComboBox<>(), "Nature du flux financier. Le type détermine le sens du flux.");
    private final DatePicker flowDate = Ui.tooltip(new DatePicker(LocalDate.now()), "Date de valeur du flux financier.");
    private final TextField flowAmount = Ui.tooltip(new TextField(), "Montant strictement positif. Le type détermine s'il s'agit d'une entrée ou d'une sortie.");
    private final TextField flowCurrency = Ui.tooltip(new TextField("EUR"), "Devise ISO du flux financier.");
    private final TextField flowLabel = Ui.tooltip(new TextField(), "Libellé optionnel pour retrouver le flux.");
    private final Button saveFlow = Ui.tooltip(new Button("Enregistrer le flux"), "Ajoute un flux financier important.");

    private final TextField snapshotFilter = Ui.tooltip(new TextField(), "Filtre les snapshots affichés.");
    private final TextField flowFilter = Ui.tooltip(new TextField(), "Filtre les flux affichés.");
    private final TableView<SnapshotEntryRow> snapshotHistory = new TableView<>();
    private final TableView<CashFlowRow> cashFlowHistory = new TableView<>();

    private volatile boolean backendReady;
    private List<AccountDto> currentAccounts = List.of();
    private List<AccountSnapshotDto> currentSnapshotHistory = List.of();
    private List<CashFlowDto> currentCashFlows = List.of();
    private boolean snapshotCorrectionMode;
    private UUID correctingSnapshotId;
    private Runnable onPortfolioDataChanged = () -> { };

    public EntriesView(SolviaApiClient apiClient) {
        this.apiClient = apiClient;
        getStyleClass().add("content-view");
        setSpacing(18);
        setPadding(new Insets(20));
        configureCombos();
        configureTables();
        snapshotFilter.textProperty().addListener((observable, previous, value) -> renderSnapshotHistory());
        flowFilter.textProperty().addListener((observable, previous, value) -> renderCashFlowHistory());
        getChildren().addAll(forms(), histories(), state, empty);
        setInputsDisabled(true);
        state.show("Vérification", "Vérification du backend local...", "state-info");
    }

    public void setOnPortfolioDataChanged(Runnable onPortfolioDataChanged) {
        this.onPortfolioDataChanged = onPortfolioDataChanged == null ? () -> { } : onPortfolioDataChanged;
    }

    public void backendStatusChanged(BackendStatusSnapshot snapshot) {
        backendReady = snapshot.canLoadData();
        if (snapshot.state() == BackendConnectionState.CHECKING) {
            setInputsDisabled(true);
            state.show("Vérification", "Vérification du backend local...", "state-info");
            return;
        }
        if (!backendReady) {
            setInputsDisabled(true);
            clearData();
            empty.show("Backend indisponible", "La saisie sera disponible quand le backend local sera connecté.");
            state.show("Backend non prêt", snapshot.message(), "state-warning");
            return;
        }
        setInputsDisabled(false);
        refreshAccounts();
    }

    public void refreshAccounts() {
        if (!backendReady) {
            state.show("Backend non prêt", "Backend local non prêt.", "state-warning");
            return;
        }
        UUID previousSnapshotAccount = snapshotAccount.getValue() == null ? null : snapshotAccount.getValue().id();
        UUID previousFlowAccount = flowAccount.getValue() == null ? null : flowAccount.getValue().id();
        empty.hide();
        state.show("Chargement", "Chargement des comptes...", "state-info");
        apiClient.accounts().whenComplete((accounts, error) -> Platform.runLater(() -> {
            if (error != null) {
                state.show("Erreur", DesktopFormatters.errorMessage(error), "state-error");
                return;
            }
            currentAccounts = List.copyOf(accounts);
            snapshotAccount.getItems().setAll(accounts);
            flowAccount.getItems().setAll(accounts);
            restoreAccountSelection(snapshotAccount, previousSnapshotAccount);
            restoreAccountSelection(flowAccount, previousFlowAccount);
            if (accounts.isEmpty()) {
                currentSnapshotHistory = List.of();
                currentCashFlows = List.of();
                renderSnapshotHistory();
                renderCashFlowHistory();
                empty.show("Aucun compte", "Crée un compte avant de saisir un snapshot ou un flux.");
                state.show("Aucun compte", "La saisie nécessite au moins un compte.", "state-warning");
            } else {
                empty.hide();
                state.show("Comptes chargés", DesktopFormatters.count(accounts.size(), "compte", "comptes"), "state-success");
                loadSnapshotHistory(snapshotAccount.getValue());
                loadCashFlowHistory(flowAccount.getValue());
            }
        }));
    }

    private HBox forms() {
        HBox row = new HBox(18, snapshotForm(), flowForm());
        HBox.setHgrow(row.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(row.getChildren().get(1), Priority.ALWAYS);
        return row;
    }

    private HBox histories() {
        HBox row = new HBox(18, snapshotHistorySection(), cashFlowHistorySection());
        HBox.setHgrow(row.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(row.getChildren().get(1), Priority.ALWAYS);
        VBox.setVgrow(snapshotHistory, Priority.ALWAYS);
        VBox.setVgrow(cashFlowHistory, Priority.ALWAYS);
        return row;
    }

    private SectionCard snapshotForm() {
        saveSnapshot.setOnAction(event -> saveSnapshot());
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.add(Ui.fieldLabel("Compte", snapshotAccount), 0, 0);
        grid.add(snapshotAccount, 1, 0);
        grid.add(Ui.fieldLabel("Date", snapshotDate), 2, 0);
        grid.add(snapshotDate, 3, 0);
        grid.add(Ui.fieldLabel("Montant", snapshotAmount), 0, 1);
        grid.add(snapshotAmount, 1, 1);
        grid.add(Ui.fieldLabel("Devise", snapshotCurrency), 2, 1);
        grid.add(snapshotCurrency, 3, 1);
        grid.add(saveSnapshot, 1, 2);
        return new SectionCard("Snapshot de compte", "Une correction crée un nouveau snapshot historisé, sans modifier l'ancien.", grid);
    }

    private SectionCard flowForm() {
        saveFlow.setOnAction(event -> saveFlow());
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.add(Ui.fieldLabel("Compte", flowAccount), 0, 0);
        grid.add(flowAccount, 1, 0);
        grid.add(Ui.fieldLabel("Type", flowType), 2, 0);
        grid.add(flowType, 3, 0);
        grid.add(Ui.fieldLabel("Date", flowDate), 0, 1);
        grid.add(flowDate, 1, 1);
        grid.add(Ui.fieldLabel("Montant", flowAmount), 2, 1);
        grid.add(flowAmount, 3, 1);
        grid.add(Ui.fieldLabel("Devise", flowCurrency), 0, 2);
        grid.add(flowCurrency, 1, 2);
        grid.add(Ui.fieldLabel("Libellé", flowLabel), 2, 2);
        grid.add(flowLabel, 3, 2);
        grid.add(saveFlow, 1, 3);
        return new SectionCard("Flux financier", "Les corrections de flux créent un flux compensatoire, sans modifier le flux source.", grid);
    }

    private SectionCard snapshotHistorySection() {
        VBox content = new VBox(10, snapshotFilter, snapshotHistory);
        return new SectionCard("Snapshots du compte", "Historique filtrable du compte sélectionné.", content);
    }

    private SectionCard cashFlowHistorySection() {
        VBox content = new VBox(10, flowFilter, cashFlowHistory);
        return new SectionCard("Flux du compte", "Historique filtrable du compte sélectionné.", content);
    }

    private void configureCombos() {
        flowType.getItems().setAll("DEPOSIT", "WITHDRAWAL", "TRANSFER_IN", "TRANSFER_OUT", "INTEREST", "DIVIDEND", "FEE", "TAX", "CASHBACK", "CORRECTION");
        flowType.setValue("DEPOSIT");
        snapshotAccount.valueProperty().addListener((observable, previous, account) -> {
            if (backendReady) {
                loadSnapshotHistory(account);
            }
        });
        flowAccount.valueProperty().addListener((observable, previous, account) -> {
            if (backendReady) {
                loadCashFlowHistory(account);
            }
        });
    }

    private void configureTables() {
        TableColumn<SnapshotEntryRow, String> snapshotDateColumn = new TableColumn<>("Date");
        snapshotDateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        TableColumn<SnapshotEntryRow, String> snapshotAmountColumn = new TableColumn<>("Solde");
        snapshotAmountColumn.setCellValueFactory(new PropertyValueFactory<>("balance"));
        TableColumn<SnapshotEntryRow, String> snapshotRecordedColumn = new TableColumn<>("Saisie");
        snapshotRecordedColumn.setCellValueFactory(new PropertyValueFactory<>("recordedAt"));
        TableColumn<SnapshotEntryRow, HBox> snapshotActionsColumn = new TableColumn<>("Actions");
        snapshotActionsColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(snapshotActions(data.getValue())));
        snapshotHistory.getColumns().setAll(snapshotDateColumn, snapshotAmountColumn, snapshotRecordedColumn, snapshotActionsColumn);
        snapshotHistory.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        snapshotHistory.setPrefHeight(260);

        TableColumn<CashFlowRow, String> flowDateColumn = new TableColumn<>("Date");
        flowDateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        TableColumn<CashFlowRow, String> flowTypeColumn = new TableColumn<>("Type");
        flowTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        TableColumn<CashFlowRow, String> flowAmountColumn = new TableColumn<>("Montant");
        flowAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        TableColumn<CashFlowRow, String> flowLabelColumn = new TableColumn<>("Libellé");
        flowLabelColumn.setCellValueFactory(new PropertyValueFactory<>("label"));
        TableColumn<CashFlowRow, HBox> flowActionsColumn = new TableColumn<>("Actions");
        flowActionsColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(cashFlowActions(data.getValue())));
        cashFlowHistory.getColumns().setAll(flowDateColumn, flowTypeColumn, flowAmountColumn, flowLabelColumn, flowActionsColumn);
        cashFlowHistory.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        cashFlowHistory.setPrefHeight(260);
    }

    private HBox snapshotActions(SnapshotEntryRow row) {
        Button correct = Ui.tooltip(new Button("Corriger"), "Prépare une correction append-only pour ce snapshot.");
        correct.setOnAction(event -> prepareSnapshotCorrection(row.snapshot()));
        return new HBox(6, correct);
    }

    private HBox cashFlowActions(CashFlowRow row) {
        Button correct = Ui.tooltip(new Button("Corriger"), "Prépare un flux compensatoire append-only.");
        correct.setOnAction(event -> prepareCashFlowCorrection(row.cashFlow()));
        return new HBox(6, correct);
    }

    private void loadSnapshotHistory(AccountDto account) {
        currentSnapshotHistory = List.of();
        renderSnapshotHistory();
        if (account == null) {
            return;
        }
        apiClient.accountSnapshots(account.id()).whenComplete((snapshots, error) -> Platform.runLater(() -> {
            if (error != null) {
                state.show("Erreur", DesktopFormatters.errorMessage(error), "state-error");
                return;
            }
            currentSnapshotHistory = snapshots.stream().sorted(snapshotComparator()).toList();
            renderSnapshotHistory();
        }));
    }

    private void loadCashFlowHistory(AccountDto account) {
        currentCashFlows = List.of();
        renderCashFlowHistory();
        if (account == null) {
            return;
        }
        apiClient.cashFlows(account.id()).whenComplete((cashFlows, error) -> Platform.runLater(() -> {
            if (error != null) {
                state.show("Erreur", DesktopFormatters.errorMessage(error), "state-error");
                return;
            }
            currentCashFlows = cashFlows.stream().sorted(cashFlowComparator()).toList();
            renderCashFlowHistory();
        }));
    }

    private void renderSnapshotHistory() {
        String query = normalized(snapshotFilter.getText());
        snapshotHistory.getItems().setAll(currentSnapshotHistory.stream()
                .filter(snapshot -> query.isBlank()
                        || DesktopFormatters.date(snapshot.valueDate()).toLowerCase(Locale.ROOT).contains(query)
                        || DesktopFormatters.money(snapshot.balance()).toLowerCase(Locale.ROOT).contains(query)
                        || normalized(snapshot.confidence()).contains(query)
                        || normalized(snapshot.note()).contains(query))
                .map(SnapshotEntryRow::from)
                .toList());
    }

    private void renderCashFlowHistory() {
        String query = normalized(flowFilter.getText());
        cashFlowHistory.getItems().setAll(currentCashFlows.stream()
                .filter(flow -> query.isBlank()
                        || DesktopFormatters.date(flow.valueDate()).toLowerCase(Locale.ROOT).contains(query)
                        || normalized(flow.type()).contains(query)
                        || normalized(cashFlowType(flow.type())).contains(query)
                        || DesktopFormatters.money(flow.amount()).toLowerCase(Locale.ROOT).contains(query)
                        || normalized(flow.label()).contains(query))
                .map(this::cashFlowRow)
                .toList());
    }

    private void saveSnapshot() {
        if (!backendReady) {
            state.show("Backend non prêt", "Backend local non prêt.", "state-warning");
            return;
        }
        AccountDto account = snapshotAccount.getValue();
        if (account == null) {
            state.show("Compte requis", "Sélectionner un compte avant la saisie.", "state-warning");
            return;
        }
        LocalDate date = snapshotDate.getValue();
        if (date == null) {
            state.show("Date requise", "Sélectionner une date de snapshot.", "state-warning");
            return;
        }
        boolean existingDate = hasSnapshotOnDate(date);
        if (existingDate && !snapshotCorrectionMode && !Ui.confirm(
                "Créer une correction",
                "Un snapshot existe déjà pour cette date.",
                "Solvia va créer un nouveau snapshot à la même date. Le plus récent par date de saisie sera utilisé par les calculs."
        )) {
            return;
        }
        ValidationResult<BigDecimal> amount = InputValidation.amount(snapshotAmount.getText(), "Montant du snapshot");
        if (!amount.valid()) {
            state.show("Montant invalide", amount.message(), "state-warning");
            return;
        }
        ValidationResult<String> currency = InputValidation.currencyCode(snapshotCurrency.getText(), "Devise du snapshot");
        if (!currency.valid()) {
            state.show("Devise invalide", currency.message(), "state-warning");
            return;
        }
        snapshotCurrency.setText(currency.value());
        String note = snapshotCorrectionMode
                ? "Correction append-only du snapshot " + correctingSnapshotId
                : existingDate ? "Correction append-only d'un snapshot existant" : null;
        AccountSnapshotCreateDto request = new AccountSnapshotCreateDto(account.id(), date, new MoneyDto(amount.value(), currency.value()), "OBSERVED", note);
        saveSnapshot.setDisable(true);
        state.show("Enregistrement", "Enregistrement du snapshot...", "state-info");
        apiClient.createAccountSnapshot(request).whenComplete((ignored, error) -> Platform.runLater(() -> {
            saveSnapshot.setDisable(false);
            if (error != null) {
                state.show("Erreur", DesktopFormatters.errorMessage(error), "state-error");
                return;
            }
            snapshotAmount.clear();
            snapshotCorrectionMode = false;
            correctingSnapshotId = null;
            saveSnapshot.setText("Enregistrer le snapshot");
            state.show("Snapshot enregistré", "La valeur observée a été ajoutée.", "state-success");
            loadSnapshotHistory(account);
            onPortfolioDataChanged.run();
        }));
    }

    private void saveFlow() {
        if (!backendReady) {
            state.show("Backend non prêt", "Backend local non prêt.", "state-warning");
            return;
        }
        AccountDto account = flowAccount.getValue();
        if (account == null) {
            state.show("Compte requis", "Sélectionner un compte avant la saisie.", "state-warning");
            return;
        }
        LocalDate date = flowDate.getValue();
        if (date == null) {
            state.show("Date requise", "Sélectionner une date de flux.", "state-warning");
            return;
        }
        ValidationResult<BigDecimal> amount = positiveAmount(flowAmount.getText(), "Montant du flux");
        if (!amount.valid()) {
            state.show("Montant invalide", amount.message(), "state-warning");
            return;
        }
        ValidationResult<String> currency = InputValidation.currencyCode(flowCurrency.getText(), "Devise du flux");
        if (!currency.valid()) {
            state.show("Devise invalide", currency.message(), "state-warning");
            return;
        }
        flowCurrency.setText(currency.value());
        String label = nullIfBlank(flowLabel.getText());
        if (hasDuplicateFlow(date, flowType.getValue(), amount.value(), currency.value(), label) && !Ui.confirm(
                "Créer un flux similaire",
                "Un flux identique existe déjà pour ce compte.",
                "Confirme la création uniquement s'il s'agit bien d'une écriture distincte."
        )) {
            return;
        }
        CashFlowCreateDto request = new CashFlowCreateDto(account.id(), flowType.getValue(), date, new MoneyDto(amount.value(), currency.value()), label);
        saveFlow.setDisable(true);
        state.show("Enregistrement", "Enregistrement du flux...", "state-info");
        apiClient.createCashFlow(request).whenComplete((ignored, error) -> Platform.runLater(() -> {
            saveFlow.setDisable(false);
            if (error != null) {
                state.show("Erreur", DesktopFormatters.errorMessage(error), "state-error");
                return;
            }
            flowAmount.clear();
            flowLabel.clear();
            state.show("Flux enregistré", "Le flux financier a été ajouté.", "state-success");
            loadCashFlowHistory(account);
            onPortfolioDataChanged.run();
        }));
    }

    private void prepareSnapshotCorrection(AccountSnapshotDto snapshot) {
        if (snapshot == null) {
            return;
        }
        if (!Ui.confirm("Corriger un snapshot", "Préparer une correction append-only ?", "Le snapshot source ne sera pas modifié. Un nouveau snapshot sera créé à la même date.")) {
            return;
        }
        selectAccount(snapshotAccount, snapshot.accountId());
        snapshotDate.setValue(snapshot.valueDate());
        snapshotAmount.setText(snapshot.balance() == null || snapshot.balance().amount() == null ? "" : snapshot.balance().amount().toPlainString());
        snapshotCurrency.setText(snapshot.balance() == null ? "EUR" : snapshot.balance().currencyCode());
        snapshotCorrectionMode = true;
        correctingSnapshotId = snapshot.id();
        saveSnapshot.setText("Enregistrer la correction");
        state.show("Correction préparée", "Modifie le montant puis enregistre une nouvelle valeur historisée.", "state-info");
    }

    private void prepareCashFlowCorrection(CashFlowDto flow) {
        if (flow == null) {
            return;
        }
        if (!Ui.confirm("Corriger un flux", "Préparer un flux compensatoire ?", "Le flux source ne sera pas modifié. Solvia va préremplir une écriture de correction.")) {
            return;
        }
        selectAccount(flowAccount, flow.accountId());
        flowDate.setValue(flow.valueDate());
        flowType.setValue(correctionType(flow.type()));
        flowAmount.setText(flow.amount() == null || flow.amount().amount() == null ? "" : flow.amount().amount().abs().toPlainString());
        flowCurrency.setText(flow.amount() == null ? "EUR" : flow.amount().currencyCode());
        flowLabel.setText("Correction du flux " + flow.id() + (flow.label() == null || flow.label().isBlank() ? "" : " - " + flow.label()));
        state.show("Correction préparée", "Vérifie le flux compensatoire puis enregistre-le.", "state-info");
    }

    private String correctionType(String type) {
        return switch (type == null ? "" : type) {
            case "DEPOSIT" -> "WITHDRAWAL";
            case "WITHDRAWAL" -> "DEPOSIT";
            case "TRANSFER_IN" -> "TRANSFER_OUT";
            case "TRANSFER_OUT" -> "TRANSFER_IN";
            default -> "CORRECTION";
        };
    }

    private boolean hasSnapshotOnDate(LocalDate date) {
        return currentSnapshotHistory.stream().anyMatch(snapshot -> date.equals(snapshot.valueDate()));
    }

    private boolean hasDuplicateFlow(LocalDate date, String type, BigDecimal amount, String currency, String label) {
        return currentCashFlows.stream().anyMatch(flow ->
                date.equals(flow.valueDate())
                        && Objects.equals(type, flow.type())
                        && flow.amount() != null
                        && flow.amount().amount() != null
                        && flow.amount().currencyCode() != null
                        && flow.amount().amount().compareTo(amount) == 0
                        && flow.amount().currencyCode().equalsIgnoreCase(currency)
                        && Objects.equals(normalizeLabel(flow.label()), normalizeLabel(label))
        );
    }

    private ValidationResult<BigDecimal> positiveAmount(String value, String fieldName) {
        ValidationResult<BigDecimal> result = InputValidation.amount(value, fieldName);
        if (!result.valid()) {
            return result;
        }
        if (result.value().signum() <= 0) {
            return ValidationResult.error(fieldName + " doit être strictement positif. Le type indique le sens du flux.");
        }
        return result;
    }

    private void restoreAccountSelection(ComboBox<AccountDto> comboBox, UUID accountId) {
        comboBox.setValue(null);
        if (accountId != null) {
            selectAccount(comboBox, accountId);
        }
        if (comboBox.getValue() == null && !comboBox.getItems().isEmpty()) {
            comboBox.setValue(comboBox.getItems().get(0));
        }
    }

    private void selectAccount(ComboBox<AccountDto> comboBox, UUID accountId) {
        comboBox.getItems().stream()
                .filter(account -> account.id().equals(accountId))
                .findFirst()
                .ifPresent(comboBox::setValue);
    }

    private Comparator<AccountSnapshotDto> snapshotComparator() {
        return Comparator.comparing(AccountSnapshotDto::valueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(AccountSnapshotDto::recordedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed();
    }

    private Comparator<CashFlowDto> cashFlowComparator() {
        return Comparator.comparing(CashFlowDto::valueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(CashFlowDto::recordedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed();
    }

    private String cashFlowType(String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }
        return switch (value) {
            case "DEPOSIT" -> "Versement";
            case "WITHDRAWAL" -> "Retrait";
            case "TRANSFER_IN" -> "Transfert entrant";
            case "TRANSFER_OUT" -> "Transfert sortant";
            case "INTEREST" -> "Intérêt";
            case "DIVIDEND" -> "Dividende";
            case "FEE" -> "Frais";
            case "TAX" -> "Taxe";
            case "CASHBACK" -> "Cashback";
            case "CORRECTION" -> "Correction";
            default -> value;
        };
    }

    private void setInputsDisabled(boolean disabled) {
        snapshotAccount.setDisable(disabled);
        snapshotDate.setDisable(disabled);
        snapshotAmount.setDisable(disabled);
        snapshotCurrency.setDisable(disabled);
        flowAccount.setDisable(disabled);
        flowType.setDisable(disabled);
        flowDate.setDisable(disabled);
        flowAmount.setDisable(disabled);
        flowCurrency.setDisable(disabled);
        flowLabel.setDisable(disabled);
        snapshotFilter.setDisable(disabled);
        flowFilter.setDisable(disabled);
        saveSnapshot.setDisable(disabled);
        saveFlow.setDisable(disabled);
    }

    private void clearData() {
        currentAccounts = List.of();
        currentSnapshotHistory = List.of();
        currentCashFlows = List.of();
        snapshotAccount.getItems().clear();
        flowAccount.getItems().clear();
        renderSnapshotHistory();
        renderCashFlowHistory();
    }

    private String nullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private String normalizeLabel(String value) {
        return value == null || value.isBlank() ? "" : value.strip().toLowerCase(Locale.ROOT);
    }

    private String normalized(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }

    public static final class SnapshotEntryRow {
        private final AccountSnapshotDto snapshot;
        private final String date;
        private final String balance;
        private final String recordedAt;

        private SnapshotEntryRow(AccountSnapshotDto snapshot, String date, String balance, String recordedAt) {
            this.snapshot = snapshot;
            this.date = date;
            this.balance = balance;
            this.recordedAt = recordedAt;
        }

        static SnapshotEntryRow from(AccountSnapshotDto snapshot) {
            return new SnapshotEntryRow(snapshot, DesktopFormatters.date(snapshot.valueDate()), DesktopFormatters.money(snapshot.balance()), DesktopFormatters.time(snapshot.recordedAt()));
        }

        AccountSnapshotDto snapshot() {
            return snapshot;
        }

        public String getDate() {
            return date;
        }

        public String getBalance() {
            return balance;
        }

        public String getRecordedAt() {
            return recordedAt;
        }
    }

    private CashFlowRow cashFlowRow(CashFlowDto cashFlow) {
        return new CashFlowRow(cashFlow, DesktopFormatters.date(cashFlow.valueDate()), cashFlowType(cashFlow.type()), DesktopFormatters.money(cashFlow.amount()), cashFlow.label() == null || cashFlow.label().isBlank() ? "—" : cashFlow.label());
    }

    public static final class CashFlowRow {
        private final CashFlowDto cashFlow;
        private final String date;
        private final String type;
        private final String amount;
        private final String label;

        private CashFlowRow(CashFlowDto cashFlow, String date, String type, String amount, String label) {
            this.cashFlow = cashFlow;
            this.date = date;
            this.type = type;
            this.amount = amount;
            this.label = label;
        }

        CashFlowDto cashFlow() {
            return cashFlow;
        }

        public String getDate() {
            return date;
        }

        public String getType() {
            return type;
        }

        public String getAmount() {
            return amount;
        }

        public String getLabel() {
            return label;
        }
    }
}
