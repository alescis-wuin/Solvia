package fr.seynax.solvia.desktop.ui;

import java.math.BigDecimal;
import java.time.LocalDate;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
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

public final class EntriesView extends VBox {

    private final SolviaApiClient apiClient;
    private final Label status = new Label("Vérification du backend local...");
    private final ComboBox<AccountDto> snapshotAccount = new ComboBox<>();
    private final DatePicker snapshotDate = new DatePicker(LocalDate.now());
    private final TextField snapshotAmount = new TextField();
    private final TextField snapshotCurrency = new TextField("EUR");
    private final ComboBox<AccountDto> flowAccount = new ComboBox<>();
    private final ComboBox<String> flowType = new ComboBox<>();
    private final DatePicker flowDate = new DatePicker(LocalDate.now());
    private final TextField flowAmount = new TextField();
    private final TextField flowCurrency = new TextField("EUR");
    private final TextField flowLabel = new TextField();
    private final Button saveSnapshot = new Button("Enregistrer le snapshot");
    private final Button saveFlow = new Button("Enregistrer le flux");
    private volatile boolean backendReady;

    public EntriesView(SolviaApiClient apiClient) {
        this.apiClient = apiClient;
        getStyleClass().add("content-view");
        setSpacing(20);
        setPadding(new Insets(20));
        flowType.getItems().setAll("DEPOSIT", "WITHDRAWAL", "TRANSFER_IN", "TRANSFER_OUT", "INTEREST", "DIVIDEND", "FEE", "TAX", "CASHBACK", "CORRECTION");
        flowType.setValue("DEPOSIT");
        status.getStyleClass().add("help-text");
        getChildren().addAll(snapshotForm(), flowForm(), status);
        setInputsDisabled(true);
    }

    public void backendStatusChanged(BackendStatusSnapshot snapshot) {
        backendReady = snapshot.canLoadData();
        if (snapshot.state() == BackendConnectionState.CHECKING) {
            setInputsDisabled(true);
            status.setText("Vérification du backend local...");
            return;
        }
        if (!backendReady) {
            setInputsDisabled(true);
            snapshotAccount.getItems().clear();
            flowAccount.getItems().clear();
            status.setText(snapshot.message());
            return;
        }
        setInputsDisabled(false);
        refreshAccounts();
    }

    public void refreshAccounts() {
        if (!backendReady) {
            status.setText("Backend local non prêt.");
            return;
        }
        status.setText("Chargement des comptes...");
        apiClient.accounts().whenComplete((accounts, error) -> Platform.runLater(() -> {
            if (error != null) {
                status.setText(DesktopFormatters.errorMessage(error));
                return;
            }
            snapshotAccount.getItems().setAll(accounts);
            flowAccount.getItems().setAll(accounts);
            if (!accounts.isEmpty()) {
                snapshotAccount.setValue(accounts.get(0));
                flowAccount.setValue(accounts.get(0));
            }
            status.setText(accounts.isEmpty()
                    ? "Créer un compte avant de saisir un snapshot ou un flux."
                    : "Comptes chargés: " + accounts.size());
        }));
    }

    private GridPane snapshotForm() {
        saveSnapshot.setOnAction(event -> saveSnapshot());
        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-card");
        grid.setHgap(12);
        grid.setVgap(8);
        grid.add(new Label("Snapshot de compte"), 0, 0, 4, 1);
        grid.add(new Label("Compte"), 0, 1);
        grid.add(snapshotAccount, 1, 1);
        grid.add(new Label("Date"), 2, 1);
        grid.add(snapshotDate, 3, 1);
        grid.add(new Label("Montant"), 0, 2);
        grid.add(snapshotAmount, 1, 2);
        grid.add(new Label("Devise"), 2, 2);
        grid.add(snapshotCurrency, 3, 2);
        grid.add(saveSnapshot, 1, 3);
        return grid;
    }

    private GridPane flowForm() {
        saveFlow.setOnAction(event -> saveFlow());
        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-card");
        grid.setHgap(12);
        grid.setVgap(8);
        grid.add(new Label("Flux financier"), 0, 0, 4, 1);
        grid.add(new Label("Compte"), 0, 1);
        grid.add(flowAccount, 1, 1);
        grid.add(new Label("Type"), 2, 1);
        grid.add(flowType, 3, 1);
        grid.add(new Label("Date"), 0, 2);
        grid.add(flowDate, 1, 2);
        grid.add(new Label("Montant"), 2, 2);
        grid.add(flowAmount, 3, 2);
        grid.add(new Label("Devise"), 0, 3);
        grid.add(flowCurrency, 1, 3);
        grid.add(new Label("Libellé"), 2, 3);
        grid.add(flowLabel, 3, 3);
        grid.add(saveFlow, 1, 4);
        return grid;
    }

    private void saveSnapshot() {
        if (!backendReady) {
            status.setText("Backend local non prêt.");
            return;
        }
        AccountDto account = snapshotAccount.getValue();
        if (account == null) {
            status.setText("Sélectionner un compte avant la saisie.");
            return;
        }
        BigDecimal amount = parseAmount(snapshotAmount.getText());
        if (amount == null) {
            return;
        }
        AccountSnapshotCreateDto request = new AccountSnapshotCreateDto(
                account.id(),
                snapshotDate.getValue(),
                new MoneyDto(amount, snapshotCurrency.getText()),
                "OBSERVED",
                null
        );
        saveSnapshot.setDisable(true);
        status.setText("Enregistrement du snapshot...");
        apiClient.createAccountSnapshot(request).whenComplete((ignored, error) -> Platform.runLater(() -> {
            saveSnapshot.setDisable(false);
            if (error != null) {
                status.setText(DesktopFormatters.errorMessage(error));
                return;
            }
            snapshotAmount.clear();
            status.setText("Snapshot enregistré.");
        }));
    }

    private void saveFlow() {
        if (!backendReady) {
            status.setText("Backend local non prêt.");
            return;
        }
        AccountDto account = flowAccount.getValue();
        if (account == null) {
            status.setText("Sélectionner un compte avant la saisie.");
            return;
        }
        BigDecimal amount = parseAmount(flowAmount.getText());
        if (amount == null) {
            return;
        }
        CashFlowCreateDto request = new CashFlowCreateDto(
                account.id(),
                flowType.getValue(),
                flowDate.getValue(),
                new MoneyDto(amount, flowCurrency.getText()),
                flowLabel.getText()
        );
        saveFlow.setDisable(true);
        status.setText("Enregistrement du flux...");
        apiClient.createCashFlow(request).whenComplete((ignored, error) -> Platform.runLater(() -> {
            saveFlow.setDisable(false);
            if (error != null) {
                status.setText(DesktopFormatters.errorMessage(error));
                return;
            }
            flowAmount.clear();
            flowLabel.clear();
            status.setText("Flux enregistré.");
        }));
    }

    private BigDecimal parseAmount(String text) {
        try {
            return new BigDecimal(text.strip().replace(',', '.'));
        } catch (RuntimeException exception) {
            status.setText("Montant invalide. Exemple attendu: 1234.56");
            return null;
        }
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
