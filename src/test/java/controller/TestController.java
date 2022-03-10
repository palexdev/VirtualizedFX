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
import io.github.palexdev.materialfx.controls.MFXIconWrapper;
import io.github.palexdev.materialfx.font.MFXFontIcon;
import io.github.palexdev.materialfx.utils.ColorUtils;
import io.github.palexdev.virtualizedfx.cell.Cell;
import io.github.palexdev.virtualizedfx.flow.simple.SimpleVirtualFlow;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.IntStream;

public class TestController implements Initializable {
    private final ObservableList<Label> labels = FXCollections.observableArrayList();
    private SimpleVirtualFlow<Label, Cell<Label>> virtualFlow;

    @FXML
    private Group group;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        IntStream.rangeClosed(0, 10_000).forEach(i -> labels.add(createLabel("Label " + i)));
        virtualFlow = new SimpleVirtualFlow<>(
                labels,
                LabelCell::new,
                Orientation.VERTICAL
        );
        virtualFlow.setPrefSize(250, 450);
        virtualFlow.setMaxWidth(100);
        virtualFlow.setFitToWidth(false);
        virtualFlow.setFitToHeight(false);
        virtualFlow.features().enableBounceEffect();
        virtualFlow.features().enableSmoothScrolling(1, 7);

        group.getChildren().setAll(virtualFlow);
    }

    private Label createLabel(String text) {
        FontIcon icon = new FontIcon(getRandomIcon());
        icon.setIconColor(Color.PURPLE);
        icon.setIconSize(14);

        Label label = new Label(text);
        label.setStyle("-fx-background-color: transparent");
        label.setGraphic(icon);
        label.setGraphicTextGap(10);
        return label;
    }

    private String getRandomIcon() {
        FontAwesomeSolid[] resources = FontAwesomeSolid.values();
        int random = (int) (Math.random() * resources.length);
        return resources[random].getDescription();
    }

    @FXML
    void add() {
        virtualFlow.getItems().add(0, createLabel("New String at 0"));
    }

    @FXML
    void addAll() {
        virtualFlow.getItems().addAll(
                2,
                List.of(
                        createLabel("Add All At 2"),
                        createLabel("Add All At 2"),
                        createLabel("Add All At 2")
                )
        );
    }

    @FXML
    void changeFactory() {
        virtualFlow.setCellFactory(LabelCell2::new);
    }

    @FXML
    void changeOrientation() {
        virtualFlow.setOrientation(
                virtualFlow.getOrientation() == Orientation.VERTICAL ?
                        Orientation.HORIZONTAL :
                        Orientation.VERTICAL
        );
    }

    @FXML
    void clear() {
        virtualFlow.getItems().clear();
    }

    @FXML
    void deleteMiddle() {
        virtualFlow.getItems().remove(2);
    }

    @FXML
    void deleteSparse() {
        virtualFlow.getItems().removeAll(
                labels.get(2),
                labels.get(5),
                labels.get(6),
                labels.get(8)
        );
    }

    @FXML
    void deleteLast() {
        virtualFlow.getItems().remove(
                virtualFlow.getItems().size() - 1
        );
    }

    @FXML
    void replace() {
        ObservableList<Label> labels = FXCollections.observableArrayList();
        IntStream.rangeClosed(0, 100).forEach(i -> labels.add(createLabel("NewList " + i)));
        virtualFlow.setItems(labels);
    }

    @FXML
    void scrollByPixel() {
        virtualFlow.scrollBy(20);
    }

    @FXML
    void scrollToFirst() {
        virtualFlow.scrollToFirst();
    }

    @FXML
    void scrollToIndex() {
        virtualFlow.scrollTo(70);
    }

    @FXML
    void scrollToLast() {
        virtualFlow.scrollToLast();
    }

    @FXML
    void scrollToPixel() {
        virtualFlow.scrollToPixel(20);
    }

    @FXML
    void setAll() {
        ObservableList<Label> labels = FXCollections.observableArrayList();
        IntStream.rangeClosed(0, 100).forEach(i -> labels.add(createLabel("NewStrings " + i)));
        virtualFlow.getItems().setAll(labels);
    }

    @FXML
    void setHeight() {
        double val = virtualFlow.getOrientation() == Orientation.VERTICAL ? 450 : 100;
        virtualFlow.setPrefHeight(val);
    }

    @FXML
    void setWidth() {
        double val = virtualFlow.getOrientation() == Orientation.HORIZONTAL ? 600 : 250;
        virtualFlow.setPrefWidth(val);
    }

    @FXML
    void updateInside() {
        virtualFlow.getItems().set(5, createLabel("5 Changed"));
    }

    @FXML
    void updateOutside() {
        virtualFlow.getItems().set(80, createLabel("80 Changed"));
    }

    private class LabelCell extends HBox implements Cell<Label> {
        private Label data;
        private int index;

        public LabelCell(Label data) {
            this.data = data;
            setPrefSize(200, 32);
            setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
            setAlignment(Pos.CENTER_LEFT);
            render(data);
        }

        private void render(Label data) {
            MFXIconWrapper randomIcon = new MFXIconWrapper(new MFXFontIcon("mfx-google", 12, ColorUtils.getRandomColor()), 24);
            Insets insets = virtualFlow.getOrientation() == Orientation.VERTICAL ?
                    new Insets(0, 12, 0, 100) :
                    new Insets(100, 0, 12, 0);
            HBox.setMargin(randomIcon, insets);
            getChildren().setAll(data, randomIcon);
        }

        @Override
        public Node getNode() {
            return this;
        }

        @Override
        public void updateItem(Label item) {
            this.data = item;
            render(data);
        }

        @Override
        public void updateIndex(int index) {
            this.index = index;
        }
    }

    private static class LabelCell2 extends HBox implements Cell<Label> {
        private final MFXCheckbox checkbox = new MFXCheckbox("");
        private Label data;
        private int index;

        public LabelCell2(Label data) {
            this.data = data;
            setPrefSize(200, 32);
            setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
            checkbox.selectedProperty().addListener((observable, oldValue, newValue) -> System.out.println(newValue));
            render(data);
        }

        private void render(Label data) {
            getChildren().setAll(checkbox, data);
        }

        @Override
        public Node getNode() {
            return this;
        }

        @Override
        public void updateItem(Label item) {
            this.data = item;
            render(data);
        }

        @Override
        public void updateIndex(int index) {
            this.index = index;
        }
    }

    private static class StringCell2 extends HBox implements Cell<String> {
        private final MFXCheckbox checkbox = new MFXCheckbox("");
        private final Label label = new Label();
        private int index;

        public StringCell2(String data) {
            label.setAlignment(Pos.CENTER_LEFT);
            label.setText(data);

            setPrefSize(200, 32);
            setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
            checkbox.selectedProperty().addListener((observable, oldValue, newValue) -> System.out.println(newValue));
            render();
        }

        private void render() {
            getChildren().setAll(checkbox, label);
        }

        @Override
        public Node getNode() {
            return this;
        }

        @Override
        public void updateItem(String item) {
            label.setText(item);
            render();
        }

        @Override
        public void updateIndex(int index) {
            this.index = index;
        }
    }

    private static class DebugCell extends HBox implements Cell<String> {
        private final StringProperty text = new SimpleStringProperty();
        private final IntegerProperty index = new SimpleIntegerProperty();

        public DebugCell(String text) {
            Label label = new Label();
            label.setAlignment(Pos.CENTER_LEFT);

            setPrefSize(200, 32);
            setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
            getChildren().setAll(label);

            label.textProperty().bind(Bindings.createStringBinding(
                    () -> getText() + " " + getIndex() + " " + getLayoutY(),
                    textProperty(), indexProperty(), layoutYProperty()
            ));
            setText(text);
        }

        @Override
        public Node getNode() {
            return this;
        }

        @Override
        public void updateItem(String item) {
            setText(item);
        }

        @Override
        public void updateIndex(int index) {
            setIndex(index);
        }

        public String getText() {
            return text.get();
        }

        public StringProperty textProperty() {
            return text;
        }

        public void setText(String text) {
            this.text.set(text);
        }

        public int getIndex() {
            return index.get();
        }

        public IntegerProperty indexProperty() {
            return index;
        }

        public void setIndex(int index) {
            this.index.set(index);
        }
    }
}
