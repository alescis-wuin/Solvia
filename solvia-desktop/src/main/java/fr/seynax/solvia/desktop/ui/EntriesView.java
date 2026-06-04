package fr.seynax.solvia.desktop.ui;

import java.math.BigDecimal;
import java.time.LocalDate;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import fr.seynax.solvia.desktop.api.ApiDtos.AccountDto;
import fr.seynax.solvia.desktop.api.ApiDtos.AccountSnapshotCreateDto;
import fr.seynax.solvia.desktop.api.ApiDtos.CashFlowCreateDto;
import fr.seynax.solvia.desktop.api.ApiDtos.MoneyDto;
import fr.seynax.solvia.desktop.api.BackendConnectionState;
import fr.seynax.solvia.desktop.api.BackendStatusSnapshot;
import fr.seynax.solvia.desktop.api.SolviaApiClient;
import fr.seynax.solvia.desktop.ui.InputValidation.ValidationResult;

public final class EntriesView extends VBox {

    private final SolviaApiClient apiClient;
    private final StateMessage state = new StateMessage();
    private final EmptyState empty = new EmptyState();
    private final ComboBox<AccountDto> snapshotAccount = Ui.tooltip(new ComboBox<>(), "Compte concerne par la valeur observee.");
    private final DatePicker snapshotDate = Ui.tooltip(new DatePicker(LocalDate.now()), "Date de valorisation observee.");
    private final TextField snapshotAmount = Ui.tooltip(new TextField(), "Montant total observe pour le compte.");
    private final TextField snapshotCurrency = Ui.tooltip(new TextField("EUR"), "Devise ISO du montant observe.");
    private final ComboBox<AccountDto> flowAccount = Ui.tooltip(new ComboBox<>(), "Compte concerne par le flux financier.");
    private final ComboBox<String> flowType = Ui.tooltip(new ComboBox<>(), "Nature du flux financier.");
    private final DatePicker flowDate = Ui.tooltip(new DatePicker(LocalDate.now()), "Date de valeur du flux financier.");
    private final TextField flowAmount = Ui.tooltip(new TextField(), "Montant du flux. Utiliser un point ou une virgule comme separateur decimal.");
    private final TextField flowCurrency = Ui.tooltip(new TextField("EUR"), "Devise ISO du flux financier.");
    private final TextField flowLabel = Ui.tooltip(new TextField(), "Libelle optionnel pour retrouver le flux.");
    private final Button saveSnapshot = Ui.tooltip(new Button("Enregistrer le snapshot"), "Ajoute une valeur observee pour le compte selectionne.");
    private final Button saveFlow = Ui.tooltip(new Button("Enregistrer le flux"), "Ajoute un flux financier important.");
    private volatile boolean backendReady;

    public EntriesView(SolviaApiClient apiClient) {
        this.apiClient = apiClient;
        getStyleClass().add("content-view");
        setSpacing(18);
        setPadding(new Insets(20));
        flowType.getItems().setAll("DEPOSIT", "WITHDRAWAL", "TRANSFER_IN", "TRANSFER_OUT", "INTEREST", "DIVIDEND", "FEE", "TAX", "CASHBACK", "CORRECTION");
        flowType.setValue("DEPOSIT");
        getChildren().addAll(snapshotForm(), flowForm(), state, empty);
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
            snapshotAccount.getItems().clear();
            flowAccount.getItems().clear();
            empty.show("Backend indisponible", "La saisie sera disponible quand le backend local sera connecte.");
            state.show("Backend non pret", snapshot.message(), "state-warning");
            return;
        }
        setInputsDisabled(false);
        refreshAccounts();
    }

    public void refreshAccounts() {
        if (!backendReady) {
            state.show("Backend non pret", "Backend local non pret.", "state-warning");
            return;
        }
        empty.hide();
        state.show("Chargement", "Chargement des comptes...", "state-info");
        apiClient.accounts().whenComplete((accounts, error) -> Platform.runLater(() -> {
            if (error != null) {
                state.show("Erreur", DesktopFormatters.errorMessage(error), "state-error");
                return;
            }
            snapshotAccount.getItems().setAll(accounts);
            flowAccount.getItems().setAll(accounts);
            if (!accounts.isEmpty()) {
                snapshotAccount.setValue(accounts.get(0));
                flowAccount.setValue(accounts.get(0));
            }
            if (accounts.isEmpty()) {
                empty.show("Aucun compte", "Cree un compte avant de saisir un snapshot ou un flux.");
                state.show("Aucun compte", "La saisie necessite au moins un compte.", "state-warning");
            } else {
                empty.hide();
                state.show("Comptes charges", "Comptes charges: " + accounts.size(), "state-success");
            }
        }));
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
        return new SectionCard("Snapshot de compte", "Saisis une valeur observee pour historiser un compte.", grid);
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
        grid.add(Ui.fieldLabel("Libelle", flowLabel), 2, 2);
        grid.add(flowLabel, 3, 2);
        grid.add(saveFlow, 1, 3);
        return new SectionCard("Flux financier", "Saisis les versements, retraits, interets, dividendes, frais ou corrections.", grid);
    }

    private void saveSnapshot() {
        if (!backendReady) {
            state.show("Backend non pret", "Backend local non pret.", "state-warning");
            return;
        }
        AccountDto account = snapshotAccount.getValue();
        if (account == null) {
            state.show("Compte requis", "Selectionner un compte avant la saisie.", "state-warning");
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
        AccountSnapshotCreateDto request = new AccountSnapshotCreateDto(
                account.id(),
                snapshotDate.getValue(),
                new MoneyDto(amount.value(), currency.value()),
                "OBSERVED",
                null
        );
        saveSnapshot.setDisable(true);
        state.show("Enregistrement", "Enregistrement du snapshot...", "state-info");
        apiClient.createAccountSnapshot(request).whenComplete((ignored, error) -> Platform.runLater(() -> {
            saveSnapshot.setDisable(false);
            if (error != null) {
                state.show("Erreur", DesktopFormatters.errorMessage(error), "state-error");
                return;
            }
            snapshotAmount.clear();
            state.show("Snapshot enregistre", "La valeur observee a ete ajoutee.", "state-success");
        }));
    }

    private void saveFlow() {
        if (!backendReady) {
            state.show("Backend non pret", "Backend local non pret.", "state-warning");
            return;
        }
        AccountDto account = flowAccount.getValue();
        if (account == null) {
            state.show("Compte requis", "Selectionner un compte avant la saisie.", "state-warning");
            return;
        }
        ValidationResult<BigDecimal> amount = InputValidation.amount(flowAmount.getText(), "Montant du flux");
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
        CashFlowCreateDto request = new CashFlowCreateDto(
                account.id(),
                flowType.getValue(),
                flowDate.getValue(),
                new MoneyDto(amount.value(), currency.value()),
                flowLabel.getText()
        );
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
            state.show("Flux enregistre", "Le flux financier a ete ajoute.", "state-success");
        }));
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
        saveSnapshot.setDisable(disabled);
        saveFlow.setDisable(disabled);
    }
}
