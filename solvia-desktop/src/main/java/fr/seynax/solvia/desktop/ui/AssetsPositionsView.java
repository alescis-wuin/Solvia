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
import fr.seynax.solvia.desktop.api.ApiDtos.MoneyDto;
import fr.seynax.solvia.desktop.api.ApiDtos.PositionCreateDto;
import fr.seynax.solvia.desktop.api.ApiDtos.PositionDto;
import fr.seynax.solvia.desktop.api.ApiDtos.PositionSnapshotCreateDto;
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

    private final TextField assetName = Ui.tooltip(new TextField(), "Nom lisible de l'actif.");
    private final ComboBox<String> assetType = Ui.tooltip(new ComboBox<>(), "Classe de l'actif.");
    private final TextField assetCurrency = Ui.tooltip(new TextField("EUR"), "Devise ISO de cotation ou de reference.");
    private final TextField assetSymbol = Ui.tooltip(new TextField(), "Symbole optionnel, par exemple CW8, AAPL ou BTC.");
    private final Button createAsset = Ui.tooltip(new Button("Créer l'actif"), "Ajoute l'actif au référentiel local.");

    private final ComboBox<AccountDto> positionAccount = Ui.tooltip(new ComboBox<>(), "Compte ou enveloppe qui détient la position.");
    private final ComboBox<AssetDto> positionAsset = Ui.tooltip(new ComboBox<>(), "Actif détenu dans le compte.");
    private final TextField positionQuantity = Ui.tooltip(new TextField("0"), "Quantité initiale détenue.");
    private final Button createPosition = Ui.tooltip(new Button("Créer la position"), "Lie un actif à un compte.");

    private final ComboBox<PositionChoice> snapshotPosition = Ui.tooltip(new ComboBox<>(), "Position à valoriser.");
    private final DatePicker snapshotDate = Ui.tooltip(new DatePicker(LocalDate.now()), "Date de valorisation observée.");
    private final TextField snapshotQuantity = Ui.tooltip(new TextField(), "Quantité observée à cette date.");
    private final TextField snapshotValue = Ui.tooltip(new TextField(), "Valeur de marché totale observée.");
    private final TextField snapshotCurrency = Ui.tooltip(new TextField("EUR"), "Devise ISO de la valeur de marché.");
    private final Button saveSnapshot = Ui.tooltip(new Button("Enregistrer la valorisation"), "Ajoute un snapshot de position.");

    private volatile boolean backendReady;
    private WorkspaceData currentData = WorkspaceData.empty();

    public AssetsPositionsView(SolviaApiClient apiClient) {
        this.apiClient = apiClient;
        getStyleClass().add("content-view");
        setSpacing(18);
        setPadding(new Insets(20));
        configureCombos();
        getChildren().addAll(forms(), tables(), state, empty);
        VBox.setVgrow(positionTable, Priority.ALWAYS);
        setInputsDisabled(true);
        state.show("Vérification", "Vérification du backend local...", "state-info");
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
            empty.show("Backend indisponible", "Les actifs et positions seront disponibles quand le backend local sera connecté.");
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

    private SectionCard assetForm() {
        createAsset.setOnAction(event -> createAsset());
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
        grid.add(createAsset, 1, 4);
        return new SectionCard("Actif", "Crée un actif suivi par Solvia.", grid);
    }

    private SectionCard positionForm() {
        createPosition.setOnAction(event -> createPosition());
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.add(Ui.fieldLabel("Compte", positionAccount), 0, 0);
        grid.add(positionAccount, 1, 0);
        grid.add(Ui.fieldLabel("Actif", positionAsset), 0, 1);
        grid.add(positionAsset, 1, 1);
        grid.add(Ui.fieldLabel("Quantité", positionQuantity), 0, 2);
        grid.add(positionQuantity, 1, 2);
        grid.add(createPosition, 1, 3);
        return new SectionCard("Position", "Associe un actif à un compte ou une enveloppe.", grid);
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
        grid.add(Ui.fieldLabel("Quantité", snapshotQuantity), 0, 2);
        grid.add(snapshotQuantity, 1, 2);
        grid.add(Ui.fieldLabel("Valeur", snapshotValue), 0, 3);
        grid.add(snapshotValue, 1, 3);
        grid.add(Ui.fieldLabel("Devise", snapshotCurrency), 0, 4);
        grid.add(snapshotCurrency, 1, 4);
        grid.add(saveSnapshot, 1, 5);
        return new SectionCard("Valorisation", "Saisis la quantité et la valeur totale observées pour une position.", grid);
    }

    private SectionCard assetsTable() {
        TableColumn<AssetRow, String> name = new TableColumn<>("Actif");
        name.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<AssetRow, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(new PropertyValueFactory<>("type"));
        TableColumn<AssetRow, String> currency = new TableColumn<>("Devise");
        currency.setCellValueFactory(new PropertyValueFactory<>("currency"));
        TableColumn<AssetRow, String> symbol = new TableColumn<>("Symbole");
        symbol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        assetTable.getColumns().setAll(name, type, currency, symbol);
        assetTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        assetTable.setPrefHeight(260);
        return new SectionCard("Actifs suivis", "Référentiel local des actifs.", assetTable);
    }

    private SectionCard positionsTable() {
        TableColumn<PositionRow, String> account = new TableColumn<>("Compte");
        account.setCellValueFactory(new PropertyValueFactory<>("account"));
        TableColumn<PositionRow, String> asset = new TableColumn<>("Actif");
        asset.setCellValueFactory(new PropertyValueFactory<>("asset"));
        TableColumn<PositionRow, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(new PropertyValueFactory<>("type"));
        TableColumn<PositionRow, String> quantity = new TableColumn<>("Quantité");
        quantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        positionTable.getColumns().setAll(account, asset, type, quantity);
        positionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        positionTable.setPrefHeight(260);
        return new SectionCard("Positions", "Actifs détenus par compte.", positionTable);
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
        if (data.accounts().isEmpty()) {
            empty.show("Aucun compte", "Crée un compte avant d'ajouter une position.");
            state.show("Aucun compte", "Les positions nécessitent au moins un compte.", "state-warning");
        } else if (data.assets().isEmpty()) {
            empty.show("Aucun actif", "Crée un actif avant d'ajouter une position.");
            state.show("Aucun actif", "Le référentiel d'actifs est vide.", "state-warning");
        } else {
            empty.hide();
            state.show("Données chargées", "Actifs: " + data.assets().size() + " / Positions: " + data.positions().size(), "state-success");
        }
    }

    private void createAsset() {
        String name = assetName.getText() == null ? "" : assetName.getText().strip();
        if (name.isBlank()) {
            state.show("Nom requis", "Saisir un nom d'actif.", "state-warning");
            return;
        }
        ValidationResult<String> currency = InputValidation.currencyCode(assetCurrency.getText(), "Devise de l'actif");
        if (!currency.valid()) {
            state.show("Devise invalide", currency.message(), "state-warning");
            return;
        }
        assetCurrency.setText(currency.value());
        AssetCreateDto request = new AssetCreateDto(name, assetType.getValue(), currency.value(), nullIfBlank(assetSymbol.getText()));
        createAsset.setDisable(true);
        state.show("Enregistrement", "Création de l'actif...", "state-info");
        apiClient.createAsset(request).whenComplete((asset, error) -> Platform.runLater(() -> {
            createAsset.setDisable(false);
            if (error != null) {
                state.show("Erreur", DesktopFormatters.errorMessage(error), "state-error");
                return;
            }
            assetName.clear();
            assetSymbol.clear();
            state.show("Actif créé", asset.name() + " a été ajouté.", "state-success");
            refresh();
        }));
    }

    private void createPosition() {
        AccountDto account = positionAccount.getValue();
        AssetDto asset = positionAsset.getValue();
        if (account == null || asset == null) {
            state.show("Sélection requise", "Sélectionner un compte et un actif.", "state-warning");
            return;
        }
        ValidationResult<BigDecimal> quantity = InputValidation.amount(positionQuantity.getText(), "Quantité");
        if (!quantity.valid() || quantity.value().signum() < 0) {
            state.show("Quantité invalide", quantity.valid() ? "La quantité doit être positive ou nulle." : quantity.message(), "state-warning");
            return;
        }
        PositionCreateDto request = new PositionCreateDto(account.id(), asset.id(), quantity.value());
        createPosition.setDisable(true);
        state.show("Enregistrement", "Création de la position...", "state-info");
        apiClient.createPosition(request).whenComplete((position, error) -> Platform.runLater(() -> {
            createPosition.setDisable(false);
            if (error != null) {
                state.show("Erreur", DesktopFormatters.errorMessage(error), "state-error");
                return;
            }
            positionQuantity.setText("0");
            state.show("Position créée", "La position a été ajoutée.", "state-success");
            refresh();
        }));
    }

    private void savePositionSnapshot() {
        PositionChoice position = snapshotPosition.getValue();
        if (position == null) {
            state.show("Position requise", "Créer ou sélectionner une position avant la valorisation.", "state-warning");
            return;
        }
        ValidationResult<BigDecimal> quantity = InputValidation.amount(snapshotQuantity.getText(), "Quantité");
        if (!quantity.valid() || quantity.value().signum() < 0) {
            state.show("Quantité invalide", quantity.valid() ? "La quantité doit être positive ou nulle." : quantity.message(), "state-warning");
            return;
        }
        ValidationResult<BigDecimal> value = InputValidation.amount(snapshotValue.getText(), "Valeur de marché");
        if (!value.valid() || value.value().signum() < 0) {
            state.show("Valeur invalide", value.valid() ? "La valeur de marché doit être positive ou nulle." : value.message(), "state-warning");
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
            state.show("Valorisation enregistrée", "Le snapshot de position a été ajouté.", "state-success");
            refresh();
        }));
    }

    private void configureCombos() {
        assetType.getItems().setAll("FIAT_CURRENCY", "STOCK", "ETF", "BOND", "CRYPTO_ASSET", "PRIVATE_EQUITY", "REAL_ESTATE", "CASHBACK_REWARD", "OTHER");
        assetType.setValue("ETF");
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

    private void setInputsDisabled(boolean disabled) {
        assetName.setDisable(disabled);
        assetType.setDisable(disabled);
        assetCurrency.setDisable(disabled);
        assetSymbol.setDisable(disabled);
        createAsset.setDisable(disabled);
        positionAccount.setDisable(disabled);
        positionAsset.setDisable(disabled);
        positionQuantity.setDisable(disabled);
        createPosition.setDisable(disabled);
        snapshotPosition.setDisable(disabled);
        snapshotDate.setDisable(disabled);
        snapshotQuantity.setDisable(disabled);
        snapshotValue.setDisable(disabled);
        snapshotCurrency.setDisable(disabled);
        saveSnapshot.setDisable(disabled);
    }

    private void clearData() {
        currentData = WorkspaceData.empty();
        assetTable.getItems().clear();
        positionTable.getItems().clear();
        positionAccount.getItems().clear();
        positionAsset.getItems().clear();
        snapshotPosition.getItems().clear();
    }

    private String nullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value.strip();
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
        private final String name;
        private final String type;
        private final String currency;
        private final String symbol;

        private AssetRow(String name, String type, String currency, String symbol) {
            this.name = name;
            this.type = type;
            this.currency = currency;
            this.symbol = symbol;
        }

        static AssetRow from(AssetDto asset) {
            return new AssetRow(asset.name(), DesktopFormatters.assetType(asset.type()), asset.currencyCode(), asset.symbol() == null ? "—" : asset.symbol());
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

        public String getSymbol() {
            return symbol;
        }
    }

    public static final class PositionRow {
        private final String account;
        private final String asset;
        private final String type;
        private final String quantity;

        private PositionRow(String account, String asset, String type, String quantity) {
            this.account = account;
            this.asset = asset;
            this.type = type;
            this.quantity = quantity;
        }

        static PositionRow from(PositionDto position, Map<UUID, AccountDto> accounts, Map<UUID, AssetDto> assets) {
            AccountDto account = accounts.get(position.accountId());
            AssetDto asset = assets.get(position.assetId());
            return new PositionRow(
                    account == null ? position.accountId().toString() : account.name(),
                    asset == null ? position.assetId().toString() : asset.name(),
                    asset == null ? "—" : DesktopFormatters.assetType(asset.type()),
                    DesktopFormatters.decimal(position.quantity())
            );
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
