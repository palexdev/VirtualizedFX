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

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.base.properties.SizeProperty;
import io.github.palexdev.mfxcore.base.properties.functional.SupplierProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableDoubleProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableIntegerProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableObjectProperty;
import io.github.palexdev.mfxcore.collections.ObservableArrayList;
import io.github.palexdev.mfxcore.controls.MFXBehavior;
import io.github.palexdev.mfxcore.controls.MFXControl;
import io.github.palexdev.mfxcore.controls.MFXSkinBase;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.mfxcore.utils.fx.PropUtils;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import io.github.palexdev.virtualizedfx.base.VFXContainer;
import io.github.palexdev.virtualizedfx.base.VFXContext;
import io.github.palexdev.virtualizedfx.base.VFXScrollable;
import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.controls.VFXScrollPane;
import io.github.palexdev.virtualizedfx.enums.BufferSize;
import io.github.palexdev.virtualizedfx.enums.ColumnsFillPolicy;
import io.github.palexdev.virtualizedfx.events.VFXContainerEvent;
import io.github.palexdev.virtualizedfx.grid.VFXGrid;
import io.github.palexdev.virtualizedfx.list.VFXList;
import io.github.palexdev.virtualizedfx.properties.CellFactory;
import io.github.palexdev.virtualizedfx.properties.VFXTableStateProperty;
import io.github.palexdev.virtualizedfx.table.VFXTableHelper.VFXDefaultTableHelper;
import io.github.palexdev.virtualizedfx.table.ViewportLayoutRequest.ViewportLayoutRequestProperty;
import io.github.palexdev.virtualizedfx.table.defaults.VFXDefaultTableRow;
import io.github.palexdev.virtualizedfx.utils.ScrollParams;
import io.github.palexdev.virtualizedfx.utils.Utils;
import io.github.palexdev.virtualizedfx.utils.VFXCellsCache;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;

import static io.github.palexdev.mfxcore.controls.MFXStyleable.styleClasses;
import static io.github.palexdev.virtualizedfx.enums.ColumnsFillPolicy.*;
import static io.github.palexdev.virtualizedfx.utils.ScrollParams.cells;
import static io.github.palexdev.virtualizedfx.utils.ScrollParams.pixels;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/// Implementation of a virtualized container to show a list of items as tabular data.
/// The default style class is: '.vfx-table'.
///
/// Extends [MFXControl], implements [VFXContainer], has its own skin implementation [VFXTableSkin]
/// and a 'manager' [VFXTableManager]. Uses cells of type [VFXTableCell].
///
/// This is a stateful component, meaning that every meaningful variable (position, size, cell size, etc.) will produce a new
/// [VFXTableState] when changing. The state determines how and which items are displayed in the container.
///
/// ## Items, columns and rows
///
/// This container is a bit special because it's like a combination of both [VFXList] and [VFXGrid].
/// We are displaying data in two dimensions, just like the grid, because we have both the items and the columns.
/// However, each item occupies a row, just like in the list. Because of such nature, this component is also more complex
/// to use/setup. There are three core aspects:
///
/// 1) The columns: to display tabular data from a single object type, we usually want to divide such objects in data fragments,
///    where each type belongs to a specific category. This is the columns' role, and to do this, we have to introduce the second core aspect...
/// 2) Unlike the list and the grid, the cell factory is not a property of the container, rather, each column has its own.
///    This way, every column can build a cell that is going to extract the appropriate piece of data from the object type (e.g., User Object -> Name String).
///    The default cell implementations have specific properties to do exactly this, so you don't really need a different cell type for each column.
///    To be precise, you just need to set the appropriate function to extract the data and display it.
///    However, note that the container does not force you to use such defaults, in theory, you could come up with a totally different strategy if you like.
/// 3) The cells are not positioned directly into the viewport, rather, they are grouped in rows. So, you'll also need to specify a rows factory,
///    see [#rowsFactoryProperty()]. By default, [VFXDefaultTableRow] is used, you may want to extend such class and change the factory
///    to implement missing features. As for the reasons, there are fundamentally two:
///      - Selection: suppose you have a selection model, you click on a cell, and you now want the entire row to be highlighted.
///        I won't say it's impossible, but it's definitely not practical. You would have to manage every single cell in the row,
///        and it also makes it harder to style in CSS.
///      - The base class [VFXTableRow] actually implements methods that are crucial for the system to correctly manage the cells.
///        Such methods could, in theory, be placed elsewhere, but doing it like this makes everything cleaner and easier to handle.
///
/// Both lists are managed automatically, and both kinds of factory can be changed anytime, even at runtime.
///
/// ## Columns
///
/// The table's columns are in a [ColumnsList], see [#columns()]. There's no need to pass the table instance to the columns,
/// the reference is set automatically when they are added to/removed from the list.
///
/// Virtualized containers are super efficient mainly because every cell has a fixed size: it's easy to determine how many
/// cells we need, which items to display, and thus we just create and render the needed number of nodes. The point is,
/// usually, tables have fixed cell heights, but the width depends on the "parent" column. Also, they often offer the possibility
/// of resizing columns to fit the "children" cells' content, or even the possibility of resizing each column with the mouse.
/// In other words, to support such features the x-axis can't be virtualized by a simple multiplication anymore, since
/// every column may have a different width. So, the columns' positions are kept in a cache, [ColumnsLayoutCache], and the
/// x-axis is virtualized by looking up which columns fall in the viewport (backed by a binary search, `O(logn)`).
///
/// As for the columns' size:
/// - The [#columnsSizeProperty()] specifies the height of all the columns, and the minimum width each of them has.
/// - Each column can ask for more width, see [VFXTableColumn].
/// - When the columns don't cover the table's width, the space left is distributed among them as specified by the
///   [#columnsFillPolicyProperty()], see [ColumnsFillPolicy].
/// - Columns can be autosized to fit their content, all at once with [#autosizeColumns()], or one by one with
///   [VFXTableColumn#sizeToContent()].
///
/// ## State, manager and helper
///
/// The [VFXTableManager] is responsible for reacting to core changes in the functionalities defined here to produce a new state.
/// It is built by [#createManager()] along with the table, and lives as long as the table does.
///
/// The state can be considered as a 'picture' of the container at a certain time. Each combination of the variables that
/// influence the way items are shown (how many, start, end, changes in the list, etc.) will produce a specific state.
/// Because of how the table is designed, there are actually two kinds of states here. The first one is common to all
/// virtualized containers, and represents the overall state of the component, for the table it's the [VFXTableState]
/// class. The other one is specific to the table, and it's actually the [VFXTableRow] class itself.<br >
/// The global state tells how many rows should be present in the viewport, which items from the list (rows range)
/// and which "data fragments" (columns range) to display, but then each row has its sub-state, they keep the cells' list,
/// the columns range, and are responsible for updating the cells when needed as well as positioning and sizing them.
///
/// You can access the current state through the [#stateProperty()]. If you'd like to observe for changes in the displayed
/// items, then you want to add a listener on this property.
///
/// Core computations such as the range of rows, the range of columns, the estimated size, the layout of cells, etc.,
/// are delegated to a separate 'helper' class which is the [VFXTableHelper]. You are allowed to change the helper
/// through the [#helperFactoryProperty()].
///
/// Of course, you are free to customize pretty much all of these mechanisms, BUT, beware, VFX components are no joke,
/// they are complex, make sure to thoroughly read the documentation before!
///
/// ## Layout
///
/// Just like the other virtualized containers, the viewport's layout is requested through a read-only property,
/// [#needsViewportLayoutProperty()]. Here, the request is a [ViewportLayoutRequest] rather than a boolean value, because
/// it can ask for only a portion of the layout to be computed. Requests are handled automatically, but you can force a
/// full layout with [#requestViewportLayout()], although this should never be necessary.
///
/// ## Scrolling
///
/// - The vertical and horizontal positions are available through the properties [#vPosProperty()] and [#hPosProperty()].
///   It could indeed be possible to use a single property for the position, but they are split for performance reasons.
/// - The virtual bounds of the container are given by the [#virtualMaxXProperty()] and the [#virtualMaxYProperty()],
///   the total number of pixels on the x-axis and on the y-axis.
/// - Just like the list and the grid, the table makes use of buffers to render a couple more rows and columns to make
///   the scrolling smoother. There are two buffers, one for the rows [#rowsBufferSizeProperty()] and one for the
///   columns [#columnsBufferSizeProperty()].
///
/// ## Caches
///
/// Also, just like the list and the grid, the table makes use of caches to store rows and cells that are not needed
/// anymore but could be used again in the future. One cache is here and is responsible for storing rows,
/// the other is in each [VFXTableColumn] and is responsible for storing cells (since every column has its own cell factory).
/// You can control the caches' capacity by the following properties: [#rowsCacheCapacityProperty()], [VFXTableColumn#cellsCacheCapacityProperty()].
///
/// Depending on the table's size, the default capacities may be too small. You can play around with the values and see
/// if there's any benefit to performance. The caches can also be filled in advance, see [#populateRowsCache()] and
/// [#populateCellsCache()].
public class VFXTable<T> extends MFXControl implements VFXContainer<T>, VFXScrollable {

    //================================================================================
    // Properties
    //================================================================================

    private final VFXContext<T> context = new VFXContext<>(this);

    private final ListProperty<T> items = new SimpleListProperty<>() {
        @Override
        public void set(ObservableList<T> newValue) {
            if (newValue == null) newValue = FXCollections.observableArrayList();
            super.set(newValue);
        }
    };

    private final ColumnsList<T> columns = new ColumnsList<>();
    private final VFXCellsCache<T, VFXTableRow<T>> rowsCache;
    private final CellFactory<T, VFXTableRow<T>> rowsFactory = new CellFactory<>(context);

    private VFXTableManager<T> manager;
    private final ReadOnlyObjectWrapper<VFXTableHelper<T>> helper = new ReadOnlyObjectWrapper<>() {
        @Override
        public void set(VFXTableHelper<T> newValue) {
            if (newValue == null)
                throw new NullPointerException("Table helper cannot be null!");
            VFXTableHelper<T> old = get();
            if (old != null) old.dispose();
            super.set(newValue);
        }
    };
    private final SupplierProperty<VFXTableHelper<T>> helperFactory = new SupplierProperty<>() {
        @Override
        public void set(Supplier<VFXTableHelper<T>> newValue) {
            if (newValue == null)
                throw new NullPointerException("Helper helper factory cannot be null!");
            super.set(newValue);
        }

        @Override
        protected void invalidated() {
            setHelper(get().get());
        }
    };

    private final DoubleProperty vPos = PropUtils.doubleProperty()
        .mapper(val -> NumberUtils.clamp(val, 0.0, getMaxVScroll()))
        .build();
    private final DoubleProperty hPos = PropUtils.doubleProperty()
        .mapper(val -> NumberUtils.clamp(val, 0.0, getMaxHScroll()))
        .build();

    @SuppressWarnings({"rawtypes", "unchecked"})
    private final VFXTableStateProperty<T> state = new VFXTableStateProperty<>(VFXTableState.INVALID);
    private final ViewportLayoutRequestProperty needsViewportLayout = new ViewportLayoutRequestProperty();

    //================================================================================
    // Constructors
    //================================================================================

    public VFXTable() {
        this(FXCollections.observableArrayList());
    }

    public VFXTable(ObservableList<T> items) {
        this(items, Collections.emptyList());
    }

    public VFXTable(ObservableList<T> items, Collection<VFXTableColumn<T, ? extends VFXTableCell<T>>> columns) {
        setItems(items);
        this.columns.setAll(columns);
        rowsCache = createRowsCache();
        init();
    }

    //================================================================================
    // Static Methods
    //================================================================================

    /// @return the given column's fill weight, or [ColumnsFillPolicy#DEFAULT_WEIGHT] if none was set.
    /// See [#setWeight(VFXTableColumn,int)].
    public static <T> int getWeight(VFXTableColumn<T, ?> column) {
        if (!column.hasProperties()) return DEFAULT_WEIGHT;
        return (int) column.getProperties().getOrDefault(WEIGHT_KEY, DEFAULT_WEIGHT);
    }

    /// Sets the given column's fill weight, which is stored in the column's properties map under [ColumnsFillPolicy#WEIGHT_KEY].
    ///
    /// See [ColumnsFillPolicy] for how weights are used. If the column is in a table, its manager is notified,
    /// see [VFXTableManager#onColumnWeightChanged(VFXTableColumn)].
    public static <T> void setWeight(VFXTableColumn<T, ?> column, int weight) {
        int curr = getWeight(column);
        if (curr == weight) return;

        column.getProperties().put(WEIGHT_KEY, weight);
        ofNullable(column.getTable())
            .map(VFXTable::getManager)
            .ifPresent(m -> m.onColumnWeightChanged(column));
    }

    //================================================================================
    // Methods
    //================================================================================

    /// Initializes the table: sets the default rows and helper factories, builds and installs the [VFXTableManager]
    /// (see [#createManager()]), gives the initial columns their table reference and index, and finally
    /// starts listening for changes in the columns' list, see [#onColumnsChanged(ListChangeListener.Change)].
    private void init() {
        setRowsFactory(defaultRowsFactory());
        setHelperFactory(defaultHelperFactory());

        manager = requireNonNull(createManager(), "Table's manager cannot be null!");
        manager.install();

        // Init columns
        for (int i = 0; i < columns.size(); i++) {
            VFXTableColumn<T, ? extends VFXTableCell<T>> column = columns.get(i);
            column.setTable(this);
            column.hintIndex(i);
        }
        columns.addListener((ListChangeListener<? super VFXTableColumn<T, ? extends VFXTableCell<T>>>) this::onColumnsChanged);
    }

    /// @return the default function used to build a [VFXTableHelper]. Uses [VFXDefaultTableHelper].
    public Supplier<VFXTableHelper<T>> defaultHelperFactory() {
        return () -> new VFXDefaultTableHelper<>(this);
    }

    /// Responsible for creating the table's [VFXTableManager]. Called only once, when the table is built, and
    /// the result cannot be `null`.
    ///
    /// Override this to use a custom manager. Beware, it runs during the table's construction, so it must not depend on
    /// state initialized in a subclass' constructor.
    protected VFXTableManager<T> createManager() {
        return new VFXTableManager<>(this);
    }

    /// @return the default function used to build rows. Uses [VFXDefaultTableRow].
    public Function<T, VFXTableRow<T>> defaultRowsFactory() {
        return VFXDefaultTableRow::new;
    }

    /// Responsible for creating the rows' cache instance used by this container.
    ///
    /// @see VFXCellsCache
    /// @see #rowsCacheCapacityProperty()
    protected VFXCellsCache<T, VFXTableRow<T>> createRowsCache() {
        return new VFXCellsCache<>(rowsFactory, getRowsCacheCapacity());
    }

    /// Sets the given state and requests a full viewport layout, see [#requestViewportLayout()].
    protected void updateState(VFXTableState<T> state) {
        setState(state);
        requestViewportLayout();
    }

    /// Sets the given state and requests a layout of the given columns interval only,
    /// see [#requestViewportLayout(IntegerRange)].
    protected void updateState(VFXTableState<T> state, IntegerRange interval) {
        setState(state);
        requestViewportLayout(interval);
    }

    /// Handles changes in the columns' list. First, the helper is notified so that its cache is up to date,
    /// see [VFXTableHelper#onColumnsChanged(ListChangeListener.Change)]. Then, the table reference is set on added columns
    /// and removed from removed ones. A column both removed and added by the same change (e.g. a `setAll` that keeps some
    /// of the old columns) is simply left as it is. Finally, the columns' indexes are hinted from the first changed index
    /// on, and the manager is notified, see [VFXTableManager#onColumnsChanged(int)].
    protected void onColumnsChanged(ListChangeListener.Change<? extends VFXTableColumn<T, ? extends VFXTableCell<T>>> c) {
        getHelper().onColumnsChanged(c);

        c.reset();
        // A setAll operation may end up adding the same columns as before (or even just some of them)
        // Which means that both wasRemoved and wasAdded computation will run, we don't want that here.
        // Simply handle removals after ensuring that a column that "was removed" is not still in the list
        Set<VFXTableColumn<T, ?>> rm = new HashSet<>();
        // Find the smallest change.getFrom() index from which to invalidate the layout later
        int from = Integer.MAX_VALUE;
        while (c.next()) {
            from = Math.min(from, c.getFrom());
            if (c.wasRemoved()) rm.addAll(c.getRemoved());
            if (c.wasAdded()) {
                for (VFXTableColumn<T, ?> column : c.getAddedSubList()) {
                    if (rm.contains(column)) {
                        rm.remove(column);
                        continue;
                    }
                    column.setTable(this);
                }
            }
        }
        rm.forEach(column -> column.setTable(null));
        for (int i = from; i < columns.size(); i++) columns.get(i).hintIndex(i);

        manager.onColumnsChanged(from);
    }

    /// Setter for the [#needsViewportLayoutProperty()].<br >
    /// This sets the property to a new [ViewportLayoutRequest], causing the default skin to recompute the entire layout.
    public void requestViewportLayout() {
        setNeedsViewportLayout(new ViewportLayoutRequest(0, Integer.MAX_VALUE));
    }

    /// Setter for the [#needsViewportLayoutProperty()].<br >
    /// This sets the property to a new [ViewportLayoutRequest] with the given columns interval, causing the default skin to
    /// recompute only a portion of the layout. An invalid interval ([Utils#INVALID_RANGE]) means no column needs it,
    /// and the request becomes [ViewportLayoutRequest#Y_ONLY].
    protected void requestViewportLayout(IntegerRange interval) {
        setNeedsViewportLayout(Utils.INVALID_RANGE.equals(interval) ?
            ViewportLayoutRequest.Y_ONLY :
            new ViewportLayoutRequest(interval.getMin(), interval.getMax()));
    }

    /// Marks all the table's columns for autosize, and requests a layout so that the skin can process them.
    /// See [VFXTableColumn#sizeToContent()] for how and when the marks are processed.
    public void autosizeColumns() {
        columns.forEach(VFXTableColumn::markForAutosize);
        requestLayout();
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    /// {@inheritDoc}
    ///
    /// Note that this may be a costly operation due to nested loops. Since cells are inside rows we must first iterate
    /// over the rows, then iterate on each of their cells and fire an update event on each of them.
    @Override
    public void update(int... indexes) {
        VFXTableState<T> state = getState();
        if (state.isEmpty()) return;
        if (indexes.length == 0) {
            state.getRowsByIndex().values().forEach(r ->
                r.cellsByIndex().values().forEach(VFXContainerEvent::update));
            return;
        }

        for (int index : indexes) {
            VFXTableRow<T> row = state.getRowsByIndex().get(index);
            if (row == null) continue;
            row.cellsByIndex().values().forEach(VFXContainerEvent::update);
        }
    }

    @Override
    public Supplier<MFXBehavior<? extends Node>> defaultBehaviorFactory() {
        return () -> new MFXBehavior<>(this) {};
    }

    @Override
    public Supplier<MFXSkinBase<? extends Node>> defaultSkinFactory() {
        return () -> new VFXTableSkin<>(this);
    }

    @Override
    public List<String> defaultStyleClasses() {
        return styleClasses("vfx-table");
    }

    /// {@inheritDoc}
    ///
    /// The vertical scroll speed is bound to one row per unit, the horizontal one to 50 pixels per unit.
    /// See [ScrollParams].
    @Override
    public VFXScrollPane makeScrollable() {
        VFXScrollPane vsp = new VFXScrollPane(this);
        VFXScrollable.bindSpeed(vsp, cells(1), pixels(50.0));
        return vsp;
    }

    //================================================================================
    // Delegate Methods
    //================================================================================

    /// Delegate for [VFXCellsCache#populate()] (on the rows' cache).
    ///
    /// @see #populateCellsCache()
    public VFXTable<T> populateRowsCache() {
        rowsCache.populate();
        return this;
    }

    /// Populates all the table's columns' caches, see [VFXTableColumn#populateCellsCache()].
    ///
    /// @see #populateRowsCache()
    public VFXTable<T> populateCellsCache() {
        columns.forEach(VFXTableColumn::populateCellsCache);
        return this;
    }

    /// Delegate for [VFXCellsCache#size()] (on the rows' cache).
    public int rowsCacheSize() {
        return rowsCache.size();
    }

    /// @return the total number of cached cells by iterating over [#columns()].
    public int cellsCacheSize() {
        return columns.stream()
            .mapToInt(VFXTableColumn::cacheSize)
            .sum();
    }

    /// Delegate for [VFXTableState#getRowsRange()]
    public IntegerRange getRowsRange() {
        return getState().getRowsRange();
    }

    /// Delegate for [VFXTableState#getColumnsRange()]
    public IntegerRange getColumnsRange() {
        return getState().getColumnsRange();
    }

    /// Delegate for [VFXTableState#getRowsByIndex()]
    public SequencedMap<Integer, VFXTableRow<T>> getRowsByIndex() {
        return getState().getRowsByIndex();
    }

    /// Delegate for [VFXTableState#getRowsByItem()]
    public List<Map.Entry<T, VFXTableRow<T>>> getRowsByItem() {
        return getState().getRowsByItem();
    }

    /// Delegate for [VFXTableHelper#virtualMaxXProperty()]
    @Override
    public ReadOnlyDoubleProperty virtualMaxXProperty() {
        return getHelper().virtualMaxXProperty();
    }

    /// Delegate for [VFXTableHelper#virtualMaxYProperty()]
    @Override
    public ReadOnlyDoubleProperty virtualMaxYProperty() {
        return getHelper().virtualMaxYProperty();
    }

    /// Delegate for [VFXTableHelper#maxVScrollProperty()].
    @Override
    public ReadOnlyDoubleProperty maxVScrollProperty() {
        return getHelper().maxVScrollProperty();
    }

    /// Delegate for [VFXTableHelper#maxHScrollProperty()].
    @Override
    public ReadOnlyDoubleProperty maxHScrollProperty() {
        return getHelper().maxHScrollProperty();
    }

    /// {@inheritDoc}
    ///
    /// For the table this is a delegate to [#rowsBufferSizeProperty()], so that it can honor the
    /// [VFXContainer] API. There is no single 'buffer size' here, the two axes have their own,
    /// see also [#columnsBufferSizeProperty()].
    @Override
    public StyleableObjectProperty<BufferSize> bufferSizeProperty() {
        return rowsBufferSize;
    }

    /// Delegate for [VFXTableHelper#scrollBy(Orientation, double)] with vertical orientation as parameter.
    public void scrollVerticalBy(double pixels) {
        getHelper().scrollBy(Orientation.VERTICAL, pixels);
    }

    /// Delegate for [VFXTableHelper#scrollBy(Orientation, double)] with horizontal orientation as parameter.
    public void scrollHorizontalBy(double pixels) {
        getHelper().scrollBy(Orientation.HORIZONTAL, pixels);
    }

    /// Delegate for [VFXTableHelper#scrollToPixel(Orientation, double)] with vertical orientation as parameter.
    public void scrollToPixelVertical(double pixel) {
        getHelper().scrollToPixel(Orientation.VERTICAL, pixel);
    }

    /// Delegate for [VFXTableHelper#scrollToPixel(Orientation, double)] with horizontal orientation as parameter.
    public void scrollToPixelHorizontal(double pixel) {
        getHelper().scrollToPixel(Orientation.HORIZONTAL, pixel);
    }

    /// Delegate for [VFXTableHelper#scrollToIndex(Orientation, int)] with vertical orientation as parameter.
    public void scrollToRow(int index) {
        getHelper().scrollToIndex(Orientation.VERTICAL, index);
    }

    /// Delegate for [VFXTableHelper#scrollToIndex(Orientation, int)] with horizontal orientation as parameter.
    public void scrollToColumn(int index) {
        getHelper().scrollToIndex(Orientation.HORIZONTAL, index);
    }

    /// Delegate for [#scrollToRow(int)] with 0 as parameter.
    public void scrollToFirstRow() {
        scrollToRow(0);
    }

    /// Delegate for [#scrollToRow(int)] with `size() - 1` as parameter.
    public void scrollToLastRow() {
        scrollToRow(size() - 1);
    }

    /// Delegate for [#scrollToColumn(int)] with 0 as parameter.
    public void scrollToFirstColumn() {
        scrollToColumn(0);
    }

    /// Delegate for [#scrollToColumn(int)] with `columns.size() - 1` as parameter.
    public void scrollToLastColumn() {
        scrollToColumn(columns.size() - 1);
    }

    //================================================================================
    // Styleable Properties
    //================================================================================

    private final StyleableDoubleProperty rowsHeight = new StyleableDoubleProperty(
        StyleableProperties.ROWS_HEIGHT,
        this,
        "rowsHeight",
        32.0
    );

    private final StyleableObjectProperty<BufferSize> rowsBufferSize = new StyleableObjectProperty<>(
        StyleableProperties.ROWS_BUFFER_SIZE,
        this,
        "rowsBufferSize",
        BufferSize.standard()
    );

    private final StyleableIntegerProperty rowsCacheCapacity = new StyleableIntegerProperty(
        StyleableProperties.ROWS_CACHE_CAPACITY,
        this,
        "rowsCacheCapacity",
        10
    ) {
        @Override
        protected void invalidated() {
            int capacity = get();
            if (capacity < 0) throw new IllegalArgumentException("Cache capacity cannot be negative!");
            rowsCache.setCapacity(capacity);
        }
    };

    private final StyleableObjectProperty<Size> columnsSize = SizeProperty.styleableProperty(
        StyleableProperties.COLUMNS_SIZE,
        this,
        "columnsSize",
        Size.size(100.0, 32.0)
    );

    private final StyleableObjectProperty<ColumnsFillPolicy> columnsFillPolicy = new StyleableObjectProperty<>(
        StyleableProperties.COLUMNS_FILL_POLICY,
        this,
        "columnsFillPolicy",
        DEFAULT_POLICY
    );

    private final StyleableObjectProperty<BufferSize> columnsBufferSize = new StyleableObjectProperty<>(
        StyleableProperties.COLUMNS_BUFFER_SIZE,
        this,
        "columnsBufferSize",
        BufferSize.standard()
    );

    private final StyleableDoubleProperty clipBorderRadius = new StyleableDoubleProperty(
        StyleableProperties.CLIP_BORDER_RADIUS,
        this,
        "clipBorderRadius",
        0.0
    );

    public double getRowsHeight() {
        return rowsHeight.get();
    }

    /// Specifies the fixed height for all the table's rows.
    ///
    /// Note that the default [VFXTableHelper] implementation will also set the cells' height to this value,
    /// however you can modify such behavior if needed by providing your custom implementation through the
    /// [#helperFactoryProperty()].
    ///
    /// Can be set in CSS via the property: '-vfx-rows-height'.
    public StyleableDoubleProperty rowsHeightProperty() {
        return rowsHeight;
    }

    public void setRowsHeight(double rowsHeight) {
        this.rowsHeight.set(rowsHeight);
    }

    public BufferSize getRowsBufferSize() {
        return rowsBufferSize.get();
    }

    /// Specifies the number of extra rows to add to the viewport to make scrolling smoother.
    /// See also [VFXContainer#bufferSizeProperty()] and [VFXTableHelper#totalRows()].
    ///
    /// Can be set in CSS via the property: '-vfx-rows-buffer-size'.
    public StyleableObjectProperty<BufferSize> rowsBufferSizeProperty() {
        return rowsBufferSize;
    }

    public void setRowsBufferSize(BufferSize rowsBufferSize) {
        this.rowsBufferSize.set(rowsBufferSize);
    }

    public int getRowsCacheCapacity() {
        return rowsCacheCapacity.get();
    }

    /// Specifies the maximum number of rows the cache can contain at any time. Excess will not be added to the queue and
    /// disposed immediately.
    ///
    /// Can be set in CSS via the property: '-vfx-rows-cache-capacity'.
    public StyleableIntegerProperty rowsCacheCapacityProperty() {
        return rowsCacheCapacity;
    }

    public void setRowsCacheCapacity(int rowsCacheCapacity) {
        this.rowsCacheCapacity.set(rowsCacheCapacity);
    }

    public Size getColumnsSize() {
        return columnsSize.get();
    }

    /// Specifies the columns' size as a [Size] object.
    ///
    /// The width is the **minimum** width all columns must have: a column's natural width is the greater between this and
    /// its [VFXTableColumn#userPrefWidthProperty()]. The height is the height of all the columns.
    /// This behavior can also be modified as it is defined by the default [VFXTableHelper] implementation.
    ///
    /// Can be set in CSS via the property: '-vfx-columns-size'.
    public StyleableObjectProperty<Size> columnsSizeProperty() {
        return columnsSize;
    }

    public void setColumnsSize(Size columnsSize) {
        this.columnsSize.set(columnsSize);
    }

    /// Convenience method to create a new [Size] object and set the [#columnsSizeProperty()].
    public void setColumnsSize(double width, double height) {
        this.columnsSize.set(new Size(width, height));
    }

    /// Convenience method to create a new [Size] object and set the [#columnsSizeProperty()].
    /// The old height will be kept.
    public void setColumnsWidth(double width) {
        this.columnsSize.set(new Size(width, getColumnsSize().height()));
    }

    /// Convenience method to create a new [Size] object and set the [#columnsSizeProperty()].
    /// The old width will be kept.
    public void setColumnsHeight(double height) {
        this.columnsSize.set(new Size(getColumnsSize().width(), height));
    }

    public ColumnsFillPolicy getColumnsFillPolicy() {
        return columnsFillPolicy.get();
    }

    /// Specifies how the leftover width, the table's width not covered by the columns' natural widths, is
    /// distributed among the columns. See [ColumnsFillPolicy].
    ///
    /// Can be set in CSS via the property: '-vfx-columns-fill-policy'.
    public StyleableObjectProperty<ColumnsFillPolicy> columnsFillPolicyProperty() {
        return columnsFillPolicy;
    }

    public void setColumnsFillPolicy(ColumnsFillPolicy columnsFillPolicy) {
        this.columnsFillPolicy.set(columnsFillPolicy);
    }

    public BufferSize getColumnsBufferSize() {
        return columnsBufferSize.get();
    }

    /// Specifies the number of extra columns to add to the viewport to make scrolling smoother.
    /// See also [VFXContainer#bufferSizeProperty()] and [VFXTableHelper#totalColumns()].
    ///
    /// Can be set in CSS via the property: '-vfx-columns-buffer-size'.
    public StyleableObjectProperty<BufferSize> columnsBufferSizeProperty() {
        return columnsBufferSize;
    }

    public void setColumnsBufferSize(BufferSize columnsBufferSize) {
        this.columnsBufferSize.set(columnsBufferSize);
    }

    public double getClipBorderRadius() {
        return clipBorderRadius.get();
    }

    /// Used by the viewport's clip to set its border radius.
    /// This is useful when you want to make a rounded container, this prevents the content from going outside the view.
    ///
    /// **Side note:** the clip is a [Rectangle], now for some fucking reason, the rectangle's arcWidth and arcHeight
    /// values used to make it round do not act like the border-radius or background-radius properties,
    /// instead their value is usually 2 / 2.5 times the latter.
    /// So, for a border radius of 5, you want this value to be at least 10/13.
    ///
    /// Can be set in CSS via the property: '-vfx-clip-border-radius'.
    public StyleableDoubleProperty clipBorderRadiusProperty() {
        return clipBorderRadius;
    }

    public void setClipBorderRadius(double clipBorderRadius) {
        this.clipBorderRadius.set(clipBorderRadius);
    }

    //================================================================================
    // CssMetaData
    //================================================================================

    private static class StyleableProperties {
        private static final StyleablePropertyFactory<VFXTable<?>> FACTORY = new StyleablePropertyFactory<>(MFXControl.getClassCssMetaData());
        private static final List<CssMetaData<? extends Styleable, ?>> cssMetaDataList;

        private static final CssMetaData<VFXTable<?>, Number> ROWS_HEIGHT =
            FACTORY.createSizeCssMetaData(
                "-vfx-rows-height",
                VFXTable::rowsHeightProperty,
                32.0
            );

        private static final CssMetaData<VFXTable<?>, BufferSize> ROWS_BUFFER_SIZE =
            FACTORY.createEnumCssMetaData(
                BufferSize.class,
                "-vfx-rows-buffer-size",
                VFXTable::rowsBufferSizeProperty,
                BufferSize.standard()
            );

        private static final CssMetaData<VFXTable<?>, Number> ROWS_CACHE_CAPACITY =
            FACTORY.createSizeCssMetaData(
                "-vfx-rows-cache-capacity",
                VFXTable::rowsCacheCapacityProperty,
                10
            );

        private static final CssMetaData<VFXTable<?>, Size> COLUMNS_SIZE =
            SizeProperty.cssMetaData(
                "-vfx-columns-size",
                VFXTable::columnsSizeProperty,
                Size.size(100, 32)
            );

        private static final CssMetaData<VFXTable<?>, ColumnsFillPolicy> COLUMNS_FILL_POLICY =
            FACTORY.createEnumCssMetaData(
                ColumnsFillPolicy.class,
                "-vfx-columns-fill-policy",
                VFXTable::columnsFillPolicyProperty,
                DEFAULT_POLICY
            );

        private static final CssMetaData<VFXTable<?>, BufferSize> COLUMNS_BUFFER_SIZE =
            FACTORY.createEnumCssMetaData(
                BufferSize.class,
                "-vfx-columns-buffer-size",
                VFXTable::columnsBufferSizeProperty,
                BufferSize.standard()
            );

        private static final CssMetaData<VFXTable<?>, Number> CLIP_BORDER_RADIUS =
            FACTORY.createSizeCssMetaData(
                "-vfx-clip-border-radius",
                VFXTable::clipBorderRadiusProperty,
                0.0
            );

        static {
            cssMetaDataList = StyleUtils.cssMetaDataList(
                MFXControl.getClassCssMetaData(),
                ROWS_HEIGHT, ROWS_BUFFER_SIZE, ROWS_CACHE_CAPACITY,
                COLUMNS_SIZE, COLUMNS_FILL_POLICY, COLUMNS_BUFFER_SIZE,
                CLIP_BORDER_RADIUS
            );
        }
    }

    @Override
    protected List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return getClassCssMetaData();
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.cssMetaDataList;
    }

    //================================================================================
    // Getters/Setters
    //================================================================================

    @Override
    public VFXContext<T> context() {
        return context;
    }

    @Override
    public ListProperty<T> itemsProperty() {
        return items;
    }

    /// @return the rows' cache instance used by this container
    protected VFXCellsCache<T, VFXTableRow<T>> getRowsCache() {
        return rowsCache;
    }

    public Function<T, VFXTableRow<T>> getRowsFactory() {
        return rowsFactory.getValue();
    }

    /// Specifies the function used to build the table's rows.
    /// See also [#defaultRowsFactory()].
    public CellFactory<T, VFXTableRow<T>> rowsFactoryProperty() {
        return rowsFactory;
    }

    public void setRowsFactory(Function<T, VFXTableRow<T>> rowsFactory) {
        this.rowsFactory.setValue(rowsFactory);
    }

    /// This is the observable list containing all the table's columns. See [ColumnsList].
    public ColumnsList<T> columns() {
        return columns;
    }

    public VFXTableHelper<T> getHelper() {
        return helper.get();
    }

    /// Specifies the instance of the [VFXTableHelper] built by the [#helperFactoryProperty()].
    ///
    /// When replaced, the old helper is disposed. The helper cannot be `null`.
    public ReadOnlyObjectProperty<VFXTableHelper<T>> helperProperty() {
        return helper.getReadOnlyProperty();
    }

    /// Sets the helper directly, bypassing the [#helperFactoryProperty()]. The old helper is disposed.
    protected void setHelper(VFXTableHelper<T> helper) {
        this.helper.set(helper);
    }

    public Supplier<VFXTableHelper<T>> getHelperFactory() {
        return helperFactory.get();
    }

    /// Specifies the function used to build a [VFXTableHelper] instance. Every time it changes, a new helper is built
    /// and replaces the current one. The function cannot be `null`. See also [#defaultHelperFactory()].
    public SupplierProperty<VFXTableHelper<T>> helperFactoryProperty() {
        return helperFactory;
    }

    public void setHelperFactory(Supplier<VFXTableHelper<T>> helperFactory) {
        this.helperFactory.set(helperFactory);
    }

    /// @return the table's [VFXTableManager], see [#createManager()]
    protected VFXTableManager<T> getManager() {
        return manager;
    }

    /// {@inheritDoc}
    ///
    /// The value is clamped between 0 and [#getMaxVScroll()].
    @Override
    public DoubleProperty vPosProperty() {
        return vPos;
    }

    /// {@inheritDoc}
    ///
    /// The value is clamped between 0 and [#getMaxHScroll()].
    @Override
    public DoubleProperty hPosProperty() {
        return hPos;
    }

    public VFXTableState<T> getState() {
        return state.get();
    }

    /// Specifies the container's current state. The state carries useful information such as the range of rows and columns
    /// and the rows ordered by index, or by item (not ordered).
    public ReadOnlyObjectProperty<VFXTableState<T>> stateProperty() {
        return state.getReadOnlyProperty();
    }

    protected void setState(VFXTableState<T> state) {
        this.state.set(state);
    }

    /// Delegate for [ViewportLayoutRequest#isValid()].
    public boolean isNeedsViewportLayout() {
        return needsViewportLayout.isValid();
    }

    public ViewportLayoutRequest getViewportLayoutRequest() {
        return needsViewportLayout.get();
    }

    /// Specifies whether the viewport needs to compute the layout of its content.
    ///
    /// Since this is read-only, layout requests must be sent by using [#requestViewportLayout()].
    public ReadOnlyObjectProperty<ViewportLayoutRequest> needsViewportLayoutProperty() {
        return needsViewportLayout.getReadOnlyProperty();
    }

    protected void setNeedsViewportLayout(ViewportLayoutRequest needsViewportLayout) {
        this.needsViewportLayout.set(needsViewportLayout);
    }

    //================================================================================
    // Inner Classes
    //================================================================================

    /// The list holding the table's columns, an [ObservableArrayList] with two extra operations to re-order the columns:
    /// [#swap(int,int)] and [#move(int,int)]. Each fires a single permutation change, which is the cheapest change for the
    /// table to handle, rather than the removal plus addition that you would get from a `remove` followed by an `add`.
    ///
    /// Since columns are nodes, the list cannot contain duplicates. That's why re-ordering through these methods is the
    /// way to go.
    ///
    /// To sort the columns, use this list's `sort(...)` rather than [FXCollections#sort(ObservableList)], only the former
    /// reports the whole operation as a single permutation.
    public static class ColumnsList<T> extends ObservableArrayList<VFXTableColumn<T, ? extends VFXTableCell<T>>> {

        /// Swaps the columns at the given indexes, firing a single permutation change.
        ///
        /// @throws IndexOutOfBoundsException if either index is out of bounds
        public void swap(int i, int j) {
            Objects.checkIndex(i, size());
            Objects.checkIndex(j, size());
            if (i == j) return;

            int lo = Math.min(i, j);
            int hi = Math.max(i, j);
            int[] perm = new int[hi - lo + 1];
            for (int k = 0; k < perm.length; k++) perm[k] = lo + k;
            perm[0] = hi;
            perm[perm.length - 1] = lo;

            beginChange();
            try {
                Collections.swap(delegate, lo, hi);
                nextPermutation(lo, hi + 1, perm);
            } finally {
                endChange();
            }
        }

        /// Moves the column at index `from` to index `to`, shifting the columns in between by one.
        /// Fires a single permutation change.
        ///
        /// @throws IndexOutOfBoundsException if either index is out of bounds
        public void move(int from, int to) {
            Objects.checkIndex(from, size());
            Objects.checkIndex(to, size());
            if (from == to) return;

            int lo = Math.min(from, to);
            int hi = Math.max(from, to);
            int[] perm = new int[hi - lo + 1];
            if (from < to) {
                perm[0] = to;
                for (int k = 1; k < perm.length; k++) perm[k] = lo + k - 1;
            } else {
                for (int k = 0; k < perm.length - 1; k++) perm[k] = lo + k + 1;
                perm[perm.length - 1] = to;
            }

            beginChange();
            try {
                delegate.add(to, delegate.remove(from));
                nextPermutation(lo, hi + 1, perm);
            } finally {
                endChange();
            }
        }
    }
}
