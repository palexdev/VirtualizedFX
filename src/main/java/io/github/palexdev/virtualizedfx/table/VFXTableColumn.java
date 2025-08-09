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

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.palexdev.mfxcore.base.properties.styleable.StyleableBooleanProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableIntegerProperty;
import io.github.palexdev.mfxcore.controls.Labeled;
import io.github.palexdev.mfxcore.controls.MFXStyleable;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import io.github.palexdev.virtualizedfx.base.VFXContext;
import io.github.palexdev.virtualizedfx.base.WithCellFactory;
import io.github.palexdev.virtualizedfx.cells.VFXSimpleTableCell;
import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.enums.ColumnsLayoutMode;
import io.github.palexdev.virtualizedfx.properties.CellFactory;
import io.github.palexdev.virtualizedfx.table.defaults.VFXTableColumnBehavior;
import io.github.palexdev.virtualizedfx.utils.VFXCellsCache;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleablePropertyFactory;
import javafx.scene.Node;

/**
 * Base class that defines common properties and behaviors for all columns to be used with {@link VFXTable}.
 * Extends {@link Labeled} for simplicity, and uses behaviors of type {@link VFXTableColumnBehavior}.
 * The default style class is set to '.vfx-column'.
 * <p>
 * This class has three basic properties:
 * <p> 1) The {@link #tableProperty()}. Every column should specify the table's instance they belong to.
 * By default, as handled by {@link VFXTableManager#onColumnsChanged(ListChangeListener.Change)}, the system will manage
 * this property automatically, by setting or re-setting (to {@code null}) the instance as columns are added/removed to/from the
 * table. This is an improvement over the previous implementation which required the user to pass the table's instance to
 * the constructors.
 * <p> 2) The {@link #indexProperty()}. I believe it may be useful for every column to specify their position in
 * {@link VFXTable#getColumns()}. However, there is not a fast way to know the index of a column from its instance,
 * {@link List#indexOf(Object)} is way too slow for a virtualized container. For this reason, the property is automatically
 * updated by the {@link VFXTableSkin} when a layout is performed, see {@link VFXTableSkin#updateColumnIndex(VFXTableColumn, int)}.
 * <p> 3) Every column should specify a function to build cells for some data from the model of type {@code T}. These functions
 * do not have to produce different cell types necessarily. Rather, since every column is probably going to refer to a specific
 * piece of sub-data for a class of type {@code T}, it should tell the built cells how to map to that piece of sub-data.
 * In fact, if you check the default cell implementations (e.g. {@link VFXSimpleTableCell}) you can see that they need
 * an 'extractor' and 'converter' functions to work as intended.
 * <p>
 * Last but not least, every column has a {@link VFXCellsCache} instance which will hold cells that are not used anymore,
 * but could be in the future. Creating nodes is a very costly operation.
 * We can "dampen" this cost by caching and just update them when needed again. You can increment the capacity or disable
 * it by setting the {@link #cellsCacheCapacityProperty()}.
 * <p></p>
 * <b>Width Handling</b>
 * <p>
 * This new implementation handles the width in a special way. First and foremost, the width of each column is decided
 * by the {@link VFXTableHelper} implementation, since the base layout methods are defined there. The criteria are various.
 * There is the {@link VFXTable#columnsSizeProperty()} which depending on the {@link ColumnsLayoutMode} specifies the
 * fixed or minimum width every column must have. So, even in the latter case, we do not use {@link Node#minWidth(double)}
 * to determine the minimum width. If you want a column to have a specific width (in variable mode ofc!!) then you must
 * use this method to resize it: {@link #resize(double)}. This new implementation uses the {@link #prefWidthProperty()}
 * to store the user's preferred width for the column. Not only that, this 'slot'/variable/property is used by autosize methods
 * too {@link VFXTable#autosizeColumn(VFXTableColumn)}, and should be used by gestures too (see {@link VFXTableColumnBehavior}).
 * <p>
 * The default behavior implementation allows you to resize the column at runtime with the mouse cursor. The feature can
 * be enabled/disabled through the {@link #gestureResizableProperty()}.
 *
 * @param <T> the type of data in the table
 * @param <C> the type of cells this column will produce
 */
public abstract class VFXTableColumn<T, C extends VFXTableCell<T>> extends Labeled<VFXTableColumnBehavior<T, C>>
    implements WithCellFactory<T, C>, MFXStyleable {
    //================================================================================
    // Properties
    //================================================================================
    private final VFXCellsCache<T, C> cache;
    private final ReadOnlyObjectWrapper<VFXTable<T>> table = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyIntegerWrapper index = new ReadOnlyIntegerWrapper(-1);
    private final CellFactory<T, C> cellFactory = new CellFactory<>(null) {
        @Override
        public VFXContext<T> context() {
            return getTable().context();
        }

        @Override
        protected void onInvalidated(Function<T, C> newFactory) {
            onCellFactoryChanged(newFactory);
        }
    };

    //================================================================================
    // Constructors
    //================================================================================
    public VFXTableColumn() {
        cache = createCache();
        initialize();
    }

    public VFXTableColumn(String text) {
        super(text);
        cache = createCache();
        initialize();
    }

    public VFXTableColumn(String text, Node graphic) {
        super(text, graphic);
        cache = createCache();
        initialize();
    }

    //================================================================================
    // Static Methods
    //================================================================================

    /**
     * This convenience method swaps the columns at the given indexes in the given list.
     * <p>
     * As probably already explained in {@link VFXTable}, since columns are concrete nodes, there's the potential risk
     * of adding duplicates in the list which would lead to a JavaFX exception. This is the exact reason why
     * {@link Collections#swap(List, int, int)} cannot be used. What this method does internally, is to copy the columns
     * to a temporary array, swap the columns in that, and finally use {@link ObservableList#setAll(Object[])}.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T> void swapColumns(ObservableList<VFXTableColumn<T, ? extends VFXTableCell<T>>> columns, int i, int j) {
        VFXTableColumn[] arr = columns.toArray(VFXTableColumn[]::new);
        VFXTableColumn tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
        columns.setAll(arr);
    }

    /**
     * Delegates to {@link #swapColumns(ObservableList, int, int)} by using {@link VFXTable#getColumns()}.
     */
    public static <T> void swapColumns(VFXTable<T> table, int i, int j) {
        swapColumns(table.getColumns(), i, j);
    }

    //================================================================================
    // Methods
    //================================================================================
    private void initialize() {
        setCellFactory(defaultCellFactory());
        defaultStyleClasses(this);
    }

    /**
     * Sets the column's pref width to the given value and invokes a layout request by calling
     * {@link VFXTableManager#onColumnWidthChanged(VFXTableColumn)}.
     */
    public void resize(double width) {
        setPrefWidth(width);
        VFXTable<T> table = getTable();
        if (table == null) return;
        table.getBehavior().onColumnWidthChanged(this);
    }

    /**
     * Responsible for creating the cells' cache instance used by this column.
     *
     * @see VFXCellsCache
     * @see #cellsCacheCapacityProperty()
     */
    protected VFXCellsCache<T, C> createCache() {
        return new VFXCellsCache<>(cellFactory, getCellsCacheCapacity());
    }

    /**
     * @return the default function used to build cells. Uses {@link VFXSimpleTableCell}.
     */
    @SuppressWarnings("unchecked")
    protected Function<T, C> defaultCellFactory() {
        return t -> (C) new VFXSimpleTableCell<>(t, t1 -> (t1 != null) ? Function.identity() : null);
    }

    /**
     * This core method is responsible for telling the table to update its state when a column changes its cell factory.
     * Since every column has its own cell factory property, it would be too inconvenient to handle such case on the table
     * side. Rather, this column implementation is responsible for communicating it to the table's manager by calling
     * {@link VFXTableManager#onCellFactoryChanged(VFXTableColumn)}. (automatically called by the property!)
     * <p>
     * This will also cause the cells' cache to clear and dispose all cached cells built by the previous factory.
     */
    @SuppressWarnings("unchecked")
    protected void onCellFactoryChanged(Function<T, C> newFactory) {
        VFXTable<T> table = getTable();
        if (table == null) return;
        VFXTableManager<T> manager = table.getBehavior();
        manager.onCellFactoryChanged((VFXTableColumn<T, VFXTableCell<T>>) this);
        cache.clear();
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    // Public to allow bypassing the pref width cache which is used by the drag resizer
    // Useful to autosize columns

    /**
     * {@inheritDoc}
     * <p></p>
     * Overridden to make it public. Since we use the {@link #prefWidthProperty()} to store the width we'd like the
     * column to have, and for how JavaFX works, invoking {@link #prefWidth(double)} would return a cached value which
     * may not be what we want. In fact, this is useful for example when we have to autosize the column as this method
     * will ignore such cached value and return a computed value depending solely on its content.
     */
    @Override
    public double computePrefWidth(double height) {
        return super.computePrefWidth(height);
    }

    @Override
    public Supplier<VFXTableColumnBehavior<T, C>> defaultBehaviorProvider() {
        return () -> new VFXTableColumnBehavior<>(this);
    }

    @Override
    public List<String> defaultStyleClasses() {
        return List.of("vfx-column");
    }

    //================================================================================
    // Styleable Properties
    //================================================================================
    private final StyleableIntegerProperty cellsCacheCapacity = new StyleableIntegerProperty(
        StyleableProperties.CELLS_CACHE_CAPACITY,
        this,
        "cellsCacheCapacity",
        10
    ) {
        @Override
        protected void invalidated() {
            cache.setCapacity(get());
        }
    };

    private final StyleableBooleanProperty gestureResizable = new StyleableBooleanProperty(
        StyleableProperties.GESTURE_RESIZABLE,
        this,
        "gestureResizable",
        true
    );

    public int getCellsCacheCapacity() {
        return cellsCacheCapacity.get();
    }

    /**
     * Specifies the maximum number of cells the cache can contain at any time. Excess will not be added to the queue and
     * disposed immediately.
     * <p>
     * Can be set in CSS via the property: '-vfx-cells-cache-capacity'.
     */
    public StyleableIntegerProperty cellsCacheCapacityProperty() {
        return cellsCacheCapacity;
    }

    public void setCellsCacheCapacity(int cellsCacheCapacity) {
        this.cellsCacheCapacity.set(cellsCacheCapacity);
    }

    public boolean isGestureResizable() {
        return gestureResizable.get();
    }

    /**
     * The default behavior {@link VFXTableColumnBehavior} allows to resize the column at runtime via gestures.
     * This property is used to enable or disable such feature.
     * <p>
     * This is also settable via CSS with the "-vfx-resizable" property.
     */
    public StyleableBooleanProperty gestureResizableProperty() {
        return gestureResizable;
    }

    public void setGestureResizable(boolean gestureResizable) {
        this.gestureResizable.set(gestureResizable);
    }

    //================================================================================
    // CssMetaData
    //================================================================================
    private static class StyleableProperties {
        private static final StyleablePropertyFactory<VFXTableColumn<?, ?>> FACTORY = new StyleablePropertyFactory<>(Labeled.getClassCssMetaData());
        private static final List<CssMetaData<? extends Styleable, ?>> cssMetaDataList;

        private static final CssMetaData<VFXTableColumn<?, ?>, Number> CELLS_CACHE_CAPACITY =
            FACTORY.createSizeCssMetaData(
                "-vfx-cells-cache-capacity",
                VFXTableColumn::cellsCacheCapacityProperty,
                10
            );

        private static final CssMetaData<VFXTableColumn<?, ?>, Boolean> GESTURE_RESIZABLE =
            FACTORY.createBooleanCssMetaData(
                "-vfx-resizable",
                VFXTableColumn::gestureResizableProperty,
                true
            );

        static {
            cssMetaDataList = StyleUtils.cssMetaDataList(
                Labeled.getClassCssMetaData(),
                CELLS_CACHE_CAPACITY, GESTURE_RESIZABLE
            );
        }
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.cssMetaDataList;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return getClassCssMetaData();
    }

    //================================================================================
    // Getters/Setters
    //================================================================================

    /**
     * @return the cells' cache instance used by this column
     */
    protected VFXCellsCache<T, C> cache() {
        return cache;
    }

    /**
     * Delegate for {@link VFXCellsCache#populate()}.
     */
    public void populateCache() {
        cache.populate();
    }

    /**
     * Delegate for {@link VFXCellsCache#size()}.
     */
    public int cacheSize() {
        return cache.size();
    }

    public VFXTable<T> getTable() {
        return table.get();
    }

    /**
     * Specifies the table's instance this column belongs to.
     * The value will be {@code null} if the column is not yet in the scene graph or if the column is not part of any table.
     */
    public ReadOnlyObjectProperty<VFXTable<T>> tableProperty() {
        return table.getReadOnlyProperty();
    }

    protected void setTable(VFXTable<T> table) {
        this.table.set(table);
    }

    public int getIndex() {
        return index.get();
    }

    /**
     * Specifies the index of the columns in the list {@link VFXTable#getColumns()}.
     * The value will be -1 if the property has not been updated yet or if the column is not in a table.
     * <p>
     * This method will be reliable 99% of the time, however, just to be sure I suggest you to use
     * {@link VFXTable#indexOf(VFXTableColumn)} instead.
     */
    public ReadOnlyIntegerProperty indexProperty() {
        return index.getReadOnlyProperty();
    }

    protected void setIndex(int index) {
        this.index.set(index);
    }

    /**
     * Specifies the function used to build the cells.
     * See also {@link #defaultCellFactory()}.
     */
    @Override
    public CellFactory<T, C> getCellFactory() {
        return cellFactory;
    }
}