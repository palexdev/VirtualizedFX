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

package io.github.palexdev.virtualizedfx.list;

import java.util.TreeMap;

import io.github.palexdev.mfxcore.base.beans.Position;
import io.github.palexdev.mfxcore.controls.MFXSkinBase;
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import javafx.scene.layout.Pane;

import static io.github.palexdev.mfxcore.observables.When.onInvalidated;

/// Default skin implementation for [VFXList], extends [MFXSkinBase].
///
/// The layout is quite simple: there is just one node, called the 'viewport', that is the `Pane` responsible for
/// containing and laying out the cells. Needless to say, the layout strategy is custom, and it's defined in the
/// [#layout()] method.
///
/// The skin reacts to changes in the list, see [#install()]. A new state updates the viewport's children, and a layout
/// request lays out the cells, [#layout()].
public class VFXListSkin<T, C extends VFXCell<T>> extends MFXSkinBase<VFXList<T, C>> {

    //================================================================================
    // Properties
    //================================================================================

    protected final Pane viewport;
    protected double DEFAULT_SIZE = 100.0;

    //================================================================================
    // Constructors
    //================================================================================

    public VFXListSkin(VFXList<T, C> list) {
        super(list);

        // Init viewport
        viewport = new Pane() {
            @Override
            protected void layoutChildren() {
                VFXListSkin.this.layout();
            }
        };
        viewport.getStyleClass().add("viewport");

        // End initialization
        getChildren().setAll(viewport);
    }

    //================================================================================
    // Methods
    //================================================================================

    /// Core method responsible for resizing and positioning cells in the viewport.
    /// This method will not execute if the layout was not requested, [VFXList#needsViewportLayoutProperty()]
    /// is false, or if the [VFXList#stateProperty()] is [VFXListState#INVALID].
    ///
    /// In any case, at the end of the method, [#onLayoutCompleted(boolean)] will be called.
    ///
    /// Cells are retrieved from the current list's state, given by the [VFXList#stateProperty()].
    /// The loop on the cells uses an external `i` variable that tracks the iteration count. This is because cells in the
    /// state are already ordered by their index (since the state uses a [TreeMap]), and the layout is 'absolute'.
    /// Meaning that the index of the cell is irrelevant for its position, we just care about which comes before/after.
    /// The layout is performed by [VFXListHelper#layout(int, VFXCell)], the index given to that method is the
    /// `i` variable.
    /// ```
    /// Little example:
    /// For a range of [16, 30]
    /// The first cell's index is 16, but its layout index is 0
    /// The second cell's index is 17, but its layout index is 1
    /// ...and so on
    ///```
    ///
    /// @see #onLayoutCompleted(boolean)
    protected void layout() {
        VFXList<T, C> list = getSkinnable();
        if (!list.isNeedsViewportLayout()) return;

        VFXListHelper<T, C> helper = list.getHelper();
        VFXListState<T, C> state = list.getState();
        if (state != VFXListState.INVALID) {
            int i = 0;
            for (C cell : state.getCellsByIndex().values()) {
                helper.layout(i, cell);
                i++;
            }
            onLayoutCompleted(true);
            return;
        }
        onLayoutCompleted(false);
    }

    /// This method is **crucial** because it resets the [VFXList#needsViewportLayoutProperty()] to false.
    /// If you override this method or the [#layout()], remember to call this!
    ///
    /// @param done this parameter can be useful to overriders as it gives information on whether the [#layout()]
    /// was executed correctly
    protected void onLayoutCompleted(boolean done) {
        VFXList<T, C> list = getSkinnable();
        list.setNeedsViewportLayout(false);
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    /// Registers the skin's listeners:
    ///
    /// - on [VFXList#stateProperty()], updates the viewport's children. They are cleared if the state is
    ///   [VFXListState#INVALID], otherwise they are set to the state's cells and a layout is requested, only if
    ///   [VFXListState#haveCellsChanged()]
    ///
    /// - on [VFXList#needsViewportLayoutProperty()], calls [#layout()] when a layout is requested
    ///
    /// - on [VFXList#helperProperty()], binds the viewport's translate properties to the helper's
    ///   [VFXListHelper#viewportPositionProperty()]. By translating the viewport, we give the illusion of scrolling
    ///   (virtual scrolling). This one also runs immediately, since the list always has a helper
    @Override
    public void install() {
        VFXList<T, C> list = getSkinnable();
        listen(
            // Core changes
            onInvalidated(list.stateProperty())
                .then(s -> {
                    if (s == VFXListState.INVALID) {
                        viewport.getChildren().clear();
                    } else if (s.haveCellsChanged()) {
                        viewport.getChildren().setAll(s.getNodes());
                        list.requestViewportLayout();
                    }
                }),
            onInvalidated(list.needsViewportLayoutProperty())
                .condition(v -> v)
                .then(v -> layout()),
            onInvalidated(list.helperProperty())
                .then(h -> {
                    viewport.translateXProperty().bind(h.viewportPositionProperty().map(Position::x));
                    viewport.translateYProperty().bind(h.viewportPositionProperty().map(Position::y));
                })
                .executeNow()
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
    /// Also sets the list's state to [VFXListState#INVALID].
    @SuppressWarnings("unchecked")
    @Override
    public void dispose() {
        VFXList<T, C> list = getSkinnable();
        list.update(VFXListState.INVALID);
        super.dispose();
    }
}
