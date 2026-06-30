package dev.genyo.installer.ui.dialog;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PrismInstanceSelectorDialog {

    public static class InstanceRow {
        final String name;
        boolean selected = false;

        InstanceRow(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final Stage stage;
    private final List<InstanceRow> rows = new ArrayList<>();
    private List<String> outputInstances = null;

    public PrismInstanceSelectorDialog(Window owner, List<String> inputInstances, boolean showFabricMeteorNote) {
        for (String name : inputInstances) {
            rows.add(new InstanceRow(name));
        }

        stage = new Stage();
        stage.setTitle("Select Prism instances");
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setResizable(true);

        ListView<InstanceRow> listView = new ListView<>(FXCollections.observableArrayList(rows));
        listView.setCellFactory(checkBoxCellFactory());
        listView.setPrefHeight(220);

        Button selectAll = new Button("Select All");
        selectAll.setOnAction(e -> {
            for (InstanceRow row : rows) {
                row.selected = true;
            }
            listView.refresh();
        });

        Button deselectAll = new Button("Deselect All");
        deselectAll.setOnAction(e -> {
            for (InstanceRow row : rows) {
                row.selected = false;
            }
            listView.refresh();
        });

        Button ok = new Button("OK");
        ok.setDefaultButton(true);
        ok.setOnAction(e -> {
            outputInstances = new ArrayList<>();
            for (InstanceRow row : rows) {
                if (row.selected) {
                    outputInstances.add(row.name);
                }
            }
            stage.close();
        });

        Button cancel = new Button("Cancel");
        cancel.setCancelButton(true);
        cancel.setOnAction(e -> {
            outputInstances = null;
            stage.close();
        });

        Label fabricMeteorLabel = new Label("Only instances with Fabric + Meteor installed are listed.");
        fabricMeteorLabel.setVisible(showFabricMeteorNote);
        fabricMeteorLabel.setManaged(showFabricMeteorNote);

        HBox topButtons = new HBox(8, selectAll, deselectAll);
        HBox bottomButtons = new HBox(8, ok, cancel);

        VBox root = new VBox(10, fabricMeteorLabel, topButtons, listView, bottomButtons);
        root.setPadding(new Insets(15));

        stage.setScene(new Scene(root, 360, 360));
    }

    public Optional<List<String>> showAndWaitForSelection() {
        stage.showAndWait();
        return Optional.ofNullable(outputInstances);
    }

    private Callback<InstanceRow, ObservableValue<Boolean>> checkedProperty() {
        return row -> {
            SimpleBooleanProperty prop = new SimpleBooleanProperty(row.selected);
            prop.addListener((obs, was, isNow) -> row.selected = isNow);
            return prop;
        };
    }

    private Callback<ListView<InstanceRow>, ListCell<InstanceRow>> checkBoxCellFactory() {
        return CheckBoxListCell.forListView(checkedProperty());
    }

}
