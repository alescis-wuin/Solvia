package fr.seynax.solvia.desktop.ui;

import java.util.List;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import fr.seynax.solvia.desktop.api.ApiDtos.AssetTypeValueDto;

public final class DashboardAllocationCard extends SectionCard {

    private final TableView<AllocationRow> table = new TableView<>();

    public DashboardAllocationCard() {
        super("Allocation", "Repartition par classe d'actifs.");
        table.setAccessibleText("Tableau de repartition du patrimoine par classe d'actifs.");
        TableColumn<AllocationRow, String> type = new TableColumn<>("Classe d'actifs");
        type.setCellValueFactory(new PropertyValueFactory<>("assetType"));
        TableColumn<AllocationRow, String> value = new TableColumn<>("Valeur");
        value.setCellValueFactory(new PropertyValueFactory<>("value"));
        TableColumn<AllocationRow, String> share = new TableColumn<>("Part");
        share.setCellValueFactory(new PropertyValueFactory<>("share"));
        table.getColumns().setAll(type, value, share);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPrefHeight(180);
        getChildren().add(table);
    }

    public void update(List<AssetTypeValueDto> allocation) {
        table.getItems().setAll(allocation.stream().map(AllocationRow::from).toList());
    }

    public void clear() {
        table.getItems().clear();
    }

    public static final class AllocationRow {
        private final String assetType;
        private final String value;
        private final String share;

        private AllocationRow(String assetType, String value, String share) {
            this.assetType = assetType;
            this.value = value;
            this.share = share;
        }

        static AllocationRow from(AssetTypeValueDto value) {
            return new AllocationRow(value.assetType(), DesktopFormatters.money(value.value()), DesktopFormatters.percent(value.allocation()));
        }

        public String getAssetType() {
            return assetType;
        }

        public String getValue() {
            return value;
        }

        public String getShare() {
            return share;
        }
    }
}
