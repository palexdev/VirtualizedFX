/*
 * Copyright (C) 2022 Parisi Alessandro
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

package app.table;

import app.cells.CellsDebugger;
import app.model.Model;
import app.model.User;
import io.github.palexdev.materialfx.controls.MFXCheckbox;
import io.github.palexdev.materialfx.controls.MFXFilterComboBox;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.fx.RegionUtils;
import io.github.palexdev.mfxresources.fonts.MFXIconWrapper;
import io.github.palexdev.virtualizedfx.cell.TableCell;
import io.github.palexdev.virtualizedfx.controls.VirtualScrollPane;
import io.github.palexdev.virtualizedfx.enums.ColumnsLayoutMode;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums;
import io.github.palexdev.virtualizedfx.table.TableColumn;
import io.github.palexdev.virtualizedfx.table.TableRow;
import io.github.palexdev.virtualizedfx.table.VirtualTable;
import io.github.palexdev.virtualizedfx.table.defaults.DefaultTableColumn;
import io.github.palexdev.virtualizedfx.table.defaults.DefaultTableRow;
import io.github.palexdev.virtualizedfx.table.defaults.SimpleTableCell;
import io.github.palexdev.virtualizedfx.utils.VSPUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;

import java.net.URL;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public class TableTestController implements Initializable {

    @FXML
    StackPane rootPane;
    @FXML
    MFXFilterComboBox<TableTestActions> actionsBox;
    @FXML
    MFXIconWrapper runIcon;
    @FXML
    StackPane contentPane;
    @FXML
    MFXCheckbox ssCheck;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Create Test Parameters
        TableTestParameters<User> parameters = new TableTestParameters<>(rootPane);

        // Table Initialization
        VirtualTable<User> table = new VirtualTable<>(Model.users);

        // Columns Initialization
        DefaultTableColumn<User, TableCell<User>> idxc = createColumn(table, "", null);
        DefaultTableColumn<User, TableCell<User>> didc = createColumn(table, "ID", User::id);
        DefaultTableColumn<User, TableCell<User>> dnc = createColumn(table, "Name", User::name);
        DefaultTableColumn<User, TableCell<User>> dac = createColumn(table, "Age", User::age);
        DefaultTableColumn<User, TableCell<User>> dhc = createColumn(table, "Height", User::height);
        DefaultTableColumn<User, TableCell<User>> gc = createColumn(table, "Gender", User::gender);
        DefaultTableColumn<User, TableCell<User>> nc = createColumn(table, "Nationality", User::nationality);
        DefaultTableColumn<User, TableCell<User>> cpc = createColumn(table, "Phone", User::cellPhone);
        DefaultTableColumn<User, TableCell<User>> uc = createColumn(table, "University", User::university);
        table.getColumns().setAll(idxc, didc, dnc, dac, dhc, gc, nc, cpc, uc);

        // Init Parameters
        parameters.initCellsMap(table, AltCell.class);
        parameters.setColumns(table.getColumns());

        // Init Actions
        actionsBox.setItems(FXCollections.observableArrayList(TableTestActions.values()));
        actionsBox.selectFirst();
        runIcon.setOnMouseClicked(e -> actionsBox.getSelectedItem().run(parameters, table));
        runIcon.enableRippleGenerator(true);
        RegionUtils.makeRegionCircular(runIcon);

        // Init Content Pane
        VirtualScrollPane vsp = table.wrap();
        vsp.setLayoutMode(ScrollPaneEnums.LayoutMode.COMPACT);
        vsp.setAutoHideBars(true);

        Runnable speedAction = () -> {
            double ch = table.getCellHeight();
            double cw = table.getColumnSize().getWidth();
            VSPUtils.setVSpeed(vsp, ch / 3, ch / 2, ch / 2);
            VSPUtils.setHSpeed(vsp, cw / 3, cw / 2, cw / 2);
        };
        When.onInvalidated(table.cellHeightProperty())
                .then(i -> speedAction.run())
                .executeNow()
                .listen();
        When.onInvalidated(table.columnSizeProperty())
                .then(i -> speedAction.run())
                .executeNow()
                .listen();

        contentPane.getChildren().add(vsp);

        // Init Settings
        Consumer<Boolean> ssAction = val -> {
            vsp.setSmoothScroll(true);
            vsp.setTrackSmoothScroll(true);
        };
        ssCheck.selectedProperty().addListener((observable, oldValue, newValue) -> ssAction.accept(newValue));
    }

    private <E> DefaultTableColumn<User, TableCell<User>> createColumn(VirtualTable<User> table, String text, Function<User, E> extractor) {
        DefaultTableColumn<User, TableCell<User>> column = new DefaultTableColumn<>(table, text);
        column.setCellFactory(u -> new AltCell<>(u, extractor));
        column.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY)
                table.getColumns().remove(column);
        });
        return column;
    }

    public static class AltCell<E> extends SimpleTableCell<User, E> {

        public AltCell(User item, Function<User, E> extractor) {
            super(item, extractor);
        }

        public AltCell(User item, Function<User, E> extractor, StringConverter<E> converter) {
            super(item, extractor, converter);
        }

        @Override
        protected void init() {
            super.init();
            addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
                if (e.getButton() == MouseButton.SECONDARY) {
                    TableColumn<User, ? extends TableCell<User>> column = getColumn();
                    VirtualTable<User> table = column.getTable();
                    if (table.getColumnsLayoutMode() == ColumnsLayoutMode.FIXED) return;
                    table.getTableHelper().autosizeColumn(column);
                }
            });
        }

        @Override
        public void updateIndex(int index) {
            super.updateIndex(index);
            invalidate();
        }

        @Override
        public void updateRow(int rIndex, DefaultTableRow<User> row) {
            super.updateRow(rIndex, row);
            invalidate();
        }

        @Override
        public void updateItem(User item) {
            super.updateItem(item);
            //CellsDebugger.randBackground(this, 0.3, getItem().id());
            if (getParent() instanceof Region r) {
                CellsDebugger.animateBackground(r, 250).play();
            }
        }

        @Override
        public void invalidate() {
            if (getExtractor() == null) {
                label.setText(Optional.ofNullable(getRow()).map(TableRow::getIndex).orElse(-1).toString());
                return;
            }

            User item = getItem();
            E e = getExtractor().apply(item);
            String s = getConverter().toString(e);
            label.setText(MessageFormat.format("At:[{1};{2}]: {0}",
                    s, Optional.ofNullable(getRow()).map(TableRow::getIndex).orElse(-1), getIndex())
            );
        }
    }
}
