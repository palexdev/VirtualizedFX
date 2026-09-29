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
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

import static io.github.palexdev.mfxcore.observables.When.onInvalidated;

/// Default skin implementation for [VFXTable], extends [MFXSkinBase].
///
/// The table is organized in columns, rows and cells. This architecture leads to more complex layout compared to other
/// containers because it comprises many more nodes. The 'viewport' node wraps two [Pane]s:
///
/// 1) one contains the table's columns, can be selected in CSS as '.columns'
///
/// 2) the other contains the rows, can be selected in CSS as '.rows'
///
/// The table's height and the viewport height are different here. The latter is given by the table's height minus
/// the columns pane height, specified by [VFXTable#columnsSizeProperty()]. The rows are given by the
/// [VFXTable#stateProperty()] and depend on the viewport's height. Each column **in the current columns range**
/// produces one cell per row.
///
/// Q: Why so many nodes?
///
/// A: scrolling in a table is a bit peculiar because: vertical scrolling should affect only the rows, while horizontal
/// scrolling should affect both rows and columns. So, the whole viewport is translated on the x-axis, while only the
/// rows container is translated on the y-axis, see [VFXTableHelper#viewportPositionProperty()].
///
/// ## Clips
///
/// The viewport is clipped by a rectangle as big as the table, rounded by [VFXTable#clipBorderRadiusProperty()]. It
/// defines the table's visible shape, and it's counter-translated on the x-axis so that it stays still while the
/// viewport scrolls. Clipping the viewport rather than the table preserves any background or effect drawn on the
/// table itself.
///
/// The rows container has a clip of its own, which keeps the rows from going over the columns on vertical scroll.
/// It's counter-translated on the y-axis, and it's as wide as the rows container, cutting on the x-axis is left to the
/// viewport's clip.
///
/// ## What the skin does
///
/// The skin reacts to changes in the table, see [#install()]. A new state updates the containers' children,
/// [#updateChildren(VFXTableState)], and a layout request lays out the viewport, [#layoutViewport()]. It also
/// processes the columns marked for autosize, see [#layoutChildren(double,double,double,double)].
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

        getChildren().setAll(viewport);
    }

    //================================================================================
    // Methods
    //================================================================================

    /// Updates the containers' children to reflect the given state. Changing a children list is costly
    /// (JavaFX processes CSS again for every node that enters it), so it's done only when needed:
    ///
    /// - if the state is [VFXTableState#INVALID], both containers are emptied
    ///
    /// - if the state has no rows, [VFXTableState#isEmpty()], the rows container is emptied. Otherwise, it's filled with
    ///   the state's rows only if [VFXTableState#haveRowsChanged()]
    ///
    /// - the columns container is filled with the columns in the state's range only if
    ///   [VFXTableState#haveColumnsChanged()]
    ///
    /// Nothing is laid out here, that's up to the layout request the manager issues along with the state.
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

    /// Sizes the given column to fit its content. Called by [#layoutChildren(double,double,double,double)] for each column
    /// marked with [VFXTableColumn#sizeToContent()].
    ///
    /// The width is the maximum between the header's pref width (the column itself) and the pref width of the column's
    /// cells in the current state's rows. If no row has a cell for the column, the minimum width given by
    /// [VFXTable#columnsSizeProperty()] takes the cells' place. The mark is removed, and the result is set as the column's
    /// [VFXTableColumn#userPrefWidthProperty()], just like a resize by the user would do.
    ///
    /// The result is exact, the column can grow as well as shrink. The column's weight is not touched, so if it absorbs
    /// the leftover width, it keeps doing so on top of the new width, see [VFXTable#setWeight(VFXTableColumn,int)].
    ///
    /// The state is read on each call, since setting the width produces a new one right away.
    ///
    /// **Beware:** only the cells in the viewport, buffer included, can be measured. The rows are virtualized, and a cell
    /// that was never built has no width to give. So, the result is the widest content among the rows shown at the time.
    protected void autosize(VFXTableColumn<T, ?> column) {
        VFXTable<T> table = getSkinnable();
        double header = column.prefWidth(-1);
        double cellsMax = table.getState().getRowsByIndex().values().stream()
            .flatMap(r -> r.cells().get(column).stream())
            .map(VFXCell::toNode)
            .mapToDouble(n -> n.prefWidth(-1))
            .max()
            .orElseGet(() -> table.getColumnsSize().width());
        column.unmarkForAutosize();
        column.setUserPrefWidth(Math.max(header, cellsMax));
    }

    /// This core method is responsible for the viewport's layout. It's called by the listener on
    /// [VFXTable#needsViewportLayoutProperty()], right away, every time the table issues a valid request. The layout is
    /// completely manual, the containers' `layoutChildren()` are no-ops, and so is the rows' one.
    ///
    /// First, the two containers and the two clips are sized, in any case. The columns container is as wide as
    /// [VFXTable#virtualMaxXProperty()] and as tall as the columns. The rows container is as wide too, and takes what is
    /// left of the table's height below the columns. The viewport's clip takes the table's size minus the insets, the
    /// rows' clip takes the rows container's size.
    ///
    /// If the state is [VFXTableState#INVALID] the method ends here, calling [#layoutCompleted(boolean)] with `false`.
    ///
    /// Otherwise, the request tells which columns need to be laid out, as an interval of indexes, see [ViewportLayoutRequest]:
    ///
    /// - the columns that are both in the interval and in the state's columns range are laid out by
    ///   [VFXTableHelper#layoutColumn(int,VFXTableColumn)]
    ///
    /// - every row in the state is laid out by [VFXTableHelper#layoutRow(int,VFXTableRow)], with a layout index that
    ///   starts at 0. A row's geometry does not depend on the columns, and laying it out again with the same values
    ///   costs little, so rows are always processed
    ///
    /// - each row then lays out its cells, [VFXTableRow#layoutCells(int,int)], given the same interval. The row also
    ///   covers the cells it never positioned, which is why a request with an empty interval, [ViewportLayoutRequest#Y_ONLY],
    ///   still does something
    ///
    /// Finally, calls [#layoutCompleted(boolean)] with whether the request was valid, which it always is when coming from
    /// the listener.
    protected void layoutViewport() {
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

    /// Brings the [VFXTable#needsViewportLayoutProperty()] back to the idle state once a request has been processed:
    /// [ViewportLayoutRequest#DONE] if the layout was computed, [ViewportLayoutRequest#NULL] otherwise. Neither is a
    /// valid request, so this does not trigger another layout.
    protected void layoutCompleted(boolean done) {
        getSkinnable().setNeedsViewportLayout(done ? ViewportLayoutRequest.DONE : ViewportLayoutRequest.NULL);
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    /// Registers the skin's listeners:
    ///
    /// - on [VFXTable#stateProperty()], calls [#updateChildren(VFXTableState)]
    ///
    /// - on [VFXTable#needsViewportLayoutProperty()], calls [#layoutViewport()], only for valid requests, see
    ///   [ViewportLayoutRequest#isValid()]
    ///
    /// - on [VFXTable#helperProperty()], binds the viewport's `translateX` and the rows container's `translateY` to the
    ///   helper's [VFXTableHelper#viewportPositionProperty()]. By translating these nodes, we give the illusion of
    ///   scrolling (virtual scrolling). This one also runs immediately, since the table always has a helper
    ///
    /// **Note:** a state that already exists when the skin is installed is not rendered, the skin waits for the next one.
    /// The table produces its first state when it's sized for the first time, which normally happens once the skin is there.
    /// So, sizing a table before its skin exists (e.g., by calling `resize(...)` on it) is not supported.
    @Override
    public void install() {
        VFXTable<T> table = getSkinnable();
        listen(
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
    /// Also processes the columns marked for autosize, see [VFXTableColumn#sizeToContent()]. JavaFX lays out the table
    /// every time the containers' children change, which happens on most state changes, scrolling included. So this
    /// runs often, and costs only a scan of the columns range when there is nothing to do.
    ///
    /// Only the marked columns in the state's columns range are processed, and only if the state has rows, since there
    /// must be something to measure. Other marks stay, and are processed by the first layout after their column
    /// becomes measurable, for example when it's scrolled into view or when the table gets items. No listener is needed
    /// for that, such changes lay out the table anyway.
    ///
    /// When there is something to process, [Node#applyCss()] is called on the table before measuring. The table's nodes
    /// are created during the layout pass, after JavaFX processed CSS for the current pulse, so without it new cells
    /// would have no style and no skin, and would report a wrong width. It processes CSS for the whole table, which is
    /// why it's done only when needed. Each column is then measured by [#autosize(VFXTableColumn)].
    ///
    /// **Known limitation:** a layout requested while the table is being laid out is lost, that's how JavaFX works.
    /// So, if processing the marks brings other marked columns in range (e.g., by making columns narrower), those wait
    /// for the next layout of the table.
    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        super.layoutChildren(x, y, w, h);

        // Autosize marked columns
        VFXTable<T> table = getSkinnable();
        VFXTableState<T> state = table.getState();
        if (state.isEmpty()) return;

        var toAutosize = state.getColumnsRange().stream()
            .map(table.columns()::get)
            .filter(VFXTableColumn::isMarkedForAutosize)
            .toList();
        if (toAutosize.isEmpty()) return;

        table.applyCss(); // We can't avoid this, we must ensure that size computation will give a correct result
        toAutosize.forEach(this::autosize);
    }
}
