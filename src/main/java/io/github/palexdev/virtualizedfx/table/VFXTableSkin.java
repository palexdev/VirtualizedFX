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

package io.github.palexdev.virtualizedfx.table;

import io.github.palexdev.mfxcore.base.beans.Position;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.controls.MFXSkinBase;
import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.enums.ColumnsLayoutMode;
import io.github.palexdev.virtualizedfx.enums.GeometryChangeType;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

import static io.github.palexdev.mfxcore.observables.OnInvalidated.withListener;
import static io.github.palexdev.mfxcore.observables.When.onChanged;
import static io.github.palexdev.mfxcore.observables.When.onInvalidated;

/// Default skin implementation for [VFXTable], extends [MFXSkinBase] and expects behaviors of type
/// [VFXTableManager].
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
/// [VFXTable#stateProperty()] and depends on the viewport's height. Each column **in the current columns range**
/// produces one cell per row.
/// Columns and cells are kept aligned by the layout methods defined in [VFXTableHelper].
///
/// Q: Why so many nodes?
///
/// A: scrolling in a table is a bit peculiar because: vertical scrolling should affect only the rows,
/// while horizontal scrolling should affect both rows and columns.
/// For such reason, a clip node is set on the rows container and avoids rows overflow on vertical scroll.
///
/// As all skins typically do, this is also responsible for catching any change in the component's properties.
/// The computation that leads to a new state is delegated to the controller/behavior, which is the [VFXTableManager].
/// Read this [#addListeners()] to check which changes are handled.
///
/// Last but not least, by design, this skin makes the component always be at least 100px tall and wide. You can change this
/// by overriding the [#DEFAULT_SIZE] variable.
public class VFXTableSkin<T> extends MFXSkinBase<VFXTable<T>> {
    //================================================================================
    // Properties
    //================================================================================
    private final Pane viewport;
    private final Rectangle viewportClip;

    private final Pane cContainer;

    private final Pane rContainer;
    private final Rectangle rClip;

    private ListChangeListener<? super VFXTableColumn<T, ?>> columnsListener;
    protected double DEFAULT_SIZE = 100.0;

    //================================================================================
    // Constructors
    //================================================================================
    public VFXTableSkin(VFXTable<T> table) {
        super(table);

        // Init containers
        cContainer = new Pane() {
            @Override
            protected void layoutChildren() {
                layoutColumns();
            }
        };
        cContainer.visibleProperty().bind(table.columnsSizeProperty().map(s -> s.height() > 0));
        cContainer.getStyleClass().add("columns");
        // enabling overlays causes this node to also capture any event that should be on the cells
        cContainer.setPickOnBounds(false);

        rContainer = new Pane() {
            @Override
            protected void layoutChildren() {
                //layoutRows();
            }
        };
        rContainer.getStyleClass().add("rows");

        viewport = new Pane(rContainer, cContainer) { // order matters for overlay
            @Override
            protected void layoutChildren() {
                VFXTableSkin.this.layout();
            }
        };
        viewport.getStyleClass().add("viewport");

        // Init clips.
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

        // End initialization
        addListeners();
        getChildren().setAll(viewport);
    }

    //================================================================================
    // Methods
    //================================================================================

    /// Adds listeners on the component's properties which need to produce a new [VFXTableState] upon changing.
    ///
    /// Here's the list:
    ///
    /// - Listener on [VFXTable#getColumns()], will invoke [VFXTableManager#onColumnsChanged(ListChangeListener.Change)].
    /// The method is also invoked the first time here with `null` as parameter to ensure that columns are initialized
    /// for the first time.
    ///
    /// - Listener on [VFXTable#stateProperty()], this is crucial to update the columns and rows containers'
    /// children, invoke [VFXTable#requestViewportLayout()] if [VFXTableState#isLayoutNeeded()] is `true`.
    /// Skips everything if the current state was cloned, [VFXTableState#isClone()]
    ///
    /// - Listener on [VFXTable#needsViewportLayoutProperty()], this is crucial because invokes both
    /// [#layoutColumns()] and [#layoutRows()]. The layout is performed only if [ViewportLayoutRequest#isValid()]
    /// returns `true`. Also, if the request carries a specific column ([ViewportLayoutRequest#column()]),
    /// the layout will be computed only partially by invoking [#partialLayout()] instead.
    ///
    /// - Listener on [VFXTable#helperProperty()], this is crucial because it's responsible for binding the
    /// viewport's translateX and rows container's translateY properties to the [VFXTableHelper#viewportPositionProperty()].
    /// By translating the viewport, we give the illusion of scrolling (virtual scrolling)
    ///
    /// - Listener on [VFXTable#widthProperty()], will invoke [VFXTableManager#onGeometryChanged(GeometryChangeType)]
    ///
    /// - Listener on [VFXTable#heightProperty()], will invoke [VFXTableManager#onGeometryChanged(GeometryChangeType)]
    ///
    /// - Listener on [VFXTable#columnsBufferSizeProperty()], will invoke [VFXTableManager#onGeometryChanged(GeometryChangeType)]
    ///
    /// - Listener on [VFXTable#rowsBufferSizeProperty()], will invoke [VFXTableManager#onGeometryChanged(GeometryChangeType)]
    ///
    /// - Listener on [VFXTable#vPosProperty()], will invoke [VFXTableManager#onPositionChanged(Orientation)]
    ///
    /// - Listener on [VFXTable#hPosProperty()], will invoke [VFXTableManager#onPositionChanged(Orientation)]
    ///
    /// - Listener on [VFXTable#itemsProperty()], will invoke [VFXTableManager#onItemsChanged()]
    ///
    /// - Listener on [VFXTable#rowFactoryProperty()], will invoke [VFXTableManager#onRowFactoryChanged()]
    ///
    /// - Listener on [VFXTable#rowsHeightProperty()], will invoke [VFXTableManager#onRowHeightChanged()]
    ///
    /// - Listener on [VFXTable#columnsSizeProperty()], will invoke [VFXTableManager#onColumnsSizeChanged()]
    ///
    /// - Listener on [VFXTable#columnsLayoutModeProperty()], will invoke [VFXTableManager#onColumnsLayoutModeChanged()]
    ///
    /// **Note:** in JavaFX there is no way to prioritize a listener over another, rather, the priority is given by
    /// which is added first (behind the scenes there must be a plain for loop running to call all the listeners).
    /// That's a trap for every handler here that reads something which is itself a binding on the property it is
    /// listening to: registered as an [InvalidationListener], it may run *before* that binding invalidates and read a
    /// stale value.
    ///
    /// Three of the listeners above are in exactly that position. [VFXTable#widthProperty()], because in
    /// [ColumnsLayoutMode#VARIABLE] the [ColumnsLayoutCache] must be invalidated before
    /// [VFXTableManager#onGeometryChanged(GeometryChangeType)] runs. And [VFXTable#vPosProperty()] /
    /// [VFXTable#hPosProperty()], because [VFXTableManager#onPositionChanged(Orientation)] reads the helper's ranges,
    /// which are lazy bindings on those same two properties.
    ///
    /// The workaround is the same for all three: use a [ChangeListener] instead of a plain [InvalidationListener],
    /// because the latter type will ALWAYS be invoked BEFORE the former. In my opinion, this mechanism is stupid and
    /// broken, bindings invalidation should ALWAYS happen before anything else!
    ///
    /// Registration order normally hides the problem, since the helper is built before the skin. What exposes it is a
    /// [ColumnsLayoutMode] switch, because it builds a **new** helper whose listeners then land after the skin's.
    /// The same trap seen from the other side is why [ColumnsLayoutCache] listens to [VFXTable#getColumns()] as an
    /// [InvalidationListener] rather than a [ListChangeListener].
    protected void addListeners() {
        VFXTable<T> table = getSkinnable();

        // This needs to be a classical listener
        columnsListener = getBehavior()::onColumnsChanged;
        table.getColumns().addListener(columnsListener);
        getBehavior().onColumnsChanged(null); // This is needed since the skin is created afterward.

        InvalidationListener gcl = i -> getBehavior().onGeometryChanged(GeometryChangeType.OTHER);
        listeners(
            // Core changes
            onInvalidated(table.stateProperty())
                .then(s -> {
                    if (s.isClone()) return;
                    if (s == VFXTableState.INVALID) {
                        cContainer.getChildren().clear();
                        rContainer.getChildren().clear();
                        return;
                    }
                    if (s.isEmpty()) {
                        rContainer.getChildren().clear();
                    } else if (s.haveRowsChanged()) {
                        rContainer.getChildren().setAll(s.getRowsByIndex().values());
                    }
                    if (s.haveColumnsChanged()) {
                        cContainer.getChildren().setAll(
                            table.getColumns().subList(
                                s.getColumnsRange().getMin(),
                                s.getColumnsRange().getMax() + 1
                            )
                        );
                    }
                    if (s.isLayoutNeeded()) table.requestViewportLayout();
                }),
            onInvalidated(table.needsViewportLayoutProperty())
                .condition(ViewportLayoutRequest::isValid)
                .then(v -> {
                    if (v.isPartial()) {
                        partialLayout();
                    } else {
                        layoutColumns();
                        layoutRows();
                    }
                }),
            onInvalidated(table.helperProperty())
                .then(h -> {
                    viewport.translateXProperty().bind(h.viewportPositionProperty().map(Position::x));
                    rContainer.translateYProperty().bind(h.viewportPositionProperty().map(Position::y));
                })
                .executeNow(),

            // Geometry changes
            // BUG: unfortunately we must use a ChangeListener here because JavaFX is stupid.
            // You see, for the VARIABLE_MODE layout, we rely on a cache to compute the columns' width only when needed.
            // The last column is a special case because it's the only one for which the value becomes invalid if the
            // table's width changes. Since JavaFX bindings use some sort of InvalidationListeners on the dependencies to
            // invalidate the bindings itself, there's a huge pain in the ass problem: priority.
            // Under the hood, these things are simple; there is a for loop somewhere that calls the listeners
            // (or at least you can think at the mechanism like this), which means that if a listener is added before
            // another one, it's executed first.
            // This is a huge problem here, because we can't proceed with the onGeometryChanged() computation before the
            // cache is invalidated.
            // A simple workaround is to use ChangeListeners which are always invoked AFTER InvalidationListeners.
            //
            // In my opinion, this mechanism is stupid and broken, an InvalidationListener whose purpose is to invalidate
            // a binding should ALWAYS be called BEFORE any other InvalidationListener
            onChanged(table.widthProperty())
                .then((ow, nw) -> getBehavior().onGeometryChanged(GeometryChangeType.WIDTH)),
            onInvalidated(table.heightProperty())
                .then(h -> getBehavior().onGeometryChanged(GeometryChangeType.HEIGHT)),
            withListener(table.columnsBufferSizeProperty(), gcl),
            withListener(table.rowsBufferSizeProperty(), gcl),

            // Position changes.
            // ChangeListeners for the same priority reason described above for the width property.
            // onPositionChanged() reads the helper's ranges, and those are lazy bindings that depend on
            // vPos/hPos. As InvalidationListeners these would run before the bindings' own invalidation
            // listeners, so the ranges would still be valid-but-stale and the manager would compare the
            // state against the *previous* range, conclude nothing changed and skip the update entirely.
            // Registration order normally saves us, since the helper is built before the skin, but a
            // layout mode switch builds a new helper whose listeners then land after the skin's.
            onChanged(table.vPosProperty())
                .then((ov, nv) -> getBehavior().onPositionChanged(Orientation.VERTICAL)),
            onChanged(table.hPosProperty())
                .then((oh, nh) -> getBehavior().onPositionChanged(Orientation.HORIZONTAL)),

            // Others
            onInvalidated(table.itemsProperty())
                .then(it -> getBehavior().onItemsChanged()),
            onInvalidated(table.rowFactoryProperty())
                .then(rf -> getBehavior().onRowFactoryChanged()),
            onInvalidated(table.rowsHeightProperty())
                .then(h -> getBehavior().onRowHeightChanged()),
            onInvalidated(table.columnsSizeProperty())
                .then(s -> getBehavior().onColumnsSizeChanged()),
            onInvalidated(table.columnsLayoutModeProperty())
                .then(m -> getBehavior().onColumnsLayoutModeChanged())
        );
    }

    /// This method redefines the viewport node layout. It's responsible for positioning and sizing both the
    /// columns and rows containers.
    ///
    /// @see #layoutColumns()
    /// @see #layoutRows()
    /// @see #partialLayout()
    protected void layout() {
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
    }

    /// This is responsible for sizing and positioning the columns specified by the current
    /// [VFXTableState#getColumnsRange()].
    ///
    /// If the state is [VFXTableState#INVALID] exits immediately.
    ///
    /// The columns are actually laid out by using [VFXTableHelper#layoutColumn(int, VFXTableColumn)], which takes
    /// the column's absolute index in [VFXTable#getColumns()], so the range index is passed straight through.
    ///
    /// This is also responsible for updating the [VFXTableColumn#indexProperty()] by calling
    /// [#updateColumnIndex(VFXTableColumn, int)]. Why here? Because this core method will ensure all columns will
    /// always have the correct index set.
    ///
    /// @see #layoutRows()
    /// @see #partialLayout()
    protected void layoutColumns() {
        VFXTable<T> table = getSkinnable();
        VFXTableState<T> state = table.getState();
        if (state == VFXTableState.INVALID) return;

        VFXTableHelper<T> helper = table.getHelper();
        IntegerRange columnsRange = state.getColumnsRange();
        ObservableList<VFXTableColumn<T, ?>> columns = table.getColumns();
        for (Integer idx : columnsRange) {
            VFXTableColumn<T, ?> column = columns.get(idx);
            updateColumnIndex(column, idx); // Updating the columns' index here should ensure to always have a correct index
            helper.layoutColumn(idx, column);
        }
    }

    /// This is responsible for sizing and positioning both the rows and their cells.
    ///
    /// If the current [VFXTableState] is either [VFXTableState#INVALID] or [VFXTableState#isEmpty()] then
    /// exits and calls [#onLayoutCompleted(boolean)] with `false` as parameter.
    ///
    /// The layout is computed by iterating over the rows given by [VFXTableState#getRowsByIndex()].
    /// Each row is laid out by [VFXTableHelper#layoutRow(int, VFXTableRow)], and on each row
    /// [VFXTableRow#layoutCells()] is called (this is actually responsible for the cells' layout).
    /// The layout index is given by an external 'i' counter which starts at 0 and is incremented at each loop iteration.
    ///
    /// If the loop completes successfully, [#onLayoutCompleted(boolean)] is invoked with `true` as parameter.
    protected void layoutRows() {
        VFXTable<T> table = getSkinnable();
        VFXTableHelper<T> helper = table.getHelper();
        VFXTableState<T> state = table.getState();
        if (state != VFXTableState.INVALID) {
            int i = 0;
            for (VFXTableRow<T> row : state.getRowsByIndex().values()) {
                helper.layoutRow(i, row);
                row.layoutCells();
                i++;
            }
            onLayoutCompleted(true);
            return;
        }
        onLayoutCompleted(false);
    }

    /// There are certain situations in which it's not necessary to re-compute the whole table layout, but it's enough to
    /// only compute it partially, starting from a specific column. This is indeed a good optimization, especially when
    /// using the [ColumnsLayoutMode#VARIABLE] mode.
    ///
    /// Examples of when this may happen: 1) when in `FIXED` mode, the table's width exceeds the `virtualMaxX`,
    /// which means that only the last column (and all its related cells) needs to be resized to fill the space;
    /// 2) when in `VARIABLE` mode, for a column that changes its width, we need to recompute the layout only for
    /// the column itself and the others that come after.
    ///
    /// So, how does this work?
    ///
    /// If the current [VFXTableState] is [VFXTableState#INVALID] exits immediately and calls
    /// [#onLayoutCompleted(boolean)] with `false` as parameter.
    ///
    /// If using the [ColumnsLayoutMode#FIXED], we simply call [VFXTableHelper#layoutColumn(int, VFXTableColumn)]
    /// on the column given by [ViewportLayoutRequest#column()] (which is expected to be the last column in the table).
    /// Then iterates on all the rows in the state, [VFXTableState#getRowsByIndex()], resize each of them because the
    /// `virtualMaxX` has probably changed, then from each row retrieves the column's related cell and lays it out with
    /// [VFXTableHelper#layoutCell(int, VFXTableCell)]. Both layout methods take the column's **absolute** index in
    /// [VFXTable#getColumns()], so the one resolved by [VFXTable#indexOf(VFXTableColumn)] is passed straight through.
    ///
    /// If using [ColumnsLayoutMode#VARIABLE] two things can happen:
    ///
    /// 1) if the column carried by [ViewportLayoutRequest#column()] is the last one in the table, then we
    /// re-compute the whole layout through [#layoutColumns()] and [#layoutRows()] (the latter reports the completion,
    /// so this branch does not call [#onLayoutCompleted(boolean)] itself). The last column's width depends on every
    /// other column's, so a change there is never local, and the remaining edge cases are not easy to manage; the
    /// strategy here is to go for stability rather than performance (also because handling all the edge cases may
    /// actually harm it). Note that a 'whole' layout is still bounded by the columns range, so it covers the window
    /// and not the entire list.
    ///
    /// 2) for any other column we can actually optimize, since a resize can only move the columns that come after it.
    /// The start index is the changed column's, clamped up to the state's [VFXTableState#getColumnsRange()] minimum,
    /// and the end is that range's maximum. If the changed column sits past the range there is nothing on screen to
    /// re-lay out, but the rows are still resized, because `virtualMaxX` may have changed, and
    /// [#onLayoutCompleted(boolean)] is called with `false`.
    /// Otherwise, each column in `[from, max]` is resized and repositioned by [VFXTableHelper#layoutColumn(int, VFXTableColumn)].
    /// Then it iterates over the rows given by [VFXTableState#getRowsByIndex()], resizes each of them, and in a nested
    /// loop lays out only their cells in that same sub-range, with [VFXTableHelper#layoutCell(int, VFXTableCell)].
    ///
    /// Finally calls [#onLayoutCompleted(boolean)] with `true` as parameter.
    protected void partialLayout() {
        VFXTable<T> table = getSkinnable();
        VFXTableState<T> state = table.getState();
        if (state == VFXTableState.INVALID) {
            onLayoutCompleted(false);
            return;
        }

        VFXTableHelper<T> helper = table.getHelper();
        ColumnsLayoutMode layoutMode = table.getColumnsLayoutMode();
        VFXTableColumn<T, ?> column = table.getViewportLayoutRequest().column();
        int cIndex = table.indexOf(column);
        if (layoutMode == ColumnsLayoutMode.FIXED) {
            helper.layoutColumn(cIndex, column);
            state.getRowsByIndex().values().forEach(r -> {
                r.resize(table.getVirtualMaxX(), r.getHeight());
                VFXTableCell<T> cell = r.getCells().get(cIndex);
                helper.layoutCell(cIndex, cell);
            });
            onLayoutCompleted(true);
            return;
        }

        // There are too many edge cases, taking into account all of them may degrade performance rather than improving it.
        // Not that costly anyway: a full layout only ever covers the columns range, never the whole columns' list.
        if (helper.isLastColumn(column)) {
            layoutColumns();
            layoutRows();
            return;
        }

        // If it's VARIABLE mode, and it's not the last column, then it means we can actually do some optimization.
        // Rather than looping over all the columns, we just need to update those starting from the index that changed.
        int min = state.getColumnsRange().getMin();
        int max = state.getColumnsRange().getMax();
        int from = Math.max(cIndex, min);
        if (from > max) {
            // Even if outside range, the rows must be resized in case the vMaxX changed (for example column grow/shrink)
            state.getRowsByIndex().values().forEach(r -> r.resize(table.getVirtualMaxX(), r.getHeight()));
            onLayoutCompleted(false);
            return;
        }

        ObservableList<VFXTableColumn<T, ? extends VFXTableCell<T>>> columns = table.getColumns();
        IntegerRange cRange = IntegerRange.of(from, max);
        cRange.forEach(i -> helper.layoutColumn(i, columns.get(i)));

        state.getRowsByIndex().values().forEach(r -> {
            r.resize(table.getVirtualMaxX(), r.getHeight());
            cRange.forEach(i -> helper.layoutCell(i, r.getCells().get(i)));
        });
        onLayoutCompleted(true);
    }

    /// This must be called after processing a [ViewportLayoutRequest] to bring the [VFXTable#needsViewportLayoutProperty()]
    /// back to the idle state, which is [ViewportLayoutRequest#DONE] if the layout was computed,
    /// [ViewportLayoutRequest#NULL] otherwise.
    protected void onLayoutCompleted(boolean done) {
        VFXTable<T> table = getSkinnable();
        table.setNeedsViewportLayout(done ? ViewportLayoutRequest.DONE : ViewportLayoutRequest.NULL);
    }

    /// This can be called during layout or other operations to update the given column's [VFXTableColumn#indexProperty()]
    /// to the given index. This is indeed a strange place to do so, but as it turns out, layout methods are the most
    /// reliable to ensure columns will always have the correct index.
    protected void updateColumnIndex(VFXTableColumn<T, ?> column, int index) {
        column.setIndex(index);
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
    public void dispose() {
        VFXTable<T> table = getSkinnable();
        if (columnsListener != null) {
            table.getColumns().removeListener(columnsListener);
            columnsListener = null;
        }
        super.dispose();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected VFXTableManager<T> getBehavior() {
        return (VFXTableManager<T>) super.getBehavior();
    }
}
