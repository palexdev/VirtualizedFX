/*
 * Copyright (C) 2026 Parisi Alessandro - alessandro.parisi406@gmail.com
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
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;

import static io.github.palexdev.mfxcore.observables.When.onInvalidated;

/// Default skin implementation for [VFXGrid], extends [MFXSkinBase].
///
/// The layout is quite simple: there is just one node, called the 'viewport', that is the `Pane` responsible for
/// containing and laying out the cells. Needless to say, the layout strategy is custom, and it's defined in the
/// [#layout()] method.
///
/// Compared to other virtualized components' skin, this implements a rather unique feature. It allows you, by setting the
/// [VFXGrid#alignmentProperty()], to change the x and y coordinates of the viewport node. This is especially useful
/// if you want the content to be centered and in combination with [VFXGrid#autoArrange(int)] (think about a gallery, for example).
///
/// The skin reacts to changes in the grid, see [#install()]. A new state updates the viewport's children, and a layout
/// request lays out the cells, [#layout()].
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

        getChildren().setAll(viewport);
    }

    //================================================================================
    // Methods
    //================================================================================

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

    /// Registers the skin's listeners:
    ///
    /// - on [VFXGrid#stateProperty()], updates the viewport's children. They are cleared if the state is
    ///   [VFXGridState#INVALID], otherwise they are set to the state's cells and a layout is requested, only if
    ///   [VFXGridState#haveCellsChanged()]
    ///
    /// - on [VFXGrid#needsViewportLayoutProperty()], calls [#layout()] when a layout is requested
    ///
    /// - on [VFXGrid#helperProperty()], binds the viewport's translate properties to the helper's
    ///   [VFXGridHelper#viewportPositionProperty()]. By translating the viewport, we give the illusion of scrolling
    ///   (virtual scrolling). This one also runs immediately, since the grid always has a helper
    ///
    /// - on [VFXGrid#alignmentProperty()], requests a layout of the grid, [Parent#requestLayout()], so that the
    ///   viewport is aligned again, see [#layoutChildren(double,double,double,double)]
    @Override
    public void install() {
        VFXGrid<T, C> grid = getSkinnable();
        listen(
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
            onInvalidated(grid.alignmentProperty())
                .then(a -> grid.requestLayout())
        );
    }

    /// @return the left and right insets plus [#DEFAULT_SIZE]
    @Override
    protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return leftInset + DEFAULT_SIZE + rightInset;
    }

    /// @return the top and bottom insets plus [#DEFAULT_SIZE]
    @Override
    protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return topInset + DEFAULT_SIZE + bottomInset;
    }

    /// {@inheritDoc}
    ///
    /// Sizes the viewport to fit the cells of the state's ranges of rows and columns (spacing between them included),
    /// and positions it according to the [VFXGrid#alignmentProperty()], never at negative coordinates. With
    /// [VFXGridState#INVALID], the viewport is sized to 0.
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

    /// {@inheritDoc}
    ///
    /// Also sets the grid's state to [VFXGridState#INVALID].
    @SuppressWarnings("unchecked")
    @Override
    public void dispose() {
        VFXGrid<T, C> grid = getSkinnable();
        grid.update(VFXGridState.INVALID);
        super.dispose();
    }
}
