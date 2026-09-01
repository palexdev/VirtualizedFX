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

package io.github.palexdev.virtualizedfx.table;

import io.github.palexdev.mfxcore.base.beans.Position;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.controls.MFXSkinBase;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

import static io.github.palexdev.mfxcore.observables.When.onInvalidated;

public class VFXTableSkin<T> extends MFXSkinBase<VFXTable<T>> {

    //================================================================================
    // Properties
    //================================================================================

    protected final Pane viewport;
    private final Rectangle viewportClip;

    protected final Pane cContainer;
    protected final Pane rContainer;
    private final Rectangle rClip;

    protected double DEFAULT_SIZE = 100.0;

    //================================================================================
    // Constructors
    //================================================================================

    public VFXTableSkin(VFXTable<T> table) {
        super(table);

        // Init containers

        cContainer = new Pane() {
            @Override
            protected void layoutChildren() {/*manual, no-op*/}
        };
        // enabling overlays causes this node to also capture any event that should be on the cells
        cContainer.setPickOnBounds(false);
        cContainer.visibleProperty().bind(table.columnsSizeProperty().map(s -> s.height() > 0));
        cContainer.getStyleClass().add("columns");

        rContainer = new Pane() {
            @Override
            protected void layoutChildren() {/*manual, no-op*/}
        };
        rContainer.getStyleClass().add("rows");

        viewport = new Pane(rContainer, cContainer) { // order matters for overlay
            @Override
            protected void layoutChildren() {/*manual, no-op*/}
        };

        // Init clips

        // The viewport itself is clipped by a rounded rectangle: this defines the table's overall visible shape
        // (including the corner radius aligned with the table's border) and covers horizontal overflow for both
        // columns and rows. Since horizontal scrolling is implemented by translating the viewport, the clip is
        // counter-translated on X to stay stationary in the parent's frame. Clipping the viewport (a child of the
        // table) rather than the table node itself preserves any background/effect (e.g. a drop shadow) drawn on
        // the table.
        viewportClip = new Rectangle();
        viewportClip.arcWidthProperty().bind(table.clipBorderRadiusProperty());
        viewportClip.arcHeightProperty().bind(table.clipBorderRadiusProperty());
        viewportClip.translateXProperty().bind(viewport.translateXProperty().multiply(-1));
        viewport.setClip(viewportClip);

        // The rows container also needs its own clip. Its sole purpose is to prevent rows from bleeding into the
        // columns area during vertical scroll (rContainer is translated on Y to scroll and, without this clip,
        // partially scrolled rows would render above the header). No corner radius here: rounded corners are the
        // viewport clip's job. Sized to the full container (virtualW): horizontal clipping is also delegated to
        // the viewport clip; if we sized this to the visible width instead, it would move left with the viewport
        // during horizontal scroll and cut off the trailing cells.
        rClip = new Rectangle();
        rClip.translateYProperty().bind(rContainer.translateYProperty().multiply(-1));
        rContainer.setClip(rClip);

        // Finalize
        addListeners();
        getChildren().setAll(viewport);
    }

    //================================================================================
    // Methods
    //================================================================================

    protected void addListeners() {
        VFXTable<T> table = getSkinnable();
        listeners(
            onInvalidated(table.stateProperty())
                .then(this::updateChildren),
            onInvalidated(table.needsViewportLayoutProperty())
                .condition(ViewportLayoutRequest::isValid)
                .then(_ -> layoutViewport()),
            onInvalidated(table.helperProperty())
                .then(h -> {
                    viewport.translateXProperty().bind(h.viewportPositionProperty().map(Position::x));
                    rContainer.translateYProperty().bind(h.viewportPositionProperty().map(Position::y));
                })
                .executeNow()
        );
    }

    protected void updateChildren(VFXTableState<T> state) {
        VFXTable<T> table = getSkinnable();
        if (state == VFXTableState.INVALID) {
            cContainer.getChildren().clear();
            rContainer.getChildren().clear();
            return;
        }

        if (state.isEmpty()) {
            rContainer.getChildren().clear();
        } else if (state.haveRowsChanged()) {
            rContainer.getChildren().setAll(state.getRowsByIndex().values());
        }

        if (state.haveColumnsChanged()) {
            IntegerRange columnsRange = state.getColumnsRange();
            cContainer.getChildren().setAll(
                table.columns().subList(columnsRange.getMin(), columnsRange.getMax() + 1)
            );
        }
    }

    protected void layoutViewport() {
        // TODO we probably want to snap
        VFXTable<T> table = getSkinnable();
        double w = table.getWidth() - snappedLeftInset() - snappedRightInset();
        double virtualW = table.getVirtualMaxX();
        double h = table.getHeight() - snappedTopInset() - snappedBottomInset();
        double cH = table.getColumnsSize().height();
        double rH = h - cH;
        cContainer.resizeRelocate(0, 0, virtualW, cH);
        rContainer.resizeRelocate(0, cH, virtualW, rH);

        viewportClip.setWidth(w);
        viewportClip.setHeight(h);
        rClip.setWidth(virtualW);
        rClip.setHeight(rH);

        VFXTableState<T> state = table.getState();
        if (state == VFXTableState.INVALID) {
            layoutCompleted(false);
            return;
        }

        ViewportLayoutRequest request = table.getViewportLayoutRequest();
        int from = request.from();
        int to = request.to();

        VFXTableHelper<T> helper = table.getHelper();
        IntegerRange columnsRange = state.getColumnsRange();
        int lo = Math.max(columnsRange.getMin(), from);
        int hi = Math.min(columnsRange.getMax(), to);
        for (int i = lo; i <= hi; i++) helper.layoutColumn(i, table.columns().get(i));

        int layoutIdx = 0;
        for (VFXTableRow<T> row : state.getRowsByIndex().values()) {
            helper.layoutRow(layoutIdx++, row);
            row.layoutCells(from, to);
        }

        layoutCompleted(request.isValid());
    }

    protected void layoutCompleted(boolean done) {
        getSkinnable().setNeedsViewportLayout(done ? ViewportLayoutRequest.DONE : ViewportLayoutRequest.NULL);
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
    protected VFXTableManager<T> getBehavior() {
        return getSkinnable().getBehavior();
    }
}
