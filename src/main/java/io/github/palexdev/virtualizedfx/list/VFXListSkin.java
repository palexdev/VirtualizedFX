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

package io.github.palexdev.virtualizedfx.list;

import java.util.TreeMap;

import io.github.palexdev.mfxcore.base.beans.Position;
import io.github.palexdev.mfxcore.controls.MFXSkinBase;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import javafx.beans.InvalidationListener;
import javafx.geometry.Orientation;
import javafx.scene.layout.Pane;

import static io.github.palexdev.mfxcore.observables.When.onInvalidated;

/// Default skin implementation for [VFXList], extends [MFXSkinBase] and expects behaviors of type
/// [VFXListManager].
///
/// The layout is quite simple: there is just one node, called the 'viewport', that is the `Pane` responsible for
/// containing and laying out the cells. Needless to say, the layout strategy is custom, and it's defined in the
/// [#layout()] method.
///
/// As all skins typically do, this is also responsible for catching any change in the component's properties.
/// The computation that leads to a new state is delegated to the controller/behavior, which is the [VFXListManager].
/// Read this [#addListeners()] to check which changes are handled.
///
/// Last but not least, by design, this skin makes the component always be at least 100px tall and wide. You can change this
/// by overriding the [#DEFAULT_SIZE] variable.
public class VFXListSkin<T, C extends VFXCell<T>> extends MFXSkinBase<VFXList<T, C>> {
    //================================================================================
    // Properties
    //================================================================================
    protected final Pane viewport;
    protected double DEFAULT_SIZE = 100.0;

    // To maximize performance, one listener is used to update on scroll, but it's added only on
    // one of the two position properties, depending on the orientation
    protected InvalidationListener pl = o -> getBehavior().onPositionChanged();

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
        swapPositionListener();
        addListeners();
        getChildren().setAll(viewport);
    }

    //================================================================================
    // Methods
    //================================================================================

    /// Adds listeners to the following component's properties which need to produce a new [VFXListState] upon changing.
    ///
    /// Here's the list:
    ///
    /// - Listener on [VFXList#stateProperty()], this is crucial to update the viewport's children and
    /// invoke [VFXList#requestViewportLayout()] if [VFXListState#haveCellsChanged()] is true
    ///
    /// - Listener on [VFXList#needsViewportLayoutProperty()], this is crucial because invokes [#layout()]
    ///
    /// - Listener on [VFXList#orientationProperty()], this is crucial because invokes [#swapPositionListener()]
    ///
    /// - Listener on [VFXList#helperProperty()], this is crucial because it's responsible for invoking
    /// [VFXListManager#onOrientationChanged()], as well as binding the viewport's translate properties to the
    /// [VFXListHelper#viewportPositionProperty()]. By translating the viewport, we give the illusion of scrolling
    /// (virtual scrolling)
    ///
    /// - Listener on [VFXList#widthProperty()], will invoke [VFXListManager#onGeometryChanged()]
    /// if the current orientation is [Orientation#HORIZONTAL], otherwise will just call [VFXList#requestViewportLayout()]
    ///
    /// - Listener on [VFXList#helperProperty()], will invoke [VFXListManager#onGeometryChanged()]
    /// if the current orientation is [Orientation#VERTICAL], otherwise will just call [VFXList#requestViewportLayout()]
    ///
    /// - Listener on [VFXList#bufferSizeProperty()], will invoke [VFXListManager#onGeometryChanged()].
    /// Yes, it is enough to threat this change as a geometry change to avoid code duplication
    ///
    /// - Listener on [VFXList#itemsProperty()], will invoke [VFXListManager#onItemsChanged()]
    ///
    /// - Listener on [VFXList#getCellFactory()], will invoke [VFXListManager#onCellFactoryChanged()]
    ///
    /// - Listener on [VFXList#fitToViewportProperty()], will invoke [VFXListManager#onFitToViewportChanged()]
    ///
    /// - Listener on [VFXList#cellSizeProperty()], will invoke [VFXListManager#onCellSizeChanged()]
    ///
    /// - Listener on [VFXList#spacingProperty()], will invoke [VFXListManager#onSpacingChanged()]
    protected void addListeners() {
        VFXList<T, C> list = getSkinnable();
        listeners(
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
            onInvalidated(list.orientationProperty())
                .then(o -> {
                    getBehavior().onOrientationChanged();
                    swapPositionListener();
                }),
            onInvalidated(list.helperProperty())
                .then(h -> {
                    viewport.translateXProperty().bind(h.viewportPositionProperty().map(Position::x));
                    viewport.translateYProperty().bind(h.viewportPositionProperty().map(Position::y));
                })
                .executeNow(),

            // Geometry changes
            onInvalidated(list.widthProperty())
                .condition(w -> list.getOrientation() == Orientation.HORIZONTAL)
                .then(w -> getBehavior().onGeometryChanged())
                .otherwise((l, w) -> list.requestViewportLayout()),
            onInvalidated(list.heightProperty())
                .condition(h -> list.getOrientation() == Orientation.VERTICAL)
                .then(h -> getBehavior().onGeometryChanged())
                .otherwise((l, h) -> list.requestViewportLayout()),
            onInvalidated(list.bufferSizeProperty())
                .then(b -> getBehavior().onGeometryChanged()),

            // Others
            onInvalidated(list.itemsProperty())
                .then(it -> getBehavior().onItemsChanged()),
            // DUDE! One thing cool in JavaFX, wow, I'm impressed. This invalidation listener will trigger when changes
            // occur in the list, or the list itself is changed, impressive!
            onInvalidated(list.getCellFactory())
                .then(f -> getBehavior().onCellFactoryChanged()),
            onInvalidated(list.fitToViewportProperty())
                .then(b -> getBehavior().onFitToViewportChanged()),
            onInvalidated(list.cellSizeProperty())
                .then(s -> getBehavior().onCellSizeChanged()),
            onInvalidated(list.spacingProperty())
                .then(s -> getBehavior().onSpacingChanged())
        );
    }

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
    /// ```
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

    /// You can scroll along two directions: vertically and horizontally. However, only the direction which coincides with
    /// the orientation ([VFXList#orientationProperty()]) will generate a new [VFXListState],
    /// in other words needs the invocation of [VFXListManager#onPositionChanged()].
    ///
    /// For this reason, there is one and only listener for the position change. When the orientation is [Orientation#VERTICAL],
    /// the listener is added to the [VFXList#vPosProperty()], otherwise it's added on the [VFXList#hPosProperty()].
    ///
    /// Note: this listener is not added through [#listeners(When\[\])], which means that its disposal is not automatic,
    /// and it's done in the overridden [#dispose()].
    protected void swapPositionListener() {
        VFXList<T, C> list = getSkinnable();
        Orientation orientation = list.getOrientation();
        if (orientation == Orientation.VERTICAL) {
            list.hPosProperty().removeListener(pl);
            list.vPosProperty().addListener(pl);
        } else {
            list.vPosProperty().removeListener(pl);
            list.hPosProperty().addListener(pl);
        }
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

    @SuppressWarnings("unchecked")
    @Override
    public void dispose() {
        VFXList<T, C> list = getSkinnable();
        list.vPosProperty().removeListener(pl);
        list.hPosProperty().removeListener(pl);
        pl = null;
        list.update(VFXListState.INVALID);
        super.dispose();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected VFXListManager<T, C> getBehavior() {
        return (VFXListManager<T, C>) super.getBehavior();
    }
}
