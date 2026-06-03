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
import fr.seynax.solvia.desktop.api.SolviaApiClient;

public final class EntriesView extends VBox {

    private final SolviaApiClient apiClient;
    private final Label status = new Label("Ready");
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

    public EntriesView(SolviaApiClient apiClient) {
        this.apiClient = apiClient;
        setSpacing(20);
        setPadding(new Insets(20));
        flowType.getItems().setAll("DEPOSIT", "WITHDRAWAL", "TRANSFER_IN", "TRANSFER_OUT", "INTEREST", "DIVIDEND", "FEE", "TAX", "CASHBACK", "CORRECTION");
        flowType.setValue("DEPOSIT");
        getChildren().addAll(snapshotForm(), flowForm(), status);
        refreshAccounts();
    }

    public void refreshAccounts() {
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
            status.setText("Accounts loaded: " + accounts.size());
        }));
    }

    private GridPane snapshotForm() {
        Button save = new Button("Save snapshot");
        save.setOnAction(event -> saveSnapshot());
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.add(new Label("Account snapshot"), 0, 0, 4, 1);
        grid.add(new Label("Account"), 0, 1);
        grid.add(snapshotAccount, 1, 1);
        grid.add(new Label("Date"), 2, 1);
        grid.add(snapshotDate, 3, 1);
        grid.add(new Label("Amount"), 0, 2);
        grid.add(snapshotAmount, 1, 2);
        grid.add(new Label("Currency"), 2, 2);
        grid.add(snapshotCurrency, 3, 2);
        grid.add(save, 1, 3);
        return grid;
    }

    private GridPane flowForm() {
        Button save = new Button("Save flow");
        save.setOnAction(event -> saveFlow());
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.add(new Label("Cash flow"), 0, 0, 4, 1);
        grid.add(new Label("Account"), 0, 1);
        grid.add(flowAccount, 1, 1);
        grid.add(new Label("Type"), 2, 1);
        grid.add(flowType, 3, 1);
        grid.add(new Label("Date"), 0, 2);
        grid.add(flowDate, 1, 2);
        grid.add(new Label("Amount"), 2, 2);
        grid.add(flowAmount, 3, 2);
        grid.add(new Label("Currency"), 0, 3);
        grid.add(flowCurrency, 1, 3);
        grid.add(new Label("Label"), 2, 3);
        grid.add(flowLabel, 3, 3);
        grid.add(save, 1, 4);
        return grid;
    }

    private void saveSnapshot() {
        AccountDto account = snapshotAccount.getValue();
        if (account == null) {
            status.setText("Select an account first");
            return;
        }
        AccountSnapshotCreateDto request = new AccountSnapshotCreateDto(
                account.id(),
                snapshotDate.getValue(),
                new MoneyDto(new BigDecimal(snapshotAmount.getText()), snapshotCurrency.getText()),
                "OBSERVED",
                null
        );
        apiClient.createAccountSnapshot(request).whenComplete((ignored, error) -> Platform.runLater(() -> {
            if (error != null) {
                status.setText(DesktopFormatters.errorMessage(error));
                return;
            }
            snapshotAmount.clear();
            status.setText("Snapshot saved");
        }));
    }

    private void saveFlow() {
        AccountDto account = flowAccount.getValue();
        if (account == null) {
            status.setText("Select an account first");
            return;
        }
        CashFlowCreateDto request = new CashFlowCreateDto(
                account.id(),
                flowType.getValue(),
                flowDate.getValue(),
                new MoneyDto(new BigDecimal(flowAmount.getText()), flowCurrency.getText()),
                flowLabel.getText()
        );
        apiClient.createCashFlow(request).whenComplete((ignored, error) -> Platform.runLater(() -> {
            if (error != null) {
                status.setText(DesktopFormatters.errorMessage(error));
                return;
            }
            flowAmount.clear();
            flowLabel.clear();
            status.setText("Cash flow saved");
        }));
    }
}
