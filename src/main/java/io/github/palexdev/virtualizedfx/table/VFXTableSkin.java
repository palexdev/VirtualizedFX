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

/**
 * Default skin implementation for {@link VFXTable}, extends {@link MFXSkinBase} and expects behaviors of type
 * {@link VFXTableManager}.
 * <p>
 * The table is organized in columns, rows and cells. This architecture leads to more complex layout compared to other
 * containers because it comprises many more nodes. The 'viewport' node wraps two {@link Pane}s:
 * <p> 1) one contains the table's columns, can be selected in CSS as '.columns'
 * <p> 2) the other contains the rows, can be selected in CSS as '.rows'
 * <p>
 * The table's height and the viewport height are different here. The latter is given by the table's height minus
 * the columns pane height, specified by {@link VFXTable#columnsSizeProperty()}. The rows are given by the
 * {@link VFXTable#stateProperty()} and depends on the viewport's height. Each column produces one cell per row.
 * Columns and cells are kept aligned by the layout methods defined in {@link VFXTableHelper}.
 * <p></p>
 * Q: Why so many nodes?
 * <p>
 * A: scrolling in a table is a bit peculiar because: vertical scrolling should affect only the rows,
 * while horizontal scrolling should affect both rows and columns.
 * For such reason, a clip node is set on the rows container and avoids rows overflow on vertical scroll.
 * <p></p>
 * As all skins typically do, this is also responsible for catching any change in the component's properties.
 * The computation that leads to a new state is delegated to the controller/behavior, which is the {@link VFXTableManager}.
 * Read this {@link #addListeners()} to check which changes are handled.
 * <p></p>
 * Last but not least, by design, this skin makes the component always be at least 100px tall and wide. You can change this
 * by overriding the {@link #DEFAULT_SIZE} variable.
 */
public class VFXTableSkin<T> extends MFXSkinBase<VFXTable<T>> {
    //================================================================================
    // Properties
    //================================================================================
    private final Pane viewport;

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

        rContainer = new Pane() {
            @Override
            protected void layoutChildren() {
                //layoutRows();
            }
        };
        rContainer.getStyleClass().add("rows");

        viewport = new Pane(cContainer, rContainer) {
            @Override
            protected void layoutChildren() {
                VFXTableSkin.this.layout();
            }
        };
        viewport.getStyleClass().add("viewport");

        // Init rows clip
        rClip = new Rectangle();
        rClip.widthProperty().bind(rContainer.widthProperty());
        rClip.heightProperty().bind(rContainer.heightProperty());
        rClip.translateYProperty().bind(rContainer.translateYProperty().multiply(-1));
        rContainer.setClip(rClip);

        // End initialization
        addListeners();
        getChildren().add(viewport);
    }

    //================================================================================
    // Methods
    //================================================================================

    /**
     * Adds listeners on the component's properties which need to produce a new {@link VFXTableState} upon changing.
     * <p>
     * Here's the list:
     * <p> - Listener on {@link VFXTable#getColumns()}, will invoke {@link VFXTableManager#onColumnsChanged(ListChangeListener.Change)}.
     * The method is also invoked the first time here with {@code null} as parameter to ensure that columns are initialized
     * for the first time.
     * <p> - Listener on {@link VFXTable#stateProperty()}, this is crucial to update the columns and rows containers'
     * children, invoke {@link VFXTable#requestViewportLayout()} if {@link VFXTableState#isLayoutNeeded()} is {@code true}.
     * Skips everything if the current state was cloned, {@link VFXTableState#isClone()}
     * <p> - Listener on {@link VFXTable#needsViewportLayoutProperty()}, this is crucial because invokes both
     * {@link #layoutColumns()} and {@link #layoutRows()}. The layout is performed only if {@link ViewportLayoutRequest#isValid()}
     * returns {@code true}. Also, if the request carries a specific column ({@link ViewportLayoutRequest#column()}),
     * the layout will be computed only partially by invoking {@link #partialLayout()} instead.
     * <p> - Listener on {@link VFXTable#helperProperty()}, this is crucial because it's responsible for binding the
     * viewport's translateX and rows container's translateY properties to the {@link VFXTableHelper#viewportPositionProperty()}.
     * By translating the viewport, we give the illusion of scrolling (virtual scrolling)
     * <p> - Listener on {@link VFXTable#widthProperty()}, will invoke {@link VFXTableManager#onGeometryChanged(GeometryChangeType)}
     * <p> - Listener on {@link VFXTable#heightProperty()}, will invoke {@link VFXTableManager#onGeometryChanged(GeometryChangeType)}
     * <p> - Listener on {@link VFXTable#columnsBufferSizeProperty()}, will invoke {@link VFXTableManager#onGeometryChanged(GeometryChangeType)}
     * <p> - Listener on {@link VFXTable#rowsBufferSizeProperty()}, will invoke {@link VFXTableManager#onGeometryChanged(GeometryChangeType)}
     * <p> - Listener on {@link VFXTable#vPosProperty()}, will invoke {@link VFXTableManager#onPositionChanged(Orientation)}
     * <p> - Listener on {@link VFXTable#hPosProperty()}, will invoke {@link VFXTableManager#onPositionChanged(Orientation)}
     * <p> - Listener on {@link VFXTable#itemsProperty()}, will invoke {@link VFXTableManager#onItemsChanged()}
     * <p> - Listener on {@link VFXTable#rowFactoryProperty()}, will invoke {@link VFXTableManager#onRowFactoryChanged()}
     * <p> - Listener on {@link VFXTable#rowsHeightProperty()}, will invoke {@link VFXTableManager#onRowHeightChanged()}
     * <p> - Listener on {@link VFXTable#columnsSizeProperty()}, will invoke {@link VFXTableManager#onColumnsSizeChanged()}
     * <p> - Listener on {@link VFXTable#columnsLayoutModeProperty()}, will invoke {@link VFXTableManager#onColumnsLayoutModeChanged()}
     * <p></p>
     * <b>Note:</b> in JavaFX there is no way to prioritize a listener over another, rather, the priority is given by
     * which is added first (behind the scenes there must be a plain for loop running to call all the listeners).
     * This causes a nasty bug regarding the table's width when using the {@link ColumnsLayoutMode#VARIABLE}. In that mode
     * we cannot proceed with the {@link VFXTableManager#onGeometryChanged(GeometryChangeType)} method before the
     * {@link ColumnsLayoutCache} is invalidated. A simple workaround for this, is to use a {@link ChangeListener}
     * instead of a plain {@link InvalidationListener} for the {@link VFXTable#widthProperty()}, because the latter type
     * will ALWAYS be invoked BEFORE the other listeners type. In my opinion, this mechanism is stupid and broken, bindings
     * invalidation should ALWAYS happen before anything else!
     */
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
            /*
             * BUG: unfortunately we must use a ChangeListener here because JavaFX is stupid.
             * You see, for the VARIABLE_MODE layout, we rely on a cache to compute the columns' width only when needed.
             * The last column is a special case because it's the only one for which the value becomes invalid if the
             * table's width changes. Since JavaFX bindings use some sort of InvalidationListeners on the dependencies to
             * invalidate the bindings itself, there's a huge pain in the ass problem: priority.
             * Under the hood, these things are simple; there is a for loop somewhere that calls the listeners
             * (or at least you can think at the mechanism like this), which means that if a listener is added before
             * another one, it's executed first.
             * This is a huge problem here, because we can't proceed with the onGeometryChanged() computation before the
             * cache is invalidated.
             * A simple workaround is to use ChangeListeners which are always invoked AFTER InvalidationListeners.
             *
             * In my opinion, this mechanism is stupid and broken, an InvalidationListener whose purpose is to invalidate
             * a binding should ALWAYS be called BEFORE any other InvalidationListener
             */
            onChanged(table.widthProperty())
                .then((ow, nw) -> getBehavior().onGeometryChanged(GeometryChangeType.WIDTH)),
            onInvalidated(table.heightProperty())
                .then(h -> getBehavior().onGeometryChanged(GeometryChangeType.HEIGHT)),
            withListener(table.columnsBufferSizeProperty(), gcl),
            withListener(table.rowsBufferSizeProperty(), gcl),

            // Position changes
            onInvalidated(table.vPosProperty())
                .then(v -> getBehavior().onPositionChanged(Orientation.VERTICAL)),
            onInvalidated(table.hPosProperty())
                .then(h -> getBehavior().onPositionChanged(Orientation.HORIZONTAL)),

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

    /**
     * This method redefines the viewport node layout. It's responsible for positioning and sizing both the
     * columns and rows containers.
     *
     * @see #layoutColumns()
     * @see #layoutRows()
     * @see #partialLayout()
     */
    protected void layout() {
        VFXTable<T> table = getSkinnable();
        double w = table.getVirtualMaxX();
        double h = table.getHeight();
        double cH = table.getColumnsSize().height();
        double rH = h - cH;
        cContainer.resizeRelocate(0, 0, w, cH);
        rContainer.resizeRelocate(0, cH, w, rH);
    }

    /**
     * This is responsible for sizing and positioning the columns specified by the current
     * {@link VFXTableState#getColumnsRange()}.
     * <p>
     * If the state is {@link VFXTableState#INVALID} exits immediately.
     * <p>
     * The columns are actually laid out by using {@link VFXTableHelper#layoutColumn(int, VFXTableColumn)}.
     * The layout index is given by an external 'i' counter which starts at 0 and is incremented at each loop iteration.
     * <p>
     * This is also responsible for updating the {@link VFXTableColumn#indexProperty()} by calling
     * {@link #updateColumnIndex(VFXTableColumn, int)}. Why here? Because this core method will ensure all columns will
     * always have the correct index set.
     *
     * @see #layoutRows()
     * @see #partialLayout()
     */
    protected void layoutColumns() {
        VFXTable<T> table = getSkinnable();
        VFXTableState<T> state = table.getState();
        if (state == VFXTableState.INVALID) return;

        VFXTableHelper<T> helper = table.getHelper();
        IntegerRange columnsRange = state.getColumnsRange();
        int i = 0;
        ObservableList<VFXTableColumn<T, ?>> columns = table.getColumns();
        for (Integer idx : columnsRange) {
            VFXTableColumn<T, ?> column = columns.get(idx);
            updateColumnIndex(column, idx); // Updating the columns' index here should ensure to always have a correct index
            helper.layoutColumn(i, column);
            i++;
        }
    }

    /**
     * This is responsible for sizing and positioning both the rows and their cells.
     * <p>
     * If the current {@link VFXTableState} is either {@link VFXTableState#INVALID} or {@link VFXTableState#isEmpty()} then
     * exits and calls {@link #onLayoutCompleted(boolean)} with {@code false} as parameter.
     * <p>
     * The layout is computed by iterating over the rows given by {@link VFXTableState#getRowsByIndex()}.
     * Each row is laid out by {@link VFXTableHelper#layoutRow(int, VFXTableRow)}, and on each row
     * {@link VFXTableRow#layoutCells()} is called (this is actually responsible for the cells' layout).
     * The layout index is given by an external 'i' counter which starts at 0 and is incremented at each loop iteration.
     * <p>
     * If the loop completes successfully, {@link #onLayoutCompleted(boolean)} is invoked with {@code true} as parameter.
     */
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

    /**
     * There are certain situations in which it's not necessary to re-compute the whole table layout, but it's enough to
     * only compute it partially, starting from a specific column. This is indeed a good optimization, especially when
     * using the {@link ColumnsLayoutMode#VARIABLE} mode.
     * <p>
     * Examples of when this may happen: 1) when in {@code FIXED} mode, the table's width exceeds the {@code virtualMaxX},
     * which means that only the last column (and all its related cells) needs to be resized to fill the space;
     * 2) when in {@code VARIABLE} mode, for a column that changes its width, we need to recompute the layout only for
     * the column itself and the others that come after.
     * <p></p>
     * So, how does this work?
     * <p>
     * If using the {@link ColumnsLayoutMode#FIXED}, we simply call {@link VFXTableHelper#layoutColumn(int, VFXTableColumn)}
     * on the column given by {@link ViewportLayoutRequest#column()} (which is expected to be the last column in the table).
     * Then iterates on all the rows in the state, {@link VFXTableState#getRowsByIndex()}, resize each of them because the
     * {@code virtualMaxX} has probably changed, then from each row retrieves the column's related cell and call
     * {@link VFXTableHelper#layoutRow(int, VFXTableRow)}. Note: the layout index is given by {@link IntegerRange#diff()}
     * on {@link VFXTableState#getColumnsRange()}.
     * <p></p>
     * If using {@link ColumnsLayoutMode#VARIABLE} two things can happen:
     * <p> 1) if the column carried by {@link ViewportLayoutRequest#column()} is the last one in the table, then we
     * re-compute the whole layout. The issue is that there are some edge cases that may not be easy to manage, so
     * the strategy here is to go for stability rather than performance (also because handling all the edge cases may
     * actually harm it).
     * <p> 2) for any other column we can actually optimize. First, it loops over the columns starting from the index
     * of the changed column. Each column is resized and repositioned by {@link VFXTableHelper#layoutColumn(int, VFXTableColumn)}.
     * Then iterates over the rows given by {@link VFXTableState#getRowsByIndex()}, iterates over then and resizes all
     * of them by using {@link VFXTableHelper#layoutRow(int, VFXTableRow)}. In a nested loop, for each row, it updates
     * only the cells from the aforementioned start index, uses {@link VFXTableHelper#layoutCell(int, VFXTableCell)}.
     * <p>
     * Finally calls {@link #onLayoutCompleted(boolean)} with {@code true} as parameter.
     */
    protected void partialLayout() {
        VFXTable<T> table = getSkinnable();
        VFXTableState<T> state = table.getState();
        if (state == VFXTableState.INVALID) return;

        VFXTableHelper<T> helper = table.getHelper();
        ColumnsLayoutMode layoutMode = table.getColumnsLayoutMode();
        VFXTableColumn<T, ?> column = table.getViewportLayoutRequest().column();
        int cIndex = table.indexOf(column);
        if (layoutMode == ColumnsLayoutMode.FIXED) {
            int layoutIndex = state.getColumnsRange().diff();
            helper.layoutColumn(layoutIndex, column);
            state.getRowsByIndex().values().forEach(r -> {
                r.resize(table.getVirtualMaxX(), r.getHeight());
                VFXTableCell<T> cell = r.getCells().get(cIndex);
                helper.layoutCell(layoutIndex, cell);
            });
            onLayoutCompleted(true);
            return;
        }

        // There are too many edge cases, taking into account all of them may degrade performance rather than improving it.
        // Leave the rest to the layout methods, columns and cells which are not visible will not be laid out (by default).
        if (helper.isLastColumn(column)) {
            layoutColumns();
            layoutRows();
            return;
        }

        // If it's VARIABLE mode, and it's not the last column, then it means we can actually do some optimization.
        // Rather than looping over all the columns, we just need to update those starting from the index that changed.
        ObservableList<VFXTableColumn<T, ? extends VFXTableCell<T>>> columns = table.getColumns();
        IntegerRange range = IntegerRange.of(cIndex, columns.size() - 1);
        range.forEach(i -> helper.layoutColumn(i, columns.get(i)));

        state.getRowsByIndex().values().forEach(r -> {
            r.resize(table.getVirtualMaxX(), r.getHeight());
            range.forEach(i -> helper.layoutCell(i, r.getCells().get(i)));
        });
        onLayoutCompleted(true);
    }

    /**
     * This must be called after processing a {@link ViewportLayoutRequest} to reset the {@link VFXTable#needsViewportLayoutProperty()}
     * to {@link ViewportLayoutRequest#NULL}.
     */
    protected void onLayoutCompleted(boolean done) {
        VFXTable<T> table = getSkinnable();
        table.setNeedsViewportLayout(ViewportLayoutRequest.NULL.setWasDone(done));
    }

    /**
     * This can be called during layout or other operations to update the given column's {@link VFXTableColumn#indexProperty()}
     * to the given index. This is indeed a strange place to do so, but as it turns out, layout methods are the most
     * reliable to ensure columns will always have the correct index.
     */
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
