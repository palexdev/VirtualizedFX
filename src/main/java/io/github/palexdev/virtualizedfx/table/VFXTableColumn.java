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

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.palexdev.mfxcore.base.properties.base.ExtendedProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableBooleanProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableIntegerProperty;
import io.github.palexdev.mfxcore.controls.MFXBehavior;
import io.github.palexdev.mfxcore.controls.MFXLabeled;
import io.github.palexdev.mfxcore.utils.fx.PropUtils;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import io.github.palexdev.virtualizedfx.base.VFXContext;
import io.github.palexdev.virtualizedfx.base.WithCellFactory;
import io.github.palexdev.virtualizedfx.cells.VFXSimpleTableCell;
import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.properties.CellFactory;
import io.github.palexdev.virtualizedfx.table.defaults.VFXTableColumnBehavior;
import io.github.palexdev.virtualizedfx.utils.VFXCellsCache;
import javafx.beans.Observable;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleablePropertyFactory;
import javafx.scene.Node;
import javafx.scene.Parent;

import static io.github.palexdev.mfxcore.controls.MFXStyleable.styleClasses;
import static java.util.Optional.ofNullable;

/// Base class that defines common properties and behaviors for all columns to be used with [VFXTable].
/// Extends [MFXLabeled] for simplicity: the text and the graphic make the column's header.
/// The default style class is: '.vfx-column'.
///
/// ## Table and index
///
/// Every column knows the table it belongs to, [#tableProperty()], and its position in the table's columns' list,
/// [#indexProperty()]. Both are handled automatically by the table as columns are added to/removed from the list,
/// there's no need to pass the table's instance to the column.
///
/// ## Cells
///
/// Every column specifies a function to build cells for some data from the model of type `T`, see [#getCellFactory()].
/// These functions do not have to produce different cell types necessarily. Rather, since every column is probably going to
/// refer to a specific piece of sub-data for a class of type `T`, it should tell the built cells how to map to that piece
/// of sub-data. In fact, if you check the default cell implementations (e.g. [VFXSimpleTableCell]) you can see that they
/// need an 'extractor' and 'converter' functions to work as intended.
///
/// Every column also has a [VFXCellsCache] instance which will hold cells that are not used anymore, but could be in the
/// future. Creating nodes is a very costly operation. We can "dampen" this cost by caching and just update them when
/// needed again. You can increment the capacity or disable it by setting the [#cellsCacheCapacityProperty()].
///
/// ## Width
///
/// The width of each column is decided by the table. It's computed by the table's [ColumnsLayoutCache], and applied at
/// layout by its [VFXTableHelper]. So, the column's min, pref and max widths do not determine it. What the column can do
/// is ask for a width through the [#userPrefWidthProperty()]. How the request is honored is up to the cache, see [VFXTable]
/// for the columns' size and how the space left in the table is distributed. For the latter, a column can also have a
/// weight, see [VFXTable#setWeight(VFXTableColumn,int)].
///
/// A column can be sized to fit its content with [#sizeToContent()].
///
/// ## Behavior
///
/// The default behavior, [VFXTableColumnBehavior], adds features such as resizing the column with the mouse. Some of them
/// can be toggled from here, e.g. [#gestureResizableProperty()]. A custom behavior is free to ignore such properties.
public abstract class VFXTableColumn<T, C extends VFXTableCell<T>> extends MFXLabeled implements WithCellFactory<T, C> {

    //================================================================================
    // Properties
    //================================================================================

    /// The key under which the autosize mark is stored in the column's properties map, see [#markForAutosize()].
    public static final String AUTOSIZE_KEY = "AUTOSIZE";

    private final ReadOnlyObjectWrapper<VFXTable<T>> table = new ReadOnlyObjectWrapper<>() {
        @Override
        protected void invalidated() {
            index.rebind();
        }
    };
    private final IndexBinding index = new IndexBinding();

    private final VFXCellsCache<T, C> cellsCache;
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

    private final ExtendedProperty<Double> userPrefWidth = PropUtils.doubleProperty()
        .onInvalidated(_ -> onColumnWidthChanged())
        .initialValue(-1.0)
        .extended(-1.0);

    //================================================================================
    // Constructors
    //================================================================================

    public VFXTableColumn() {
        this("");
    }

    public VFXTableColumn(String text) {
        this(text, null);
    }

    public VFXTableColumn(String text, Node graphic) {
        super(text, graphic);
        cellsCache = createCellsCache();
        setCellFactory(defaultCellFactory());
    }

    //================================================================================
    // Methods
    //================================================================================

    /// Marks the column for autosize, see [#markForAutosize()], and requests a layout of the table (if the column is in one).
    ///
    /// The mark is processed by the table's skin, which sizes the column to fit its content by setting the
    /// [#userPrefWidthProperty()], and then removes the mark. Until then, the mark stays, so if the column is not in a
    /// table yet, it will be autosized once it is. See [VFXTableSkin#autosize(VFXTableColumn)] for what the default skin
    /// measures and when it can do it.
    public void sizeToContent() {
        markForAutosize();
        ofNullable(getTable()).ifPresent(Parent::requestLayout);
    }

    /// @return whether the column is marked for autosize, see [#sizeToContent()]
    public boolean isMarkedForAutosize() {
        return hasProperties() && getProperties().containsKey(AUTOSIZE_KEY);
    }

    /// Puts the autosize mark, [#AUTOSIZE_KEY], in the column's properties map.
    ///
    /// The mark alone does not trigger anything, it waits for the table's skin to process it at the next layout.
    /// That's why [#sizeToContent()] also requests one.
    protected void markForAutosize() {
        getProperties().put(AUTOSIZE_KEY, null);
    }

    /// Removes the autosize mark from the column's properties map.<br >
    /// Called by the table's skin once the column has been autosized.
    protected void unmarkForAutosize() {
        getProperties().remove(AUTOSIZE_KEY);
    }

    /// Responsible for creating the cells' cache instance used by this column.
    ///
    /// @see VFXCellsCache
    /// @see #cellsCacheCapacityProperty()
    protected VFXCellsCache<T, C> createCellsCache() {
        return new VFXCellsCache<>(cellFactory, getCellsCacheCapacity());
    }

    /// @return the default function used to build cells. Uses [VFXSimpleTableCell] with an identity extractor, so the
    /// cells display the item itself.
    @SuppressWarnings("unchecked")
    public Function<T, C> defaultCellFactory() {
        return t -> (C) new VFXSimpleTableCell<>(t, Function.identity());
    }

    /// This core method is responsible for telling the table to update its state when the column changes its cell factory.
    /// Since every column has its own cell factory property, it would be too inconvenient to handle such case on the table side.
    /// Rather, the column is responsible for communicating it to the table's manager by calling
    /// [VFXTableManager#onCellFactoryChanged(VFXTableColumn)]. (automatically called by the property!)
    ///
    /// Before that, the cells' cache is cleared, since the cached cells were built by the previous factory. This happens
    /// even if the column is not in a table, in which case the manager is not notified.
    protected void onCellFactoryChanged(Function<T, C> newFactory) {
        VFXTable<T> table = getTable();
        cellsCache.clear(); // make sure to clear the cache first!
        if (table != null) table.getManager().onCellFactoryChanged(this);
    }

    /// Called when the [#userPrefWidthProperty()] changes, notifies the table's manager by calling
    /// [VFXTableManager#onColumnResized(VFXTableColumn)].<br >
    /// Does nothing if the column is not in a table.
    protected void onColumnWidthChanged() {
        VFXTable<T> table = getTable();
        if (table != null) table.getManager().onColumnResized(VFXTableColumn.this);
    }

    /// Tells the [#indexProperty()] where the column is, see [IndexBinding#hint(int)].
    ///
    /// Only the table should call this, since it knows the new indexes from the changes in its columns' list.
    void hintIndex(int index) { // pkg-private, only table should run this
        this.index.hint(index);
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    /// {@inheritDoc}
    ///
    /// The default behavior is [VFXTableColumnBehavior].
    @Override
    public Supplier<MFXBehavior<? extends Node>> defaultBehaviorFactory() {
        return () -> new VFXTableColumnBehavior<>(this);
    }

    @Override
    public List<String> defaultStyleClasses() {
        return styleClasses("vfx-column");
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
            int capacity = get();
            if (capacity < 0) throw new IllegalArgumentException("Cache capacity cannot be negative!");
            cellsCache.setCapacity(capacity);
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

    /// Specifies the maximum number of cells the cache can contain at any time. Excess will not be added to the queue and
    /// disposed immediately.
    ///
    /// Can be set in CSS via the property: '-vfx-cells-cache-capacity'.
    public StyleableIntegerProperty cellsCacheCapacityProperty() {
        return cellsCacheCapacity;
    }

    public void setCellsCacheCapacity(int cellsCacheCapacity) {
        this.cellsCacheCapacity.set(cellsCacheCapacity);
    }

    public boolean isGestureResizable() {
        return gestureResizable.get();
    }

    /// Specifies whether the column can be resized with the mouse.
    ///
    /// This property is honored by the default behavior, [VFXTableColumnBehavior]. A custom behavior may ignore it.
    ///
    /// Can be set in CSS via the property: '-vfx-resizable'.
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
        private static final StyleablePropertyFactory<VFXTableColumn<?, ?>> FACTORY = new StyleablePropertyFactory<>(MFXLabeled.getClassCssMetaData());
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
                MFXLabeled.getClassCssMetaData(),
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

    public VFXTable<T> getTable() {
        return table.get();
    }

    /// Specifies the table's instance this column belongs to.<br >
    /// Set automatically by the table as the column is added to/removed from its columns' list, the value is `null`
    /// if the column is not part of any table.
    public ReadOnlyObjectProperty<VFXTable<T>> tableProperty() {
        return table.getReadOnlyProperty();
    }

    protected void setTable(VFXTable<T> table) {
        this.table.set(table);
    }

    public int getIndex() {
        return index.get();
    }

    /// Specifies the index of the column in the table's [VFXTable#columns()], -1 if the column is not in a table.
    ///
    /// There is not a fast way to know the index of a column from its instance, [List#indexOf(Object)] is way too slow
    /// for a virtualized container. So, the index is cached and validated on read, see [IndexBinding].
    public ObservableValue<Number> indexProperty() {
        return index;
    }

    /// @return the cells' cache instance used by this column
    public VFXCellsCache<T, C> getCellsCache() {
        return cellsCache;
    }

    /// Delegate for [VFXCellsCache#populate()].
    ///
    /// The column must be in a table, since cells are built with the table's [VFXContext], see [#getCellFactory()].
    public void populateCellsCache() {
        cellsCache.populate();
    }

    /// Delegate for [VFXCellsCache#size()].
    public int cacheSize() {
        return cellsCache.size();
    }

    /// Specifies the function used to build the cells.
    /// See also [#defaultCellFactory()].
    ///
    /// The cells are given the table's [VFXContext], so they can only be built while the column is in a table.
    @Override
    public CellFactory<T, C> getCellFactory() {
        return cellFactory;
    }

    public double getUserPrefWidth() {
        return userPrefWidth.getValue();
    }

    /// Specifies the width the column would like to have, -1 (the default value) means none.<br >
    /// This is also where [#sizeToContent()] stores its result, and where gestures should store theirs.
    ///
    /// How the value is honored is up to the table's [ColumnsLayoutCache], see [VFXTable#columnsSizeProperty()] and
    /// [VFXTable#columnsFillPolicyProperty()]. Changes are notified to the table, see [#onColumnWidthChanged()].
    public ExtendedProperty<Double> userPrefWidthProperty() {
        return userPrefWidth;
    }

    public void setUserPrefWidth(double userPrefWidth) {
        this.userPrefWidth.set(userPrefWidth);
    }

    //================================================================================
    // Inner Classes
    //================================================================================

    /// The binding behind the [#indexProperty()].
    ///
    /// It caches the column's index and validates it on every computation: if the column is still at the cached index,
    /// that's the value, otherwise the index is searched with [List#indexOf(Object)] and cached again.
    /// The binding depends on the table's columns' list, so any change to it invalidates the index. Being lazy, though,
    /// the validation runs only when someone reads the value.
    ///
    /// The table hints the new indexes when the list changes, see [#hint(int)], so the search should be rare.
    protected class IndexBinding extends IntegerBinding {
        private int current = -1;
        private Observable[] deps = {};

        /// Validates the cached index against the table's columns' list, and searches it with [List#indexOf(Object)] only
        /// if it's wrong.
        ///
        /// @return the column's index, or -1 if the column is not in a table
        @Override
        protected int computeValue() {
            VFXTable<T> table = getTable();
            if (table == null) return current = -1;

            var columns = table.columns();
            if (current < 0 || current >= columns.size() || columns.get(current) != VFXTableColumn.this)
                current = columns.indexOf(VFXTableColumn.this);

            return current;
        }

        /// Sets the cached index without searching it.<br >
        /// A wrong hint is harmless, [#computeValue()] catches it and searches the right index.
        public void hint(int index) {
            current = index;
        }

        /// Called when the column's table changes. Binds to the new table's columns' list
        /// (or to nothing if the column was removed), forgets the cached index, and invalidates.
        public void rebind() {
            unbind(deps);
            deps = new Observable[0];
            current = -1;

            VFXTable<T> table = getTable();
            if (table != null) {
                deps = new Observable[]{table.columns()};
                bind(deps);
            }
            invalidate();
        }
    }
}
