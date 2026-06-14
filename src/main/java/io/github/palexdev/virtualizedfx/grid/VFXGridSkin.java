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

package io.github.palexdev.virtualizedfx.grid;

import java.util.SequencedMap;

import io.github.palexdev.mfxcore.base.beans.Position;
import io.github.palexdev.mfxcore.controls.MFXSkinBase;
import io.github.palexdev.mfxcore.utils.GridUtils;
import io.github.palexdev.mfxcore.utils.fx.LayoutUtils;
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import javafx.beans.InvalidationListener;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;

import static io.github.palexdev.mfxcore.observables.OnInvalidated.withListener;
import static io.github.palexdev.mfxcore.observables.When.onInvalidated;

/// Default skin implementation for [VFXGrid], extends [MFXSkinBase] and expects behaviors of type
/// [VFXGridManager].
///
/// The layout is quite simple: there is just one node, called the 'viewport', that is the `Pane` resposnible for
/// containing and laying out the cells. Needless to say, the layout strategy is custom, and it's defined in the
/// [#layout()] method.
///
/// Compared to other virtualized components' skin, this implements a rather unique feature. It allows you, by setting the
/// [VFXGrid#alignmentProperty()], to change the x and y coordinates of the viewport node. This is especially useful
/// if you want the content to be centered and in combination with [VFXGrid#autoArrange(int)] (think about a gallery, for example).
///
/// As all skins typically do, this is also responsible for catching any change in the component's properties.
/// The computation that leads to a new state is delegated to the controller/behavior, which is the [VFXGridManager].
/// Read this [#addListeners()] to check which changes are handled.
///
/// Last but not least, by design, this skin makes the component always be at least 100px tall and wide. You can change this
/// by overriding the [#DEFAULT_SIZE] variable.
public class VFXGridSkin<T, C extends VFXCell<T>> extends MFXSkinBase<VFXGrid<T, C>> {
    //================================================================================
    // Properties
    //================================================================================
    protected final Pane viewport;
    protected double DEFAULT_SIZE = 100.0;

    //================================================================================
    // Constructors
    //================================================================================
    public VFXGridSkin(VFXGrid<T, C> grid) {
        super(grid);

        // Init viewport
        viewport = new Pane() {
            @Override
            protected void layoutChildren() {
                VFXGridSkin.this.layout();
            }
        };
        viewport.getStyleClass().add("viewport");

        // End initialization
        addListeners();
        getChildren().setAll(viewport);
    }

    //================================================================================
    // Methods
    //================================================================================

    /// Adds listeners on the component's properties which need to produce a new [VFXGridState] upon changing.
    ///
    /// Here's the list:
    ///
    /// - Listener on [VFXGrid#stateProperty()], this is crucial to update the viewport's children and
    /// invoke [VFXGrid#requestViewportLayout()] if [VFXGridState#haveCellsChanged()] is `true`
    ///
    /// - Listener on [VFXGrid#needsViewportLayoutProperty()], this is crucial because invokes [#layout()]
    ///
    /// - Listener on [VFXGrid#helperProperty()], this is crucial because it's responsible for binding the
    /// viewport's translate properties to the [VFXGridHelper#viewportPositionProperty()]
    /// By translating the viewport, we give the illusion of scrolling (virtual scrolling)
    ///
    /// - Listener on [VFXGrid#widthProperty()], will invoke [VFXGridManager#onGeometryChanged()]
    ///
    /// - Listener on [VFXGrid#helperProperty()], will invoke [VFXGridManager#onGeometryChanged()]
    ///
    /// - Listener on [VFXGrid#bufferSizeProperty()], will invoke [VFXGridManager#onGeometryChanged()].
    /// Yes, it is enough to threat this change as a geometry change to avoid code duplication
    ///
    /// -Listener on [VFXGrid#vPosProperty()], will invoke [VFXGridManager#onPositionChanged(Orientation)]
    /// with [Orientation#VERTICAL] as parameter
    ///
    /// -Listener on [VFXGrid#hPosProperty()], will invoke [VFXGridManager#onPositionChanged(Orientation)]
    /// with [Orientation#HORIZONTAL] as parameter
    ///
    /// - Listener on [VFXGrid#columnsNumProperty()], will invoke [VFXGridManager#onColumnsNumChanged()]
    ///
    /// - Listener on [VFXGrid#getCellFactory()], will invoke [VFXGridManager#onCellFactoryChanged()]
    ///
    /// - Listener on [VFXGrid#cellSizeProperty()], will invoke [VFXGridManager#onCellSizeChanged()]
    ///
    /// - Listener on [VFXGrid#vSpacingProperty()], will invoke [VFXGridManager#onSpacingChanged()]
    ///
    /// - Listener on [VFXGrid#hSpacingProperty()], will invoke [VFXGridManager#onSpacingChanged()]
    ///
    /// - Listener on [VFXGrid#itemsProperty()], will invoke [VFXGridManager#onItemsChanged()]
    ///
    /// - Listener on [VFXGrid#alignmentProperty()], will invoke [Parent#requestLayout()]
    protected void addListeners() {
        VFXGrid<T, C> grid = getSkinnable();

        InvalidationListener gcl = i -> getBehavior().onGeometryChanged();
        listeners(
            // Core changes
            onInvalidated(grid.stateProperty())
                .then(s -> {
                    if (s == VFXGridState.INVALID) {
                        viewport.getChildren().clear();
                    } else if (s.haveCellsChanged()) {
                        viewport.getChildren().setAll(s.getNodes());
                        grid.requestViewportLayout();
                    }
                }),
            onInvalidated(grid.needsViewportLayoutProperty())
                .condition(v -> v)
                .then(v -> layout()),
            onInvalidated(grid.helperProperty())
                .then(h -> {
                    viewport.translateXProperty().bind(h.viewportPositionProperty().map(Position::x));
                    viewport.translateYProperty().bind(h.viewportPositionProperty().map(Position::y));
                })
                .executeNow(),

            // Geometry changes
            withListener(grid.widthProperty(), gcl),
            withListener(grid.heightProperty(), gcl),
            withListener(grid.bufferSizeProperty(), gcl),

            // Position changes
            onInvalidated(grid.vPosProperty())
                .then(v -> getBehavior().onPositionChanged(Orientation.VERTICAL)),
            onInvalidated(grid.hPosProperty())
                .then(h -> getBehavior().onPositionChanged(Orientation.HORIZONTAL)),

            // Others
            onInvalidated(grid.columnsNumProperty())
                .then(n -> getBehavior().onColumnsNumChanged()),
            onInvalidated(grid.getCellFactory())
                .then(f -> getBehavior().onCellFactoryChanged()),
            onInvalidated(grid.cellSizeProperty())
                .then(s -> getBehavior().onCellSizeChanged()),
            onInvalidated(grid.hSpacingProperty())
                .then(s -> getBehavior().onSpacingChanged()),
            onInvalidated(grid.vSpacingProperty())
                .then(s -> getBehavior().onSpacingChanged()),
            onInvalidated(grid.itemsProperty())
                .then(it -> getBehavior().onItemsChanged()),
            onInvalidated(grid.alignmentProperty())
                .then(a -> grid.requestLayout())
        );
    }

    /// Core method responsible for resizing and positioning cells in the viewport.
    /// This method will not execute if the layout was not requested, [VFXGrid#needsViewportLayoutProperty()]
    /// is false, or if the [VFXGrid#stateProperty()] is [VFXGridState#INVALID].
    ///
    /// In any case, at the end of the method, [#onLayoutCompleted(boolean)] will be called.
    ///
    /// Cells are retrieved from the current grid's state, given by the [VFXGrid#stateProperty()].
    /// The iteration over each row and column gives us all the indexes to retrieve the cells from
    /// [VFXGridState#getCellsByIndex()]. For the actual layout, however, we use two counters because the layout
    /// is 'absolute'. Meaning that the row and column indexes are irrelevant for the cell's position, we just care about
    /// which comes before/after, above/below. Make sure to also read [VFXGridState] to understand how indexes are
    /// managed for the [VFXGrid]. In other words, it doesn't matter whether our range is `[1, 5]` or `[4, 6]` or whatever,
    /// the layout index will always start from 0 and increment towards the end of the range.
    ///
    /// The layout is performed by [VFXGridHelper#layout(int, int, VFXCell)], the two aforementioned counters are passed
    /// as arguments.
    /// ```
    /// Little example:
    /// For a rows range of [2, 7] and columns range of [2, 7]
    /// The first cell's coordinates are [2, 2], but its layout coordinates are [0, 0]
    /// The second cell's coordinates are [2, 3], but its layout coordinates are [0, 1]
    /// ...and so on
    ///```
    ///
    /// @see #onLayoutCompleted(boolean)
    protected void layout() {
        VFXGrid<T, C> grid = getSkinnable();
        if (!grid.isNeedsViewportLayout()) return;

        VFXGridHelper<T, C> helper = grid.getHelper();
        VFXGridState<T, C> state = grid.getState();
        int nColumns = helper.maxColumns();
        if (state != VFXGridState.INVALID) {
            SequencedMap<Integer, C> cells = state.getCellsByIndex();
            int i = 0, j = 0;
            outer_loop:
            for (Integer rIdx : state.getRowsRange()) {
                for (Integer cIdx : state.getColumnsRange()) {
                    int linear = GridUtils.subToInd(nColumns, rIdx, cIdx);
                    if (linear < 0 || linear >= grid.size()) break outer_loop;
                    helper.layout(i, j, cells.get(linear));
                    j++;
                }
                i++;
                j = 0;
            }
            onLayoutCompleted(true);
            return;
        }
        onLayoutCompleted(false);
    }

    /// This method is **crucial** because it resets the [VFXGrid#needsViewportLayoutProperty()] to false.
    /// If you override this method or the [#layout()], remember to call this!
    ///
    /// @param done this parameter can be useful to overriders as it gives information on whether the [#layout()]
    /// was executed correctly
    protected void onLayoutCompleted(boolean done) {
        VFXGrid<T, C> grid = getSkinnable();
        grid.setNeedsViewportLayout(false);
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    @Override
    protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return leftInset + DEFAULT_SIZE + rightInset;
    }

    @Override
    protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return topInset + DEFAULT_SIZE + bottomInset;
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        VFXGrid<T, C> grid = getSkinnable();
        VFXGridHelper<T, C> helper = grid.getHelper();
        VFXGridState<T, C> state = grid.getState();
        if (state == VFXGridState.INVALID) {
            viewport.resize(0, 0);
            return;
        }

        double vw = ((state.getColumnsRange().diff() + 1) * helper.getTotalCellSize().width()) - grid.getHSpacing();
        double vh = ((state.getRowsRange().diff() + 1) * helper.getTotalCellSize().height()) - grid.getVSpacing();
        viewport.resize(vw, vh);
        Position pos = LayoutUtils.computePosition(
            grid, viewport,
            0, 0, grid.getWidth(), grid.getHeight(), 0, Insets.EMPTY,
            grid.getAlignment().getHpos(), grid.getAlignment().getVpos(),
            false, false
        );
        viewport.relocate(
            Math.max(0, pos.x()),
            Math.max(0, pos.y())
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public void dispose() {
        VFXGrid<T, C> grid = getSkinnable();
        grid.update(VFXGridState.INVALID);
        super.dispose();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected VFXGridManager<T, C> getBehavior() {
        return (VFXGridManager<T, C>) super.getBehavior();
    }
}
