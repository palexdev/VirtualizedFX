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
import io.github.palexdev.mfxcore.behavior.MFXBehavior;
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
import io.github.palexdev.virtualizedfx.properties.CellFactory;
import io.github.palexdev.virtualizedfx.properties.VFXTableStateProperty;
import io.github.palexdev.virtualizedfx.table.defaults.VFXDefaultTableRow;
import io.github.palexdev.virtualizedfx.table.VFXTableHelper.VFXDefaultTableHelper;
import io.github.palexdev.virtualizedfx.table.ViewportLayoutRequest.ViewportLayoutRequestProperty;
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

import static io.github.palexdev.mfxcore.controls.MFXStyleable.styleClasses;
import static io.github.palexdev.virtualizedfx.enums.ColumnsFillPolicy.*;
import static io.github.palexdev.virtualizedfx.utils.ScrollParams.cells;
import static io.github.palexdev.virtualizedfx.utils.ScrollParams.pixels;
import static java.util.Optional.ofNullable;

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

    private final VFXCellsCache<T, VFXTableRow<T>> rowsCache;
    private final CellFactory<T, VFXTableRow<T>> rowsFactory = new CellFactory<>(context);

    private final ObservableList<VFXTableColumn<T, ? extends VFXTableCell<T>>> columns = FXCollections.observableArrayList();

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
        initialize();
    }

    //================================================================================
    // Static Methods
    //================================================================================

    public static <T> int getWeight(VFXTableColumn<T, ?> column) {
        return (int) ofNullable(column.getProperties().get(WEIGHT_KEY)).orElse(DEFAULT_WEIGHT);
    }

    public static <T> void setWeight(VFXTableColumn<T, ?> column, int weight) {
        int curr = getWeight(column);
        if (curr == weight) return;

        column.getProperties().put(WEIGHT_KEY, weight);
        ofNullable(column.getTable())
            .map(VFXTable::getBehavior)
            .ifPresent(VFXTableManager::onWeightsChanged);
    }

    //================================================================================
    // Methods
    //================================================================================

    private void initialize() {
        setRowsFactory(defaultRowsFactory());
        setHelperFactory(defaultHelperFactory());

        columns.forEach(c -> c.setTable(this)); // init columns
        columns.addListener((ListChangeListener<? super VFXTableColumn<T, ? extends VFXTableCell<T>>>) this::onColumnsChanged);
    }

    protected VFXCellsCache<T, VFXTableRow<T>> createRowsCache() {
        return new VFXCellsCache<>(rowsFactory, getRowsCacheCapacity());
    }

    public Function<T, VFXTableRow<T>> defaultRowsFactory() {
        return VFXDefaultTableRow::new;
    }

    public Supplier<VFXTableHelper<T>> defaultHelperFactory() {
        return () -> new VFXDefaultTableHelper<>(this);
    }

    protected void updateState(VFXTableState<T> state) {
        setState(state);
        requestViewportLayout();
    }

    protected void updateState(VFXTableState<T> state, IntegerRange interval) {
        setState(state);
        requestViewportLayout(interval);
    }

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

        getBehavior().onColumnsChanged(from);
    }

    public void requestViewportLayout() {
        setNeedsViewportLayout(new ViewportLayoutRequest(0, Integer.MAX_VALUE));
    }

    protected void requestViewportLayout(IntegerRange interval) {
        setNeedsViewportLayout(Utils.INVALID_RANGE.equals(interval) ?
            ViewportLayoutRequest.Y_ONLY :
            new ViewportLayoutRequest(interval.getMin(), interval.getMax()));
    }

    // TODO autosize methods are removed for now to be superseded by AUTOSIZE_ONCE

    //================================================================================
    // Overridden Methods
    //================================================================================

    @Override
    public void update(int... indexes) {
        // TODO can we optimize this?
        // The first branch updates every row and cell, can't we fire a single event on the container and let it be
        // delivered to every cell in the scenegraph?
        //
        // As for the second branch, we can't do the exactly the same, but maybe we could fire the event on each row
        // rather than on each individual cell
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
        return () -> new VFXTableManager<>(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public VFXTableManager<T> getBehavior() {
        return (VFXTableManager<T>) super.getBehavior();
    }

    @Override
    public Supplier<MFXSkinBase<? extends Node>> defaultSkinFactory() {
        return () -> new VFXTableSkin<>(this);
    }

    @Override
    public List<String> defaultStyleClasses() {
        return styleClasses("vfx-table");
    }

    @Override
    public VFXScrollPane makeScrollable() {
        VFXScrollPane vsp = new VFXScrollPane(this);
        VFXScrollable.bindSpeed(vsp, cells(1), pixels(50.0));
        return vsp;
    }

    //================================================================================
    // Delegate Methods
    //================================================================================

    public VFXTable<T> populateRowsCache() {
        rowsCache.populate();
        return this;
    }

    public VFXTable<T> populateCellsCache() {
        columns.forEach(VFXTableColumn::populateCellsCache);
        return this;
    }

    public int rowsCacheSize() {
        return rowsCache.size();
    }

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

    public void scrollVerticalBy(double pixels) {
        getHelper().scrollBy(Orientation.VERTICAL, pixels);
    }

    public void scrollHorizontalBy(double pixels) {
        getHelper().scrollBy(Orientation.HORIZONTAL, pixels);
    }

    public void scrollToPixelVertical(double pixel) {
        getHelper().scrollToPixel(Orientation.VERTICAL, pixel);
    }

    public void scrollToPixelHorizontal(double pixel) {
        getHelper().scrollToPixel(Orientation.HORIZONTAL, pixel);
    }

    public void scrollToRow(int index) {
        getHelper().scrollToIndex(Orientation.VERTICAL, index);
    }

    public void scrollToColumn(int index) {
        getHelper().scrollToIndex(Orientation.HORIZONTAL, index);
    }

    public void scrollToFirstRow() {
        scrollToRow(0);
    }

    public void scrollToLastRow() {
        scrollToRow(size() - 1);
    }

    public void scrollToFirstColumn() {
        scrollToColumn(0);
    }

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
        Size.size(100.0, 32.0),
        // FIXME this should be moved in the manager, but MFXCore architecture present issues that prevent that
        _ -> getHelper().onColumnsSizeChanged()
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

    public StyleableDoubleProperty rowsHeightProperty() {
        return rowsHeight;
    }

    public void setRowsHeight(double rowsHeight) {
        this.rowsHeight.set(rowsHeight);
    }

    public BufferSize getRowsBufferSize() {
        return rowsBufferSize.get();
    }

    public StyleableObjectProperty<BufferSize> rowsBufferSizeProperty() {
        return rowsBufferSize;
    }

    public void setRowsBufferSize(BufferSize rowsBufferSize) {
        this.rowsBufferSize.set(rowsBufferSize);
    }

    public int getRowsCacheCapacity() {
        return rowsCacheCapacity.get();
    }

    public StyleableIntegerProperty rowsCacheCapacityProperty() {
        return rowsCacheCapacity;
    }

    public void setRowsCacheCapacity(int rowsCacheCapacity) {
        this.rowsCacheCapacity.set(rowsCacheCapacity);
    }

    public Size getColumnsSize() {
        return columnsSize.get();
    }

    public StyleableObjectProperty<Size> columnsSizeProperty() {
        return columnsSize;
    }

    public void setColumnsSize(Size columnsSize) {
        this.columnsSize.set(columnsSize);
    }

    public void setColumnsSize(double width, double height) {
        this.columnsSize.set(new Size(width, height));
    }

    public void setColumnsWidth(double width) {
        this.columnsSize.set(new Size(width, getColumnsSize().height()));
    }

    public void setColumnsHeight(double height) {
        this.columnsSize.set(new Size(getColumnsSize().width(), height));
    }

    public ColumnsFillPolicy getColumnsFillPolicy() {
        return columnsFillPolicy.get();
    }

    public StyleableObjectProperty<ColumnsFillPolicy> columnsFillPolicyProperty() {
        return columnsFillPolicy;
    }

    public void setColumnsFillPolicy(ColumnsFillPolicy columnsFillPolicy) {
        this.columnsFillPolicy.set(columnsFillPolicy);
    }

    public BufferSize getColumnsBufferSize() {
        return columnsBufferSize.get();
    }

    public StyleableObjectProperty<BufferSize> columnsBufferSizeProperty() {
        return columnsBufferSize;
    }

    public void setColumnsBufferSize(BufferSize columnsBufferSize) {
        this.columnsBufferSize.set(columnsBufferSize);
    }

    public double getClipBorderRadius() {
        return clipBorderRadius.get();
    }

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

    protected VFXCellsCache<T, VFXTableRow<T>> getRowsCache() {
        return rowsCache;
    }

    public Function<T, VFXTableRow<T>> getRowsFactory() {
        return rowsFactory.getValue();
    }

    public CellFactory<T, VFXTableRow<T>> rowsFactoryProperty() {
        return rowsFactory;
    }

    public void setRowsFactory(Function<T, VFXTableRow<T>> rowsFactory) {
        this.rowsFactory.setValue(rowsFactory);
    }

    public ObservableList<VFXTableColumn<T, ? extends VFXTableCell<T>>> columns() {
        return columns;
    }

    public VFXTableHelper<T> getHelper() {
        return helper.get();
    }

    public ReadOnlyObjectProperty<VFXTableHelper<T>> helperProperty() {
        return helper.getReadOnlyProperty();
    }

    public void setHelper(VFXTableHelper<T> helper) {
        this.helper.set(helper);
    }

    public Supplier<VFXTableHelper<T>> getHelperFactory() {
        return helperFactory.get();
    }

    public SupplierProperty<VFXTableHelper<T>> helperFactoryProperty() {
        return helperFactory;
    }

    public void setHelperFactory(Supplier<VFXTableHelper<T>> helperFactory) {
        this.helperFactory.set(helperFactory);
    }

    @Override
    public DoubleProperty vPosProperty() {
        return vPos;
    }

    @Override
    public DoubleProperty hPosProperty() {
        return hPos;
    }

    public VFXTableState<T> getState() {
        return state.get();
    }

    public ReadOnlyObjectProperty<VFXTableState<T>> stateProperty() {
        return state.getReadOnlyProperty();
    }

    protected void setState(VFXTableState<T> state) {
        this.state.set(state);
    }

    public boolean isNeedsViewportLayout() {
        return needsViewportLayout.isValid();
    }

    public ViewportLayoutRequest getViewportLayoutRequest() {
        return needsViewportLayout.get();
    }

    public ReadOnlyObjectProperty<ViewportLayoutRequest> needsViewportLayoutProperty() {
        return needsViewportLayout.getReadOnlyProperty();
    }

    protected void setNeedsViewportLayout(ViewportLayoutRequest needsViewportLayout) {
        this.needsViewportLayout.set(needsViewportLayout);
    }
}
