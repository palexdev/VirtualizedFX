/*
 * Copyright (C) 2024 Parisi Alessandro - alessandro.parisi406@gmail.com
 * This file is part of VirtualizedFX (https://github.com/palexdev/VirtualizedFX)
 *
 * VirtualizedFX is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * VirtualizedFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VirtualizedFX. If not, see <http://www.gnu.org/licenses/>.
 */

package app;

import java.util.function.Supplier;

import interactive.grid.GridTestUtils.Grid;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.events.WhenEvent;
import io.github.palexdev.mfxcore.utils.GridUtils;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.mfxcore.utils.fx.ColorUtils;
import io.github.palexdev.mfxcore.utils.fx.ScrollUtils;
import io.github.palexdev.virtualizedfx.cells.VFXLabeledCellSkin;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import src.cells.TestGridCell;
import src.utils.MouseHoldHandler;
import src.utils.NodeMover;

import static src.utils.Utils.isTouchSupported;
import static src.utils.Utils.items;

public class GridVisualizer extends Application {
    private final ObservableList<Integer> items = items(500);
    private final ObservableSet<Integer> linears = FXCollections.observableSet();
    private final HBox pane = new HBox(600);

    @Override
    public void start(Stage stage) {
        pane.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        pane.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane root = new StackPane(pane);

        if (isTouchSupported()) {
            WhenEvent.intercept(pane, ZoomEvent.ZOOM)
                .process(e -> {
                    pane.setScaleX(e.getTotalZoomFactor());
                    pane.setScaleY(e.getTotalZoomFactor());
                })
                .asFilter()
                .register();
        } else {
            System.err.println("Touch not supported!");
            WhenEvent.intercept(pane, ScrollEvent.SCROLL)
                .process(e -> {
                    ScrollUtils.ScrollDirection sd = ScrollUtils.determineScrollDirection(e);
                    double factor = switch (sd) {
                        case UP -> 0.25;
                        case DOWN -> -0.25;
                        default -> 0.0;
                    };
                    double scale = NumberUtils.clamp(pane.getScaleX() + factor, 0.25, 2);
                    pane.setScaleX(scale);
                    pane.setScaleY(scale);
                })
                .asFilter()
                .register();
        }
        NodeMover.install(pane);
        MouseHoldHandler.install(pane)
            .setAction((e, n) -> {
                if (e.getButton() != MouseButton.SECONDARY) return;
                n.setTranslateX(0);
                n.setTranslateY(0);
            });

        /*
         * The first grid should be used to display the old state, the second to visualize the new state.
         * 'initLinears(...)' builds a list of linear indexes. Cells that are in common between the two grids will be
         * highlighted in green, otherwise in red.
         * Linear indexes should be built from the parameters of the old state. This way you can compare the new grid
         * against the old one and count exactly how many cells need to be updated/created.
         * Last note: the created grids are sized to always show all the rows and columns specified by the given ranges,
         * however, when comparing, keep in mind that you need to watch the cells in the given ranges to count right!
         */

        IntegerRange oldR = IntegerRange.of(0, 5);
        IntegerRange oldC = IntegerRange.of(0, 5);
        int oldN = 10;

        initLinears(oldR, oldC, oldN);
        drawGrid(oldR, oldC, oldN);
        drawGrid(IntegerRange.of(0, 5), IntegerRange.of(0, 5), 11);

        Scene scene = new Scene(root, 1024, 720);
        stage.setScene(scene);
        stage.show();
        //debugView(null, root);
    }

    Grid drawGrid(IntegerRange rows, IntegerRange columns, int nColumns) {
        Grid grid = new Grid(items, ComparisonCell::new);
        int nRows = rows.diff() + 1;
        grid.setColumnsNum(nColumns);
        grid.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        grid.setPrefSize(
            Math.max(1, nColumns - 2) * 100,
            Math.max(1, nRows - 2) * 100
        );
        grid.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        grid.scrollToRow(rows.getMin() + 1);
        grid.scrollToColumn(columns.getMin() + 1);
        pane.getChildren().add(grid);
        return grid;
    }

    void initLinears(IntegerRange rows, IntegerRange columns, int nColumns) {
        for (Integer rIdx : rows) {
            for (Integer cIdx : columns) {
                int linear = GridUtils.subToInd(nColumns, rIdx, cIdx);
                linears.add(linear);
            }
        }
    }

    class ComparisonCell extends TestGridCell {
        public ComparisonCell(Integer item) {
            super(item);
        }

        @Override
        public void updateItem(Integer item) {
            super.updateItem(item);

            Color color = linears.contains(item) ? Color.GREEN : Color.RED;
            setStyle("-fx-background-color: " + ColorUtils.rgb(color));
        }

        @Override
        public Supplier<SkinBase<?, ?>> defaultSkinProvider() {
            return () -> new VFXLabeledCellSkin<>(this) {
                {
                    setStyle("-fx-border-color: blue");
                }
            };
        }
    }
}
