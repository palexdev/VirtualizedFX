/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit5TestClass.java to edit this template
 */
package io.github.palexdev.virtualizedfx.table;

import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.assertions.api.Assertions;

import io.github.palexdev.virtualizedfx.controls.VirtualScrollPane;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums;
import io.github.palexdev.virtualizedfx.table.TableColumn;
import io.github.palexdev.virtualizedfx.table.TableRow;
import io.github.palexdev.virtualizedfx.table.VirtualTable;
import io.github.palexdev.virtualizedfx.table.defaults.DefaultTableColumn;
import io.github.palexdev.virtualizedfx.table.defaults.DefaultTableRow;
import io.github.palexdev.virtualizedfx.table.defaults.SimpleTableCell;
import io.github.palexdev.virtualizedfx.utils.VSPUtils;

import javafx.collections.ObservableList;
import javafx.geometry.VerticalDirection;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import io.github.palexdev.materialfx.font.MFXFontIcon;
import io.github.palexdev.mfxcore.controls.MFXIconWrapper;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.fx.RegionUtils;
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
import javafx.util.StringConverter;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;


import java.util.Optional;
import java.util.function.Function;
import javafx.application.Platform;

/**
 *
 */
public class TableManagerTest extends ApplicationTest {

    protected VirtualTable table;

    @Override
    public void start(Stage stage) throws Exception {
        BorderPane mainWindow = new BorderPane();
        stage.setScene(new Scene(mainWindow));

        table = new VirtualTable<>();
        DefaultTableColumn<Model, TableCell<Model>> first = createColumn(table, "first", Model::firstName);
        DefaultTableColumn<Model, TableCell<Model>> last = createColumn(table, "last", Model::lastName);
        DefaultTableColumn<Model, TableCell<Model>> email = createColumn(table, "email", Model::email);

        table.getColumns().setAll(first, last, email);

        // Init Content Pane
        VirtualScrollPane vsp = table.wrap();
        vsp.setLayoutMode(ScrollPaneEnums.LayoutMode.COMPACT);
        vsp.setAutoHideBars(true);

        mainWindow.setCenter(vsp);

        stage.show();

    }

    @Test
    public void empty_table_shows_columns() {
        Assertions.assertThat(lookup(".columns-container").queryAllAs(DefaultTableColumn.class)).isNotEmpty();
        Assertions.assertThat(lookup(".columns-container").lookup("first").queryAll()).isNotEmpty();
        Assertions.assertThat(lookup(".columns-container").lookup("last").queryAll()).isNotEmpty();
        Assertions.assertThat(lookup(".columns-container").lookup("email").queryAll()).isNotEmpty();
    }

    private <E> DefaultTableColumn<Model, TableCell<Model>> createColumn(VirtualTable<Model> table, String name, Function<Model, E> extractor) {
        DefaultTableColumn<Model, TableCell<Model>> column = new DefaultTableColumn<>(table, name);

        column.setCellFactory(u -> new AltCell<>(u, extractor));

        return column;
    }

    // ------------------------------------------------------------------- Model
    private class Model {

        public String firstName, lastName, email;

        public Model(final String first, final String last, final String email) {
            this.firstName = first;
            this.lastName = last;
            this.email = email;
        }

        public String firstName() {
            return firstName;
        }

        public String lastName() {
            return lastName;
        }

        public String email() {
            return email;
        }
    }

    public class AltCell<E> extends SimpleTableCell<Model, E> {

        public AltCell(Model item, Function<Model, E> extractor) {
            super(item, extractor);
        }

        public AltCell(Model item, Function<Model, E> extractor, StringConverter<E> converter) {
            super(item, extractor, converter);
        }

        @Override
        protected void init() {
            super.init();
            addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
                if (e.getButton() == MouseButton.SECONDARY) {
                    TableColumn<Model, ? extends TableCell<Model>> column = getColumn();
                    VirtualTable<Model> table = column.getTable();
                    if (table.getColumnsLayoutMode() == ColumnsLayoutMode.FIXED) {
                        return;
                    }
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
        public void updateRow(int rIndex, DefaultTableRow<Model> row) {
            super.updateRow(rIndex, row);
            invalidate();
        }

        @Override
        public void updateItem(Model item) {
            super.updateItem(item);
        }

        @Override
        public void invalidate() {
            if (getExtractor() == null) {
                label.setText(Optional.ofNullable(getRow()).map(TableRow::getIndex).orElse(-1).toString());
                return;
            }

            E e = getExtractor().apply(getItem());
            Platform.runLater(() -> {
                label.setText(getConverter().toString(e));
            });
        }
    }
}
