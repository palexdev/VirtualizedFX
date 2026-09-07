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
import io.github.palexdev.mfxcore.behavior.MFXBehavior;
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

import static io.github.palexdev.mfxcore.controls.MFXStyleable.styleClasses;
import static java.util.Objects.requireNonNull;

public abstract class VFXTableColumn<T, C extends VFXTableCell<T>> extends MFXLabeled implements WithCellFactory<T, C> {

    //================================================================================
    // Properties
    //================================================================================

    private final ReadOnlyObjectWrapper<VFXTable<T>> table = new ReadOnlyObjectWrapper<>() {
        @Override
        protected void invalidated() {
            if (index.rebind()) {
                VFXTable<T> table = get();
                if (getUserPrefWidth() > table.getColumnsSize().width())
                    onColumnWidthChanged();
            }
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

    // TODO swap methods should be moved to the table

    //================================================================================
    // Methods
    //================================================================================

    protected VFXCellsCache<T, C> createCellsCache() {
        return new VFXCellsCache<>(cellFactory, getCellsCacheCapacity());
    }

    @SuppressWarnings("unchecked")
    public Function<T, C> defaultCellFactory() {
        return t -> (C) new VFXSimpleTableCell<>(t, Function.identity());
    }

    protected void onCellFactoryChanged(Function<T, C> newFactory) {
        VFXTable<T> table = getTable();
        if (table == null) return;
        cellsCache.clear(); // make sure to clear the cache first!
        requireNonNull(table.getBehavior(), "Table's manager cannot be null").onCellFactoryChanged(this);
    }

    protected void onColumnWidthChanged() {
        VFXTable<T> table = getTable();
        if (table == null) return;
        requireNonNull(table.getBehavior(), "Table's manager cannot be null").onColumnResized(VFXTableColumn.this);
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

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

    public StyleableIntegerProperty cellsCacheCapacityProperty() {
        return cellsCacheCapacity;
    }

    public void setCellsCacheCapacity(int cellsCacheCapacity) {
        this.cellsCacheCapacity.set(cellsCacheCapacity);
    }

    public boolean isGestureResizable() {
        return gestureResizable.get();
    }

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

    public ReadOnlyObjectProperty<VFXTable<T>> tableProperty() {
        return table.getReadOnlyProperty();
    }

    protected void setTable(VFXTable<T> table) {
        this.table.set(table);
    }

    public int getIndex() {
        return index.get();
    }

    // TODO document that it's O(n) if not cached yet, and that it's self healing
    public ObservableValue<Number> indexProperty() {
        return index;
    }

    public VFXCellsCache<T, C> getCellsCache() {
        return cellsCache;
    }

    public void populateCellsCache() {
        cellsCache.populate();
    }

    public int cacheSize() {
        return cellsCache.size();
    }

    @Override
    public CellFactory<T, C> getCellFactory() {
        return cellFactory;
    }

    public double getUserPrefWidth() {
        return userPrefWidth.getValue();
    }

    public ExtendedProperty<Double> userPrefWidthProperty() {
        return userPrefWidth;
    }

    public void setUserPrefWidth(double userPrefWidth) {
        this.userPrefWidth.set(userPrefWidth);
    }

    //================================================================================
    // Inner Classes
    //================================================================================

    protected class IndexBinding extends IntegerBinding {
        private int current = -1;

        private Observable[] deps = {};

        @Override
        protected int computeValue() {
            VFXTable<T> table = getTable();
            if (table == null) return current = -1;

            var columns = table.columns();
            if (current < 0 || current >= columns.size() || columns.get(current) != VFXTableColumn.this)
                current = columns.indexOf(VFXTableColumn.this);

            return current;
        }

        public boolean rebind() {
            unbind(deps);
            VFXTable<T> table = getTable();
            if (table == null) {
                invalidate();
                return false;
            }

            deps = new Observable[]{table.columns()};
            bind(deps);
            invalidate();
            return true;
        }
    }
}
