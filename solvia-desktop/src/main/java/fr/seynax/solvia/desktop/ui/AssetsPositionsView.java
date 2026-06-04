package fr.seynax.solvia.desktop.ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import fr.seynax.solvia.desktop.api.ApiDtos.AccountDto;
import fr.seynax.solvia.desktop.api.ApiDtos.AssetCreateDto;
import fr.seynax.solvia.desktop.api.ApiDtos.AssetDto;
import fr.seynax.solvia.desktop.api.ApiDtos.AssetUpdateDto;
import fr.seynax.solvia.desktop.api.ApiDtos.MoneyDto;
import fr.seynax.solvia.desktop.api.ApiDtos.PositionCreateDto;
import fr.seynax.solvia.desktop.api.ApiDtos.PositionDto;
import fr.seynax.solvia.desktop.api.ApiDtos.PositionSnapshotCreateDto;
import fr.seynax.solvia.desktop.api.ApiDtos.PositionSnapshotDto;
import fr.seynax.solvia.desktop.api.ApiDtos.PositionUpdateDto;
import fr.seynax.solvia.desktop.api.BackendConnectionState;
import fr.seynax.solvia.desktop.api.BackendStatusSnapshot;
import fr.seynax.solvia.desktop.api.SolviaApiClient;
import fr.seynax.solvia.desktop.ui.InputValidation.ValidationResult;

public final class AssetsPositionsView extends VBox {

    private final SolviaApiClient apiClient;
    private final StateMessage state = new StateMessage();
    private final EmptyState empty = new EmptyState();

    private final TableView<AssetRow> assetTable = new TableView<>();
    private final TableView<PositionRow> positionTable = new TableView<>();
    private final TableView<SnapshotRow> snapshotTable = new TableView<>();

    private final TextField assetName = Ui.tooltip(new TextField(), "Nom lisible de l'actif.");
    private final ComboBox<String> assetType = Ui.tooltip(new ComboBox<>(), "Classe de l'actif.");
    private final TextField assetCurrency = Ui.tooltip(new TextField("EUR"), "Devise ISO de cotation ou de reference.");
    private final TextField assetSymbol = Ui.tooltip(new TextField(), "Symbole optionnel, par exemple CW8, AAPL ou BTC.");
    private final Label assetTypeHelp = Ui.help("—");
    private final Button createAsset = Ui.tooltip(new Button("Creer l'actif"), "Ajoute l'actif au referentiel local.");
    private final Button updateAsset = Ui.tooltip(new Button("Mettre a jour"), "Modifie l'actif selectionne.");
    private final Button deactivateAsset = Ui.tooltip(new Button("Desactiver"), "Desactive l'actif selectionne sans supprimer son historique.");
    private final Button clearAssetSelection = Ui.tooltip(new Button("Nouveau"), "Vide la selection pour creer un nouvel actif.");

    private final ComboBox<AccountDto> positionAccount = Ui.tooltip(new ComboBox<>(), "Compte ou enveloppe qui detient la position.");
    private final ComboBox<AssetDto> positionAsset = Ui.tooltip(new ComboBox<>(), "Actif detenu dans le compte.");
    private final TextField positionQuantity = Ui.tooltip(new TextField("0"), "Quantite initiale ou courante de la position.");
    private final Button createPosition = Ui.tooltip(new Button("Creer la position"), "Lie un actif a un compte.");
    private final Button updatePosition = Ui.tooltip(new Button("Mettre a jour"), "Modifie la quantite de la position selectionnee.");
    private final Button deactivatePosition = Ui.tooltip(new Button("Desactiver"), "Desactive la position selectionnee sans supprimer son historique.");
    private final Button clearPositionSelection = Ui.tooltip(new Button("Nouvelle"), "Vide la selection pour creer une nouvelle position.");

    private final ComboBox<PositionChoice> snapshotPosition = Ui.tooltip(new ComboBox<>(), "Position a valoriser.");
    private final DatePicker snapshotDate = Ui.tooltip(new DatePicker(LocalDate.now()), "Date de valorisation observee.");
    private final TextField snapshotQuantity = Ui.tooltip(new TextField(), "Quantite observee a cette date.");
    private final TextField snapshotValue = Ui.tooltip(new TextField(), "Valeur de marche totale observee.");
    private final TextField snapshotCurrency = Ui.tooltip(new TextField("EUR"), "Devise ISO de la valeur de marche.");
    private final Button saveSnapshot = Ui.tooltip(new Button("Enregistrer la valorisation"), "Ajoute un snapshot de position.");

    private final Label assetDetail = Ui.help("Selectionne un actif pour voir son detail.");
    private final Label positionDetail = Ui.help("Selectionne une position pour voir son detail et son historique.");

    private volatile boolean backendReady;
    private WorkspaceData currentData = WorkspaceData.empty();
    private AssetDto selectedAsset;
    private PositionDto selectedPosition;
    private Runnable onPortfolioDataChanged = () -> { };

    public AssetsPositionsView(SolviaApiClient apiClient) {
        this.apiClient = apiClient;
        getStyleClass().add("content-view");
        setSpacing(18);
        setPadding(new Insets(20));
        configureCombos();
        configureTables();
        getChildren().addAll(forms(), tables(), details(), state, empty);
        setInputsDisabled(true);
        state.show("Verification", "Verification du backend local...", "state-info");
    }

    public void setOnPortfolioDataChanged(Runnable onPortfolioDataChanged) {
        this.onPortfolioDataChanged = onPortfolioDataChanged == null ? () -> { } : onPortfolioDataChanged;
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
            clearData();
            empty.show("Backend indisponible", "Les actifs et positions seront disponibles quand le backend local sera connecte.");
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
        empty.hide();
        setInputsDisabled(true);
        state.show("Chargement", "Chargement des comptes, actifs et positions...", "state-info");
        loadWorkspaceData().whenComplete((data, error) -> Platform.runLater(() -> {
            setInputsDisabled(false);
            if (error != null) {
                state.show("Erreur", DesktopFormatters.errorMessage(error), "state-error");
                return;
            }
            updateData(data);
        }));
    }

    private HBox forms() {
        HBox row = new HBox(18, assetForm(), positionForm(), snapshotForm());
        row.getStyleClass().add("dashboard-body");
        return row;
    }

    private HBox tables() {
        HBox row = new HBox(18, assetsTable(), positionsTable());
        HBox.setHgrow(positionTable, Priority.ALWAYS);
        return row;
    }

    private HBox details() {
        HBox row = new HBox(18, assetDetailCard(), positionDetailCard(), snapshotHistoryCard());
        row.getStyleClass().add("dashboard-body");
        HBox.setHgrow(snapshotTable, Priority.ALWAYS);
        return row;
    }

    private SectionCard assetForm() {
        createAsset.setOnAction(event -> createAsset());
        updateAsset.setOnAction(event -> updateAsset());
        deactivateAsset.setOnAction(event -> deactivateAsset());
        clearAssetSelection.setOnAction(event -> clearSelectedAsset());
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.add(Ui.fieldLabel("Nom", assetName), 0, 0);
        grid.add(assetName, 1, 0);
        grid.add(Ui.fieldLabel("Type", assetType), 0, 1);
        grid.add(assetType, 1, 1);
        grid.add(Ui.fieldLabel("Devise", assetCurrency), 0, 2);
        grid.add(assetCurrency, 1, 2);
        grid.add(Ui.fieldLabel("Symbole", assetSymbol), 0, 3);
        grid.add(assetSymbol, 1, 3);
        grid.add(assetTypeHelp, 0, 4, 2, 1);
        grid.add(new HBox(8, createAsset, updateAsset, deactivateAsset, clearAssetSelection), 0, 5, 2, 1);
        return new SectionCard("Actif", "Cree ou modifie un actif suivi par Solvia.", grid);
    }

    private SectionCard positionForm() {
        createPosition.setOnAction(event -> createPosition());
        updatePosition.setOnAction(event -> updatePosition());
        deactivatePosition.setOnAction(event -> deactivatePosition());
        clearPositionSelection.setOnAction(event -> clearSelectedPosition());
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.add(Ui.fieldLabel("Compte", positionAccount), 0, 0);
        grid.add(positionAccount, 1, 0);
        grid.add(Ui.fieldLabel("Actif", positionAsset), 0, 1);
        grid.add(positionAsset, 1, 1);
        grid.add(Ui.fieldLabel("Quantite", positionQuantity), 0, 2);
        grid.add(positionQuantity, 1, 2);
        grid.add(new HBox(8, createPosition, updatePosition, deactivatePosition, clearPositionSelection), 0, 3, 2, 1);
        return new SectionCard("Position", "Associe un actif a un compte, puis maintiens sa quantite courante.", grid);
    }

    private SectionCard snapshotForm() {
        saveSnapshot.setOnAction(event -> savePositionSnapshot());
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.add(Ui.fieldLabel("Position", snapshotPosition), 0, 0);
        grid.add(snapshotPosition, 1, 0);
        grid.add(Ui.fieldLabel("Date", snapshotDate), 0, 1);
        grid.add(snapshotDate, 1, 1);
        grid.add(Ui.fieldLabel("Quantite", snapshotQuantity), 0, 2);
        grid.add(snapshotQuantity, 1, 2);
        grid.add(Ui.fieldLabel("Valeur", snapshotValue), 0, 3);
        grid.add(snapshotValue, 1, 3);
        grid.add(Ui.fieldLabel("Devise", snapshotCurrency), 0, 4);
        grid.add(snapshotCurrency, 1, 4);
        grid.add(saveSnapshot, 1, 5);
        return new SectionCard("Valorisation", "Saisis la quantite et la valeur totale observees pour une position.", grid);
    }

    private SectionCard assetsTable() {
        return new SectionCard("Actifs suivis", "Selectionne une ligne pour afficher et modifier l'actif.", assetTable);
    }

    private SectionCard positionsTable() {
        return new SectionCard("Positions", "Selectionne une ligne pour afficher, modifier et consulter l'historique.", positionTable);
    }

    private SectionCard assetDetailCard() {
        return new SectionCard("Detail actif", assetDetail);
    }

    private SectionCard positionDetailCard() {
        return new SectionCard("Detail position", positionDetail);
    }

    private SectionCard snapshotHistoryCard() {
        return new SectionCard("Historique de valorisation", "Snapshots de la position selectionnee.", snapshotTable);
    }

    private void configureTables() {
        TableColumn<AssetRow, String> assetNameColumn = new TableColumn<>("Actif");
        assetNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<AssetRow, String> assetTypeColumn = new TableColumn<>("Type");
        assetTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        TableColumn<AssetRow, String> assetCurrencyColumn = new TableColumn<>("Devise");
        assetCurrencyColumn.setCellValueFactory(new PropertyValueFactory<>("currency"));
        TableColumn<AssetRow, String> assetStatusColumn = new TableColumn<>("Etat");
        assetStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        assetTable.getColumns().setAll(assetNameColumn, assetTypeColumn, assetCurrencyColumn, assetStatusColumn);
        assetTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        assetTable.setPrefHeight(260);
        assetTable.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) -> selectAsset(selected == null ? null : selected.asset()));

        TableColumn<PositionRow, String> account = new TableColumn<>("Compte");
        account.setCellValueFactory(new PropertyValueFactory<>("account"));
        TableColumn<PositionRow, String> asset = new TableColumn<>("Actif");
        asset.setCellValueFactory(new PropertyValueFactory<>("asset"));
        TableColumn<PositionRow, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(new PropertyValueFactory<>("type"));
        TableColumn<PositionRow, String> quantity = new TableColumn<>("Quantite");
        quantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        TableColumn<PositionRow, String> status = new TableColumn<>("Etat");
        status.setCellValueFactory(new PropertyValueFactory<>("status"));
        positionTable.getColumns().setAll(account, asset, type, quantity, status);
        positionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        positionTable.setPrefHeight(260);
        positionTable.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) -> selectPosition(selected == null ? null : selected.position()));

        TableColumn<SnapshotRow, String> date = new TableColumn<>("Date");
        date.setCellValueFactory(new PropertyValueFactory<>("date"));
        TableColumn<SnapshotRow, String> snapshotQuantityColumn = new TableColumn<>("Quantite");
        snapshotQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        TableColumn<SnapshotRow, String> marketValue = new TableColumn<>("Valeur");
        marketValue.setCellValueFactory(new PropertyValueFactory<>("marketValue"));
        TableColumn<SnapshotRow, String> confidence = new TableColumn<>("Confiance");
        confidence.setCellValueFactory(new PropertyValueFactory<>("confidence"));
        snapshotTable.getColumns().setAll(date, snapshotQuantityColumn, marketValue, confidence);
        snapshotTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        snapshotTable.setPrefHeight(220);
    }

    private CompletableFuture<WorkspaceData> loadWorkspaceData() {
        return apiClient.accounts().thenCombine(apiClient.assets(), BaseData::new)
                .thenCompose(base -> {
                    if (base.accounts().isEmpty()) {
                        return CompletableFuture.completedFuture(new WorkspaceData(base.accounts(), base.assets(), List.of()));
                    }
                    List<CompletableFuture<List<PositionDto>>> futures = base.accounts().stream()
                            .map(account -> apiClient.positions(account.id()))
                            .toList();
                    return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                            .thenApply(ignored -> {
                                List<PositionDto> positions = new ArrayList<>();
                                for (CompletableFuture<List<PositionDto>> future : futures) {
                                    positions.addAll(future.join());
                                }
                                return new WorkspaceData(base.accounts(), base.assets(), positions);
                            });
                });
    }

    private void updateData(WorkspaceData data) {
        UUID previousAssetId = selectedAsset == null ? null : selectedAsset.id();
        UUID previousPositionId = selectedPosition == null ? null : selectedPosition.id();
        currentData = data;
        Map<UUID, AccountDto> accountsById = data.accountsById();
        Map<UUID, AssetDto> assetsById = data.assetsById();
        assetTable.getItems().setAll(data.assets().stream().map(AssetRow::from).toList());
        positionTable.getItems().setAll(data.positions().stream()
                .map(position -> PositionRow.from(position, accountsById, assetsById))
                .sorted(Comparator.comparing(PositionRow::getAccount).thenComparing(PositionRow::getAsset))
                .toList());
        positionAccount.getItems().setAll(data.accounts());
        positionAsset.getItems().setAll(data.assets());
        snapshotPosition.getItems().setAll(data.positions().stream()
                .map(position -> PositionChoice.from(position, accountsById, assetsById))
                .toList());
        selectFirstValues();
        reselect(previousAssetId, previousPositionId);
        if (data.accounts().isEmpty()) {
            empty.show("Aucun compte", "Cree un compte avant d'ajouter une position.");
            state.show("Aucun compte", "Les positions necessitent au moins un compte.", "state-warning");
        } else if (data.assets().isEmpty()) {
            empty.show("Aucun actif", "Cree un actif avant d'ajouter une position.");
            state.show("Aucun actif", "Le referentiel d'actifs est vide.", "state-warning");
        } else {
            empty.hide();
            state.show("Donnees chargees", "Actifs: " + data.assets().size() + " / Positions: " + data.positions().size(), "state-success");
        }
    }

    private void createAsset() {
        AssetCreateDto request = readAssetCreateRequest();
        if (request == null) {
            return;
        }
        createAsset.setDisable(true);
        state.show("Enregistrement", "Creation de l'actif...", "state-info");
        apiClient.createAsset(request).whenComplete((asset, error) -> Platform.runLater(() -> {
            createAsset.setDisable(false);
            if (error != null) {
                state.show("Erreur", DesktopFormatters.errorMessage(error), "state-error");
                return;
            }
            clearSelectedAsset();
            state.show("Actif cree", asset.name() + " a ete ajoute.", "state-success");
            refresh();
        }));
    }

    private void updateAsset() {
        if (selectedAsset == null) {
            state.show("Selection requise", "Selectionner un actif avant modification.", "state-warning");
            return;
        }
        AssetUpdateDto request = readAssetUpdateRequest(selectedAsset.active());
        if (request == null) {
            return;
        }
        updateAsset.setDisable(true);
        state.show("Enregistrement", "Mise a jour de l'actif...", "state-info");
        apiClient.updateAsset(selectedAsset.id(), request).whenComplete((asset, error) -> Platform.runLater(() -> {
            updateAsset.setDisable(false);
            if (error != null) {
                state.show("Erreur", DesktopFormatters.errorMessage(error), "state-error");
                return;
            }
            selectedAsset = asset;
            state.show("Actif mis a jour", asset.name() + " a ete modifie.", "state-success");
            refresh();
        }));
    }

    private void deactivateAsset() {
        if (selectedAsset == null) {
            state.show("Selection requise", "Selectionner un actif avant desactivation.", "state-warning");
            return;
        }
        deactivateAsset.setDisable(true);
        state.show("Desactivation", "Desactivation de l'actif...", "state-info");
        apiClient.deactivateAsset(selectedAsset.id()).whenComplete((ignored, error) -> Platform.runLater(() -> {
            deactivateAsset.setDisable(false);
            if (error != null) {
                state.show("Erreur", DesktopFormatters.errorMessage(error), "state-error");
                return;
            }
            clearSelectedAsset();
            state.show("Actif desactive", "L'actif reste conserve dans l'historique.", "state-success");
            refresh();
        }));
    }

    private void createPosition() {
        AccountDto account = positionAccount.getValue();
        AssetDto asset = positionAsset.getValue();
        if (account == null || asset == null) {
            state.show("Selection requise", "Selectionner un compte et un actif.", "state-warning");
            return;
        }
        if (hasActiveDuplicatePosition(account.id(), asset.id())) {
            state.show("Position existante", "Une position active existe deja pour ce compte et cet actif.", "state-warning");
            return;
        }
        ValidationResult<BigDecimal> quantity = zeroOrPositive(positionQuantity.getText(), "Quantite");
        if (!quantity.valid()) {
            state.show("Quantite invalide", quantity.message(), "state-warning");
            return;
        }
        PositionCreateDto request = new PositionCreateDto(account.id(), asset.id(), quantity.value());
        createPosition.setDisable(true);
        state.show("Enregistrement", "Creation de la position...", "state-info");
        apiClient.createPosition(request).whenComplete((position, error) -> Platform.runLater(() -> {
            createPosition.setDisable(false);
            if (error != null) {
                state.show("Erreur", DesktopFormatters.errorMessage(error), "state-error");
                return;
            }
            selectedPosition = position;
            positionQuantity.setText("0");
            state.show("Position creee", "La position a ete ajoutee.", "state-success");
            refresh();
        }));
    }

    private void updatePosition() {
        if (selectedPosition == null) {
            state.show("Selection requise", "Selectionner une position avant modification.", "state-warning");
            return;
        }
        ValidationResult<BigDecimal> quantity = zeroOrPositive(positionQuantity.getText(), "Quantite");
        if (!quantity.valid()) {
            state.show("Quantite invalide", quantity.message(), "state-warning");
            return;
        }
        PositionUpdateDto request = new PositionUpdateDto(quantity.value(), selectedPosition.active());
        updatePosition.setDisable(true);
        state.show("Enregistrement", "Mise a jour de la position...", "state-info");
        apiClient.updatePosition(selectedPosition.id(), request).whenComplete((position, error) -> Platform.runLater(() -> {
            updatePosition.setDisable(false);
            if (error != null) {
                state.show("Erreur", DesktopFormatters.errorMessage(error), "state-error");
                return;
            }
            selectedPosition = position;
            state.show("Position mise a jour", "La quantite courante a ete modifiee.", "state-success");
            refresh();
        }));
    }

    private void deactivatePosition() {
        if (selectedPosition == null) {
            state.show("Selection requise", "Selectionner une position avant desactivation.", "state-warning");
            return;
        }
        deactivatePosition.setDisable(true);
        state.show("Desactivation", "Desactivation de la position...", "state-info");
        apiClient.deactivatePosition(selectedPosition.id()).whenComplete((ignored, error) -> Platform.runLater(() -> {
            deactivatePosition.setDisable(false);
            if (error != null) {
                state.show("Erreur", DesktopFormatters.errorMessage(error), "state-error");
                return;
            }
            clearSelectedPosition();
            state.show("Position desactivee", "La position reste conservee dans l'historique.", "state-success");
            refresh();
            onPortfolioDataChanged.run();
        }));
    }

    private void savePositionSnapshot() {
        PositionChoice position = snapshotPosition.getValue();
        if (position == null) {
            state.show("Position requise", "Creer ou selectionner une position avant la valorisation.", "state-warning");
            return;
        }
        ValidationResult<BigDecimal> quantity = zeroOrPositive(snapshotQuantity.getText(), "Quantite");
        if (!quantity.valid()) {
            state.show("Quantite invalide", quantity.message(), "state-warning");
            return;
        }
        ValidationResult<BigDecimal> value = zeroOrPositive(snapshotValue.getText(), "Valeur de marche");
        if (!value.valid()) {
            state.show("Valeur invalide", value.message(), "state-warning");
            return;
        }
        ValidationResult<String> currency = InputValidation.currencyCode(snapshotCurrency.getText(), "Devise de valorisation");
        if (!currency.valid()) {
            state.show("Devise invalide", currency.message(), "state-warning");
            return;
        }
        snapshotCurrency.setText(currency.value());
        PositionSnapshotCreateDto request = new PositionSnapshotCreateDto(
                position.position().id(),
                snapshotDate.getValue(),
                quantity.value(),
                new MoneyDto(value.value(), currency.value()),
                "OBSERVED"
        );
        saveSnapshot.setDisable(true);
        state.show("Enregistrement", "Enregistrement de la valorisation...", "state-info");
        apiClient.createPositionSnapshot(request).whenComplete((ignored, error) -> Platform.runLater(() -> {
            saveSnapshot.setDisable(false);
            if (error != null) {
                state.show("Erreur", DesktopFormatters.errorMessage(error), "state-error");
                return;
            }
            snapshotQuantity.clear();
            snapshotValue.clear();
            state.show("Valorisation enregistree", "Le snapshot de position a ete ajoute.", "state-success");
            loadSnapshotHistory(position.position().id());
            refresh();
            onPortfolioDataChanged.run();
        }));
    }

    private AssetCreateDto readAssetCreateRequest() {
        AssetUpdateDto update = readAssetUpdateRequest(true);
        return update == null ? null : new AssetCreateDto(update.name(), update.type(), update.currencyCode(), update.symbol());
    }

    private AssetUpdateDto readAssetUpdateRequest(boolean active) {
        String name = assetName.getText() == null ? "" : assetName.getText().strip();
        if (name.isBlank()) {
            state.show("Nom requis", "Saisir un nom d'actif.", "state-warning");
            return null;
        }
        ValidationResult<String> currency = InputValidation.currencyCode(assetCurrency.getText(), "Devise de l'actif");
        if (!currency.valid()) {
            state.show("Devise invalide", currency.message(), "state-warning");
            return null;
        }
        assetCurrency.setText(currency.value());
        return new AssetUpdateDto(name, assetType.getValue(), currency.value(), nullIfBlank(assetSymbol.getText()), active);
    }

    private void selectAsset(AssetDto asset) {
        selectedAsset = asset;
        if (asset == null) {
            clearAssetForm();
            assetDetail.setText("Selectionne un actif pour voir son detail.");
            return;
        }
        assetName.setText(asset.name());
        assetType.setValue(asset.type());
        assetCurrency.setText(asset.currencyCode());
        assetSymbol.setText(asset.symbol() == null ? "" : asset.symbol());
        assetDetail.setText("Nom: " + asset.name()
                + "\nType: " + DesktopFormatters.assetType(asset.type())
                + "\nDevise: " + asset.currencyCode()
                + "\nSymbole: " + (asset.symbol() == null || asset.symbol().isBlank() ? "—" : asset.symbol())
                + "\nEtat: " + status(asset.active())
                + "\nCreation: " + DesktopFormatters.time(asset.createdAt()));
    }

    private void selectPosition(PositionDto position) {
        selectedPosition = position;
        if (position == null) {
            positionDetail.setText("Selectionne une position pour voir son detail et son historique.");
            snapshotTable.getItems().clear();
            return;
        }
        AccountDto account = currentData.accountsById().get(position.accountId());
        AssetDto asset = currentData.assetsById().get(position.assetId());
        positionAccount.setValue(account);
        positionAsset.setValue(asset);
        positionQuantity.setText(position.quantity() == null ? "0" : position.quantity().toPlainString());
        snapshotPosition.getItems().stream()
                .filter(choice -> choice.position().id().equals(position.id()))
                .findFirst()
                .ifPresent(snapshotPosition::setValue);
        positionDetail.setText("Compte: " + (account == null ? position.accountId() : account.name())
                + "\nActif: " + (asset == null ? position.assetId() : asset.name())
                + "\nType: " + (asset == null ? "—" : DesktopFormatters.assetType(asset.type()))
                + "\nQuantite: " + DesktopFormatters.decimal(position.quantity())
                + "\nEtat: " + status(position.active())
                + "\nCreation: " + DesktopFormatters.time(position.createdAt()));
        loadSnapshotHistory(position.id());
    }

    private void loadSnapshotHistory(UUID positionId) {
        snapshotTable.getItems().clear();
        apiClient.positionSnapshots(positionId).whenComplete((snapshots, error) -> Platform.runLater(() -> {
            if (error != null) {
                state.show("Erreur", DesktopFormatters.errorMessage(error), "state-error");
                return;
            }
            snapshotTable.getItems().setAll(snapshots.stream()
                    .sorted(Comparator.comparing(PositionSnapshotDto::valueDate).reversed())
                    .map(SnapshotRow::from)
                    .toList());
        }));
    }

    private void clearSelectedAsset() {
        selectedAsset = null;
        assetTable.getSelectionModel().clearSelection();
        clearAssetForm();
        assetDetail.setText("Selectionne un actif pour voir son detail.");
    }

    private void clearSelectedPosition() {
        selectedPosition = null;
        positionTable.getSelectionModel().clearSelection();
        positionQuantity.setText("0");
        snapshotTable.getItems().clear();
        positionDetail.setText("Selectionne une position pour voir son detail et son historique.");
    }

    private void clearAssetForm() {
        assetName.clear();
        assetType.setValue("ETF");
        assetCurrency.setText("EUR");
        assetSymbol.clear();
    }

    private boolean hasActiveDuplicatePosition(UUID accountId, UUID assetId) {
        return currentData.positions().stream()
                .filter(PositionDto::active)
                .anyMatch(position -> position.accountId().equals(accountId) && position.assetId().equals(assetId));
    }

    private ValidationResult<BigDecimal> zeroOrPositive(String value, String fieldName) {
        ValidationResult<BigDecimal> result = InputValidation.amount(value, fieldName);
        if (!result.valid()) {
            return result;
        }
        if (result.value().signum() < 0) {
            return ValidationResult.error(fieldName + " doit etre positif ou nul.");
        }
        return result;
    }

    private void configureCombos() {
        assetType.getItems().setAll("FIAT_CURRENCY", "STOCK", "ETF", "BOND", "CRYPTO_ASSET", "PRIVATE_EQUITY", "REAL_ESTATE", "CASHBACK_REWARD", "OTHER");
        assetType.setValue("ETF");
        assetTypeHelp.setText(assetTypeHelp(assetType.getValue()));
        assetType.valueProperty().addListener((observable, previous, value) -> assetTypeHelp.setText(assetTypeHelp(value)));
    }

    private String assetTypeHelp(String type) {
        return switch (type == null ? "" : type) {
            case "FIAT_CURRENCY" -> "Liquidites ou devise suivie comme actif de reference.";
            case "STOCK" -> "Action cotee detenue directement.";
            case "ETF" -> "Fonds indiciel ou ETF, utile pour PEA/CTO.";
            case "BOND" -> "Obligation, fonds obligataire ou support assimile.";
            case "CRYPTO_ASSET" -> "Crypto-actif conserve sur plateforme ou wallet.";
            case "PRIVATE_EQUITY" -> "Actif non cote ou participation privee, souvent estimee.";
            case "REAL_ESTATE" -> "Immobilier ou part de support immobilier valorise manuellement.";
            case "CASHBACK_REWARD" -> "Recompense ou cashback valorise comme actif.";
            default -> "Classe generique pour les actifs non encore categorises.";
        };
    }

    private void selectFirstValues() {
        if (!positionAccount.getItems().isEmpty() && positionAccount.getValue() == null) {
            positionAccount.setValue(positionAccount.getItems().get(0));
        }
        if (!positionAsset.getItems().isEmpty() && positionAsset.getValue() == null) {
            positionAsset.setValue(positionAsset.getItems().get(0));
        }
        if (!snapshotPosition.getItems().isEmpty() && snapshotPosition.getValue() == null) {
            snapshotPosition.setValue(snapshotPosition.getItems().get(0));
        }
    }

    private void reselect(UUID assetId, UUID positionId) {
        if (assetId != null) {
            assetTable.getItems().stream()
                    .filter(row -> row.asset().id().equals(assetId))
                    .findFirst()
                    .ifPresent(row -> assetTable.getSelectionModel().select(row));
        }
        if (positionId != null) {
            positionTable.getItems().stream()
                    .filter(row -> row.position().id().equals(positionId))
                    .findFirst()
                    .ifPresent(row -> positionTable.getSelectionModel().select(row));
        }
    }

    private void setInputsDisabled(boolean disabled) {
        assetName.setDisable(disabled);
        assetType.setDisable(disabled);
        assetCurrency.setDisable(disabled);
        assetSymbol.setDisable(disabled);
        createAsset.setDisable(disabled);
        updateAsset.setDisable(disabled || selectedAsset == null);
        deactivateAsset.setDisable(disabled || selectedAsset == null);
        clearAssetSelection.setDisable(disabled);
        positionAccount.setDisable(disabled);
        positionAsset.setDisable(disabled);
        positionQuantity.setDisable(disabled);
        createPosition.setDisable(disabled);
        updatePosition.setDisable(disabled || selectedPosition == null);
        deactivatePosition.setDisable(disabled || selectedPosition == null);
        clearPositionSelection.setDisable(disabled);
        snapshotPosition.setDisable(disabled);
        snapshotDate.setDisable(disabled);
        snapshotQuantity.setDisable(disabled);
        snapshotValue.setDisable(disabled);
        snapshotCurrency.setDisable(disabled);
        saveSnapshot.setDisable(disabled);
    }

    private void clearData() {
        currentData = WorkspaceData.empty();
        selectedAsset = null;
        selectedPosition = null;
        assetTable.getItems().clear();
        positionTable.getItems().clear();
        snapshotTable.getItems().clear();
        positionAccount.getItems().clear();
        positionAsset.getItems().clear();
        snapshotPosition.getItems().clear();
    }

    private String nullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private String status(boolean active) {
        return active ? "Actif" : "Inactif";
    }

    private record BaseData(List<AccountDto> accounts, List<AssetDto> assets) {
    }

    private record WorkspaceData(List<AccountDto> accounts, List<AssetDto> assets, List<PositionDto> positions) {
        static WorkspaceData empty() {
            return new WorkspaceData(List.of(), List.of(), List.of());
        }

        Map<UUID, AccountDto> accountsById() {
            Map<UUID, AccountDto> map = new LinkedHashMap<>();
            for (AccountDto account : accounts) {
                map.put(account.id(), account);
            }
            return map;
        }

        Map<UUID, AssetDto> assetsById() {
            Map<UUID, AssetDto> map = new LinkedHashMap<>();
            for (AssetDto asset : assets) {
                map.put(asset.id(), asset);
            }
            return map;
        }
    }

    public static final class AssetRow {
        private final AssetDto asset;
        private final String name;
        private final String type;
        private final String currency;
        private final String status;

        private AssetRow(AssetDto asset, String name, String type, String currency, String status) {
            this.asset = asset;
            this.name = name;
            this.type = type;
            this.currency = currency;
            this.status = status;
        }

        static AssetRow from(AssetDto asset) {
            return new AssetRow(asset, asset.name(), DesktopFormatters.assetType(asset.type()), asset.currencyCode(), asset.active() ? "Actif" : "Inactif");
        }

        AssetDto asset() {
            return asset;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public String getCurrency() {
            return currency;
        }

        public String getStatus() {
            return status;
        }
    }

    public static final class PositionRow {
        private final PositionDto position;
        private final String account;
        private final String asset;
        private final String type;
        private final String quantity;
        private final String status;

        private PositionRow(PositionDto position, String account, String asset, String type, String quantity, String status) {
            this.position = position;
            this.account = account;
            this.asset = asset;
            this.type = type;
            this.quantity = quantity;
            this.status = status;
        }

        static PositionRow from(PositionDto position, Map<UUID, AccountDto> accounts, Map<UUID, AssetDto> assets) {
            AccountDto account = accounts.get(position.accountId());
            AssetDto asset = assets.get(position.assetId());
            return new PositionRow(
                    position,
                    account == null ? position.accountId().toString() : account.name(),
                    asset == null ? position.assetId().toString() : asset.name(),
                    asset == null ? "—" : DesktopFormatters.assetType(asset.type()),
                    DesktopFormatters.decimal(position.quantity()),
                    position.active() ? "Actif" : "Inactif"
            );
        }

        PositionDto position() {
            return position;
        }

        public String getAccount() {
            return account;
        }

        public String getAsset() {
            return asset;
        }

        public String getType() {
            return type;
        }

        public String getQuantity() {
            return quantity;
        }

        public String getStatus() {
            return status;
        }
    }

    public static final class SnapshotRow {
        private final String date;
        private final String quantity;
        private final String marketValue;
        private final String confidence;

        private SnapshotRow(String date, String quantity, String marketValue, String confidence) {
            this.date = date;
            this.quantity = quantity;
            this.marketValue = marketValue;
            this.confidence = confidence;
        }

        static SnapshotRow from(PositionSnapshotDto snapshot) {
            return new SnapshotRow(
                    DesktopFormatters.date(snapshot.valueDate()),
                    DesktopFormatters.decimal(snapshot.quantity()),
                    DesktopFormatters.money(snapshot.marketValue()),
                    snapshot.confidence()
            );
        }

        public String getDate() {
            return date;
        }

        public String getQuantity() {
            return quantity;
        }

        public String getMarketValue() {
            return marketValue;
        }

        public String getConfidence() {
            return confidence;
        }
    }

    private record PositionChoice(PositionDto position, String label) {
        static PositionChoice from(PositionDto position, Map<UUID, AccountDto> accounts, Map<UUID, AssetDto> assets) {
            AccountDto account = accounts.get(position.accountId());
            AssetDto asset = assets.get(position.assetId());
            String accountName = account == null ? "Compte inconnu" : account.name();
            String assetName = asset == null ? "Actif inconnu" : asset.name();
            return new PositionChoice(position, accountName + " / " + assetName);
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
