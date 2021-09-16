/*
 * Copyright (C) 2021 Parisi Alessandro
 * This file is part of VirtualizedFX (https://github.com/palexdev/VirtualizedFX).
 *
 * VirtualizedFX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VirtualizedFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VirtualizedFX.  If not, see <http://www.gnu.org/licenses/>.
 */

package controller;

import io.github.palexdev.materialfx.controls.MFXCheckbox;
import io.github.palexdev.materialfx.controls.MFXLabel;
import io.github.palexdev.virtualizedfx.cell.Cell;
import io.github.palexdev.virtualizedfx.flow.simple.SimpleVirtualFlow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.IntStream;

public class TestController implements Initializable {
    private final ObservableList<MFXLabel> labels = FXCollections.observableArrayList();
    private SimpleVirtualFlow<MFXLabel, Cell<MFXLabel>> virtualFlow;

    @FXML
    private Group group;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        IntStream.rangeClosed(0, 10_000).forEach(i -> labels.add(createLabel("Label " + i, getRandomIcon())));
        virtualFlow = SimpleVirtualFlow.Builder.create(
                labels,
                LabelCell::new,
                Orientation.VERTICAL
        );
        virtualFlow.setPrefSize(500, 100);

        group.getChildren().setAll(virtualFlow);
    }

    private MFXLabel createLabel(String text, String iconDescription) {
        FontIcon icon = new FontIcon(iconDescription);
        icon.setIconColor(Color.PURPLE);
        icon.setIconSize(14);

        MFXLabel label = new MFXLabel(text);
        label.setLineColor(Color.TRANSPARENT);
        label.setUnfocusedLineColor(Color.TRANSPARENT);
        label.setStyle("-fx-background-color: transparent");
        label.setLeadingIcon(icon);
        label.setGraphicTextGap(10);
        return label;
    }

    private String getRandomIcon() {
        FontAwesomeSolid[] resources = FontAwesomeSolid.values();
        int random = (int) (Math.random() * resources.length);
        return resources[random].getDescription();
    }

    @FXML
    void add(ActionEvent event) {
        virtualFlow.getItems().add(0, createLabel("New Label at 0", getRandomIcon()));
    }

    @FXML
    void addAll(ActionEvent event) {
        virtualFlow.getItems().addAll(
                2,
                List.of(
                        createLabel("Add All At 2", getRandomIcon()),
                        createLabel("Add All At 2", getRandomIcon()),
                        createLabel("Add All At 2", getRandomIcon())
                )
        );
    }

    @FXML
    void changeFactory(ActionEvent event) {
        virtualFlow.setCellFactory(LabelCell2::new);
    }

    @FXML
    void changeOrientation(ActionEvent event) {
        virtualFlow.setOrientation(
                virtualFlow.getOrientation() == Orientation.VERTICAL ?
                        Orientation.HORIZONTAL :
                        Orientation.VERTICAL
        );
    }

    @FXML
    void clear(ActionEvent event) {
        virtualFlow.getItems().clear();
    }

    @FXML
    void deleteMiddle(ActionEvent event) {
        virtualFlow.getItems().remove(2);
    }

    @FXML
    void deleteSparse(ActionEvent event) {
        virtualFlow.getItems().removeAll(
                labels.get(2),
                labels.get(5),
                labels.get(6),
                labels.get(8)
        );
    }

    @FXML
    void replace(ActionEvent event) {
        ObservableList<MFXLabel> newLabels = FXCollections.observableArrayList();
        IntStream.rangeClosed(0, 100).forEach(i -> newLabels.add(createLabel("NewList " + i, getRandomIcon())));
        virtualFlow.setItems(newLabels);
    }

    @FXML
    void scrollByPixel(ActionEvent event) {
        virtualFlow.scrollBy(20);
    }

    @FXML
    void scrollToFirst(ActionEvent event) {
        virtualFlow.scrollToFirst();
    }

    @FXML
    void scrollToIndex(ActionEvent event) {
        virtualFlow.scrollTo(70);
    }

    @FXML
    void scrollToLast(ActionEvent event) {
        virtualFlow.scrollToLast();
    }

    @FXML
    void scrollToPixel(ActionEvent event) {
        virtualFlow.scrollToPixel(20);
    }

    @FXML
    void setAll(ActionEvent event) {
        ObservableList<MFXLabel> newLabels = FXCollections.observableArrayList();
        IntStream.rangeClosed(0, 100).forEach(i -> newLabels.add(createLabel("NewLabels " + i, getRandomIcon())));
        virtualFlow.getItems().setAll(newLabels);
    }

    @FXML
    void setHeight(ActionEvent event) {
        virtualFlow.setPrefHeight(400);
    }

    @FXML
    void setWidth(ActionEvent event) {
        virtualFlow.setPrefWidth(300);
    }

    @FXML
    void updateInside(ActionEvent event) {
        virtualFlow.getItems().set(5, createLabel("5 Changed", getRandomIcon()));
    }

    @FXML
    void updateOutside(ActionEvent event) {
        virtualFlow.getItems().set(80, createLabel("80 Changed", getRandomIcon()));
    }

    private static class LabelCell extends HBox implements Cell<MFXLabel> {
        private MFXLabel data;
        private int index;

        public LabelCell(MFXLabel data) {
            this.data = data;
            setPrefHeight(32);
            setMaxHeight(USE_PREF_SIZE);
            render(data);
        }

        private void render(MFXLabel data) {
            getChildren().setAll(data);
        }

        @Override
        public Node getNode() {
            return this;
        }

        @Override
        public void updateItem(MFXLabel item) {
            this.data = item;
            render(data);
        }

        @Override
        public void updateIndex(int index) {
            this.index = index;
        }
    }

    private static class LabelCell2 extends HBox implements Cell<MFXLabel> {
        private final MFXCheckbox checkbox = new MFXCheckbox("");
        private MFXLabel data;
        private int index;

        public LabelCell2(MFXLabel data) {
            this.data = data;
            setPrefSize(200, 32);
            setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
            checkbox.selectedProperty().addListener((observable, oldValue, newValue) -> System.out.println(newValue));
            render(data);
        }

        private void render(MFXLabel data) {
            getChildren().setAll(checkbox, data);
        }

        @Override
        public Node getNode() {
            return this;
        }

        @Override
        public void updateItem(MFXLabel item) {
            this.data = item;
            render(data);
        }

        @Override
        public void updateIndex(int index) {
            this.index = index;
        }
    }
}
