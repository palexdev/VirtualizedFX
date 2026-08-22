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

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.enums.ColumnsLayoutMode;
import io.github.palexdev.virtualizedfx.table.VFXTableHelper.VariableTableHelper;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;

import static java.util.Optional.ofNullable;

/// Complex cache mechanism to simplify and vastly improve layout performance for [ColumnsLayoutMode#VARIABLE].
/// In this mode columns may have different widths, which makes some computations way more expensive.
///
/// Example 1: A columns width must be 'asked' to the column itself rather than using the value specified by
/// [VFXTable#columnsSizeProperty()]
///
/// Example 2: A columns position cannot be determined by a simple multiplication, but it's the sum of all previous
/// columns' widths (a prefix sum)
///
/// Also, keep in mind that such computations are not needed only for the columns, but also for their corresponding cells
/// (can't rely on JavaFX bounds because sometimes they are messed up, garbage framework).
///
/// This cache implementation tries to mitigate this by caching each column's width and x position. Listeners and
/// bindings will automatically invalidate the data as needed, and re-compute it once requested, which in other words
/// means that the cache is 'lazy'.
///
/// The positions being memoized is also what makes the mode virtualizable at all: a prefix sum is monotonic, therefore
/// binary-searchable, so [VariableTableHelper] can find the columns that fall in the viewport in `O(log n)` without
/// ever walking the list. See [VariableTableHelper#columnAt(double)].
///
/// **Why this extends** [DoubleBinding]
///
/// When I decided to create this special cache, it was mainly to improve the computation speed of the [VFXTable#virtualMaxXProperty()]
/// (in VARIABLE mode ofc), because it requires summing every column's width. So, I came up with a simple extension of
/// [DoubleBinding] which would invalidate the cached widths and thus re-compute upon request their sum.
/// It was then that I decided to expand the cache to also hold the positions, because the two pieces of information are
/// tightly coupled: a position is the sum of the previous columns' widths.
/// So, besides making such computations faster, this also still allows computing the `virtualMaxX` much faster.
/// There's even a special width value given by [#getPartialWidth()] which is the sum of all column's widths excluding
/// the last one. This is useful to compute the last column's width, as it may need to be bigger than expected to fill
/// the table (such value could be given by `tableWidth - partialWidth`).
///
/// **How data is stored**
///
/// The cache makes use of a [Map] and a wrapper class [LayoutInfo] to gather all the computations in one place.
/// Each table's column will have an entry in the map like this: [Column->LayoutInfo]. When something needs to be
/// invalidated, setters are called on the appropriate [LayoutInfo] object.
///
/// **Listeners**
///
/// To manage invalidations and columns changes in the table, this uses a series of listeners.
///
/// 1) An [InvalidationListener] on [VFXTable#getColumns()] ensures the above-mentioned map stays always updated,
/// more info here [#handleColumns()]. It is deliberately not a [ListChangeListener]: that would make it run
/// *after* [VFXTableSkin]'s, which is too late. See the method's docs
///
/// 2) An [InvalidationListener] watches for [VFXTable#columnsSizeProperty()] changes and by iterating over
/// the [LayoutInfo] stored in the map, performs the following actions:
///     - Resets all the positions, since the property specifies the minimum width and thus may move every column
///     - Invalidates the width if it's below the new value specified by the property
///     - At the end it also invalidates the width for the last column (if it wasn't done before).
///     This is important to ensure that the last column takes all the available space
///
/// 3) An [InvalidationListener] added on [VFXTable#widthProperty()], which invalidates the **last** column's width.
/// That column is the only one whose width depends on the table's size, since it stretches to fill whatever space the
/// others leave over.
///
/// 4) Lastly, there an [InvalidationListener] for each column in the map to watch for [VFXTableColumn#prefWidthProperty()]
/// changes. This is managed by each [LayoutInfo], more info there.
///
/// **Computing functions and initialization**
///
/// For the cache to work, the user must specify the two functions used to compute:
///
/// 1) the widths, [#setWidthFunction(BiFunction)]
///
/// 2) the positions, [#setPositionFunction(BiFunction)]
///
/// To avoid cluttering the constructors, and for other reasons, the cache won't be active until you call the
/// [#init()] method. Both the setters and the init methods follow the fluent API pattern. **Beware,** if either
/// of the two functions is not set, [#init()] will throw an exception!
///
/// @see LayoutInfoCache
public class ColumnsLayoutCache<T> extends DoubleBinding {
    //================================================================================
    // Properties
    //================================================================================
    private VFXTable<T> table;
    private final LayoutInfoCache cache;
    private boolean init = false;
    /// When `true`, [#toString()] sorts the entries by column index rather than printing them in the map's own
    /// (arbitrary) order. Debugging aid, off by default because it copies the map into a [TreeMap] on every call.
    public boolean sortToString = false;

    private VFXTableColumn<T, ?> lColumn;
    private final ReadOnlyBooleanWrapper anyChanged = new ReadOnlyBooleanWrapper(false) {
        @Override
        protected void invalidated() {
            if (get()) {
                invalidateLast();
                invalidate();
            }
        }
    };
    private Consumer<Boolean> invalidatingAction = last -> {
        if (last) invalidate();
        else anyChanged.set(true);
    };

    // Layout functions
    private BiFunction<VFXTableColumn<T, ?>, Boolean, Double> widthFn;
    private BiFunction<Integer, Double, Double> xPosFn;

    // Listeners
    private InvalidationListener clListener;
    private InvalidationListener csListener;
    private InvalidationListener wListener;

    //================================================================================
    // Constructors
    //================================================================================
    public ColumnsLayoutCache(VFXTable<T> table) {
        this.table = table;
        cache = new LayoutInfoCache();
        clListener = _ -> handleColumns();
        csListener = i -> {
            for (LayoutInfo li : cache.values()) {
                // Resets all positions
                li.resetPos();
                // Invalidate only the ones that are now below the minimum
                if (!li.isWidthValid()) continue;
                if (li.getWidth() < table.getColumnsSize().width()) li.invalidateWidth();
            }
            // Also invalidate last
            invalidateLast();
        };
        wListener = _ -> invalidateLast();
    }

    //================================================================================
    // Methods
    //================================================================================

    /// If [#preInitCheck()] does not throw any exception, initializes the cache by adding the needed listeners
    /// to the appropriate properties, as well as creating the cache mappings for each column in the table.
    ///
    /// Further calls to this method won't do anything if the cache has already been initialized before.
    public ColumnsLayoutCache<T> init() {
        if (!init) {
            preInitCheck();
            ObservableList<VFXTableColumn<T, ? extends VFXTableCell<T>>> columns = table.getColumns();
            if (!columns.isEmpty()) {
                lColumn = columns.getLast();
                for (VFXTableColumn<T, ? extends VFXTableCell<T>> c : columns) cache.put(c, new LayoutInfo(c));
            }
            columns.addListener(clListener);
            table.columnsSizeProperty().addListener(csListener);
            table.widthProperty().addListener(wListener);
            init = true;
        }
        return this;
    }

    /// Checks that all the computing functions are set.
    ///
    /// @see #setWidthFunction(BiFunction)
    /// @see #setPositionFunction(BiFunction)
    private void preInitCheck() {
        if (widthFn == null)
            throw new IllegalStateException("Cannot initialize because: width function has not been set.");
        if (xPosFn == null)
            throw new IllegalStateException("Cannot initialize because: x position function has not been set.");
    }

    /// Delegates to [LayoutInfoCache#getWidth(VFXTableColumn)].
    ///
    /// @return either the cached or computed width for the given column
    public double getColumnWidth(VFXTableColumn<T, ?> column) {
        return cache.getWidth(column);
    }

    /// Delegates to [#getColumnWidth(VFXTableColumn)] by passing the last column in the table.
    public double getLastColumnWidth() {
        return getColumnWidth(lColumn);
    }

    /// @return the sum of all columns' widths excluding the last one
    public double getPartialWidth() {
        return cache.entrySet().stream()
            .filter(e -> e.getKey() != lColumn)
            .mapToDouble(e -> e.getValue().getWidth())
            .sum();
    }

    /// The position of the column at the given index. This method is recursive!
    ///
    /// A column's x position is the sum of all the previous columns' widths, in other words a prefix
    /// sum. Rather than looping from 0 on every query, the values are memoized in the [LayoutInfo]
    /// objects, using `-1.0` as the 'invalid' sentinel, see [LayoutInfo#getPos()].
    ///
    /// Detailing the internals:
    /// ```
    ///// Let's suppose we want to compute the position of the column at index 2 (so third one)
    ///// First we convert the index to the corresponding column
    /// VFXTableColumn c = ...;
    ///// Then we query the map and get the known position for that column
    /// double pos = map.getPos(c);
    ///// Index 0 is a special case and we handle it as follows
    /// if (index == 0){
    ///     map.setPos(c, 0); // Column 0 is always at x = 0
    ///     return 0;
    ///}
    ///// If 'pos' is lesser than 0, then it either means it was never been computed before or it was invalidated
    ///// We need to ask the position function to compute the value as follows...
    /// if (pos < 0){
    ///     pos = posFn.apply(index -1, getColumnPos(index -1)); // Here's where the method calls itself
    ///     map.setPos(index, pos); // Store the found pos in the cache so we don't fall in this 'if' again until invalidated
    ///}
    /// return pos;
    ///// Why the recursion?
    ///// In general, to compute a column's position, we can simply get the position of the previous column + its width.
    ///// So, for the third one, we need the second one's position, and so on...
    ///// The recursion doesn't happen if the previous value is known, so the method acts almost like a simple getter
    ///// The recursion stops at column 0, because it's position is always 0.
    ///```
    ///
    /// **Mind the arguments given to the position function.** It is invoked as
    /// `posFn.apply(index - 1, getColumnPos(index - 1))`, not as `(index, ...)`. That is not an
    /// off-by-one: the function takes the **previous** column's index and the **previous** column's
    /// position, and returns the position of the one that follows it, which is
    /// `pos(index - 1) + width(index - 1)`. See [VariableTableHelper#computeColumnPos(int, double)].
    public double getColumnPos(int index) {
        VFXTableColumn<T, ? extends VFXTableCell<T>> column = table.getColumns().get(index);
        LayoutInfo li = cache.require(column);
        double pos = li.getPos();
        if (index == 0) {
            li.setPos(0.0);
            return 0.0;
        }
        if (pos < 0) {
            pos = xPosFn.apply(index - 1, getColumnPos(index - 1));
            li.setPos(pos);
        }
        return pos;
    }

    /// @return the number of entries in the cache's map. This should always be equal to the size of [VFXTable#getColumns()]
    public int size() {
        return cache.size();
    }

    /// Invalidates the last column's width.
    private void invalidateLast() {
        cache.invalidateWidth(lColumn);
    }

    /// This method is responsible for updating the cache map's entries when changes occur in [VFXTable#getColumns()].
    ///
    /// If there are no columns anymore, calls [#clear()] and [#invalidate()], then exits.
    ///
    /// **Beware, the order of the operations is crucial here!** The map must be synchronized with the list _before_
    /// any invalidation occurs. The reason is that invalidating a width triggers the 'partial invalidation' mechanism
    /// described in [LayoutInfo#createWidthBinding()], which iterates over [VFXTable#getColumns()] and queries the map
    /// for **each** of them. If we were to invalidate first, entries for newly added columns would still be missing,
    /// and the resulting failure would also abort this method, leaving the cache in a corrupted state (missing entries)
    /// forever.
    ///
    /// So, first, we synchronize. One pass over the list gives every column an entry, creating the missing ones, and
    /// invalidates the index and position of each: any structural change (addition, removal, permutation) is likely to
    /// have shifted the columns, so re-computing is both simpler and more reliable than guessing which values survived.
    /// Freshly created [LayoutInfo] objects already start invalid, so for them those two calls are no-ops.
    ///
    /// The map can only hold **more** entries than the list if something was removed, so the sweep that disposes stale
    /// entries is guarded by that check and skipped entirely for pure additions.
    ///
    /// Then, remember, the last column is always a special case in the table because it behaves a little different from
    /// the others. So, we must ensure that the last column is still the same as before. If that's not the case, first we
    /// call [#invalidateLast()] to ensure that the 'now previously last' column has the right width (the width computing
    /// function is likely to return a different value now); note that this is a no-op if the column was removed, as its
    /// entry is already gone by now. Then we update the local reference for the last column (yes, the cache stores it
    /// for fast access) and invalidate its width too, since the `widthFn` is likely to return a different value for it
    /// as well. Finally we call [#invalidate()] and [#invalidateLast()].
    ///
    /// **Why this takes no [ListChangeListener.Change] and is driven by an [InvalidationListener]**
    ///
    /// Two reasons, and the second is the important one.
    ///
    /// 1) The [Change] was never a good fit. `setAll()` is reported as a removal of everything plus an addition of
    /// everything, even when the two lists are nearly identical, so an implementation that trusts it would dispose and
    /// re-create every [LayoutInfo] for no reason. Working around that needed a temporary collection to cancel out the
    /// columns that appear on both sides. Diffing against the list itself never sees that distinction, so the special
    /// case simply disappears.
    ///
    /// 2) **Listener ordering.** [VFXTableSkin] also listens to [VFXTable#getColumns()], and its listener computes a
    /// new state, which triggers a layout, which reads [VFXTable#virtualMaxXProperty()], which is bound to this cache.
    /// So this cache *must* be up to date first. Registration order used to decide that, and it is not something we
    /// control: the helper is built before the skin at construction, but a [ColumnsLayoutMode] switch builds a **new**
    /// helper, and therefore a new cache, whose listener lands *after* the skin's. The result was a layout computed
    /// against a stale `virtualMaxX`, with nothing to correct it afterwards.
    ///
    /// JavaFX invokes **all** [InvalidationListener]s before **any** [ListChangeListener]
    /// (`ListListenerHelper.Generic#fireValueChangedEvent`), so registering as the former guarantees this runs first
    /// no matter when it was added. The skin relies on the same guarantee, for the same reason, on
    /// [VFXTable#widthProperty()].
    private void handleColumns() {
        ObservableList<VFXTableColumn<T, ? extends VFXTableCell<T>>> columns = table.getColumns();
        if (columns.isEmpty()) {
            clear();
            invalidate();
            return;
        }

        // Sync the map with the list first!
        for (VFXTableColumn<T, ? extends VFXTableCell<T>> c : columns) {
            /*
             * Since bindings are all the same whether it's the first column, in the middle or the last one...
             * There is no need to replace an already existing binding.
             */
            LayoutInfo li = cache.computeIfAbsent(c, LayoutInfo::new);
            li.invalidateIndex();
            li.resetPos();
        }

        // More entries than columns can only mean something was removed
        if (cache.size() > columns.size()) {
            Set<VFXTableColumn<T, ?>> live = Collections.newSetFromMap(new IdentityHashMap<>());
            live.addAll(columns);
            cache.entrySet().removeIf(e -> {
                if (live.contains(e.getKey())) return false;
                e.getValue().dispose();
                return true;
            });
        }

        VFXTableColumn<T, ? extends VFXTableCell<T>> last = columns.getLast();
        if (last != lColumn) {
            // Invalidate the previous last's binding (no-op if it's not in the cache anymore)
            invalidateLast();
            lColumn = last;
            cache.invalidateWidth(last);
        }

        invalidate();
        invalidateLast();
    }

    /// Clears the cache by removing all the entries from the map and setting the last column local reference to `null`.
    private void clear() {
        cache.clear();
        anyChanged.set(false);
        lColumn = null;
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    /// @return the sum of all columns' widths, each given by [LayoutInfo#getWidth()]
    @Override
    protected double computeValue() {
        anyChanged.set(false);
        return cache.values().stream()
            .mapToDouble(LayoutInfo::getWidth)
            .sum();
    }

    /// Disposes the cache making it not usable anymore.
    ///
    /// @see #clear()
    @Override
    public void dispose() {
        clear();
        widthFn = null;
        invalidatingAction = null;
        table.getColumns().removeListener(clListener);
        table.columnsSizeProperty().removeListener(csListener);
        table.widthProperty().removeListener(wListener);
        clListener = null;
        csListener = null;
        wListener = null;
        table = null;
    }

    @Override
    public String toString() {
        Map<VFXTableColumn<T, ?>, LayoutInfo> cache = this.cache;
        if (sortToString) cache = new TreeMap<>(this.cache);

        StringBuilder sb = new StringBuilder();
        sb.append("ColumnsLayoutCache [%s][%d] {".formatted(isValid() ? "valid:[%f]".formatted(get()) : "invalid", size()));
        if (cache.isEmpty()) {
            sb.append("empty}");
            return sb.toString();
        }
        sb.append("\n");

        // Pretty print
        int maxL = 0;
        for (VFXTableColumn<T, ?> c : cache.keySet()) {
            String text = ofNullable(c.getText()).orElse("");
            maxL = Math.max(maxL, text.length());
        }

        for (Iterator<Map.Entry<VFXTableColumn<T, ?>, LayoutInfo>> iterator = cache.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<VFXTableColumn<T, ?>, LayoutInfo> entry = iterator.next();
            VFXTableColumn<T, ?> c = entry.getKey();
            LayoutInfo i = entry.getValue();
            int index = i.getIndex();
            String text = ofNullable(c.getText()).orElse("");

            DoubleBinding b = i.wBinding;
            double pos = i.getPos();

            sb.append("  ")
                .append("Column: ")
                .repeat(" ", maxL - "Column".length())
                .append(text)
                .append("\n")
                .append("  ")
                .append("Index: ")
                .repeat(" ", maxL - "Index".length())
                .append("[%d]".formatted(index))
                .append("\n")
                .append("  ")
                .append("Width: ")
                .repeat(" ", maxL - "Width".length())
                .append(b.isValid() ? "[valid:%.2f]".formatted(b.get()) : "[invalid]")
                .append("\n")
                .append("  ")
                .append("Position: ")
                .repeat(" ", maxL - "Position".length())
                .append((pos <= -1.0) ? "[invalid]" : "[valid:%.2f]".formatted(pos))
                .append("\n");
            if (iterator.hasNext()) {
                sb.append("  ");
                sb.repeat("_", 30);
                sb.append("\n");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    //================================================================================
    // Getters/Setters
    //================================================================================

    /// @return the [VFXTable] instance this cache is related to
    public VFXTable<T> getTable() {
        return table;
    }

    /// @return the map containing the columns' layout data as [LayoutInfo] objects
    protected Map<VFXTableColumn<T, ?>, LayoutInfo> getCacheMap() {
        return cache;
    }

    /// @return the local reference to the last column in the table
    protected VFXTableColumn<T, ?> getLastColumn() {
        return lColumn;
    }

    /// Getter for [#anyChangedProperty()].
    public boolean isAnyChanged() {
        return anyChanged.get();
    }

    /// Specifies whether any of the [LayoutInfo] objects in [#getCacheMap()] was invalidated.
    public ReadOnlyBooleanProperty anyChangedProperty() {
        return anyChanged.getReadOnlyProperty();
    }

    /// Sets the [BiFunction] responsible for computing a column's width. The function gives the following parameters:
    /// 1) the column to compute the width for; 2) whether it is the last column in the table which may need special handling.
    ///
    /// You can check [VariableTableHelper#computeColumnWidth(VFXTableColumn, boolean)] for an example.
    public ColumnsLayoutCache<T> setWidthFunction(BiFunction<VFXTableColumn<T, ?>, Boolean, Double> widthFn) {
        this.widthFn = widthFn;
        return this;
    }

    /// Sets the [BiFunction] responsible for computing a column's position. The function gives the following parameters:
    /// 1) the **previous** column's index;
    /// 2) the **previous** column's position. It returns the position of the
    /// column that follows it, so `f.apply(i, pos(i))` gives `pos(i + 1)`.
    ///
    /// To understand the why of those shifted parameters, read [#getColumnPos(int)].
    ///
    /// You can check [VariableTableHelper#computeColumnPos(int, double)] for an example.
    public ColumnsLayoutCache<T> setPositionFunction(BiFunction<Integer, Double, Double> xPosFn) {
        this.xPosFn = xPosFn;
        return this;
    }

    //================================================================================
    // Internal Classes
    //================================================================================

    /// Nothing special, just an extension of [HashMap] to store data about columns' layout as [LayoutInfo] objects.
    ///
    /// Makes the variable declarations shorter and offers a bunch of convenience methods, that's all.
    ///
    /// @see LayoutInfo
    public class LayoutInfoCache extends HashMap<VFXTableColumn<T, ?>, LayoutInfo> {

        // Every column in the table must have an entry in this map, invariant guaranteed by handleColumns().
        // A missing entry means the cache is out of sync with the table: since every layout computation depends on this
        // data, going on would just produce a wrong layout with no hint whatsoever about the cause. Fail loudly instead
        private LayoutInfo require(VFXTableColumn<T, ?> column) {
            LayoutInfo li = get(column);
            if (li == null) {
                String name = (column == null) ? "null" : ofNullable(column.getText()).orElse(column.toString());
                throw new IllegalStateException(
                    "The layout cache is out of sync with the table. No layout info found for column: " + name
                );
            }
            return li;
        }

        //================================================================================
        // Width
        //================================================================================
        public double getWidth(VFXTableColumn<T, ?> column) {
            return require(column).getWidth();
        }

        public boolean isWidthValid(VFXTableColumn<T, ?> column) {
            return require(column).isWidthValid();
        }

        // Lenient on purpose: invalidateLast() may run when there is no last column at all, or when the previous last
        // column has just been removed from the table (and thus from this map)
        private void invalidateWidth(VFXTableColumn<T, ?> column) {
            LayoutInfo li = get(column);
            if (li == null) return;
            li.invalidateWidth();
        }

        //================================================================================
        // Position
        //================================================================================
        public double getPos(int index) {
            ObservableList<VFXTableColumn<T, ? extends VFXTableCell<T>>> columns = table.getColumns();
            return require(columns.get(index)).getPos();
        }

        private void setPos(int index, double pos) {
            ObservableList<VFXTableColumn<T, ? extends VFXTableCell<T>>> columns = table.getColumns();
            if (index > columns.size() - 1) return;
            require(columns.get(index)).setPos(pos);
        }

        //================================================================================
        // Misc
        //================================================================================
        public void clearPositionCache() {
            values().forEach(LayoutInfo::resetPos);
        }

        @Override
        public void clear() {
            values().forEach(LayoutInfo::dispose);
            super.clear();
        }
    }

    /// Wrapper class for layout data related to a specific [VFXTableColumn].
    /// This stores: its index in [VFXTable#getColumns()] [init:-1], its width as a [DoubleBinding], and its x
    /// position [default:-1.0].
    ///
    /// **Width handling**
    ///
    /// For better performance, the column's width is stored as a binding, so the value is computed lazily (only upon request).
    /// Invalidation is handled "manually". Check [#createWidthBinding()] for more details.
    ///
    /// **Why -1 as a sentinel**
    ///
    /// Both the index and the position use a negative value to mean 'not computed yet, or invalidated'. Neither can
    /// legitimately be negative (column 0 sits at x 0), so one field carries both the value and its validity, with no
    /// boxing and no companion flag. See [#getIndex()] and [#getPos()].
    public class LayoutInfo implements Comparable<LayoutInfo> {
        //================================================================================
        // Properties
        //================================================================================
        private VFXTableColumn<T, ?> column;
        private int index = -1;
        private DoubleBinding wBinding;
        private double pos = -1.0;

        //================================================================================
        // Constructors
        //================================================================================
        public LayoutInfo(VFXTableColumn<T, ?> column) {
            this.column = column;
            this.wBinding = createWidthBinding();
        }

        //================================================================================
        // Methods
        //================================================================================

        /// @return the column instance the layout data refers to
        public VFXTableColumn<T, ?> getColumn() {
            return column;
        }

        /// @return the column's index in [VFXTable#getColumns()], computed the first time (then cached)
        ///
        /// **Note:** this purposefully does not go through [VFXTable#indexOf(VFXTableColumn)]. That method relies on
        /// [VFXTableColumn#indexProperty()], which is refreshed by the skin during the layout pass, and thus is stale
        /// in the window that goes from a change in [VFXTable#getColumns()] to the next layout. Since this is
        /// re-computed only when invalidated by [#invalidateIndex()] (so, once per structural change at most), we can
        /// afford to ask the list itself, which is always right.
        public int getIndex() {
            if (index == -1) index = table.getColumns().indexOf(column);
            return index;
        }

        /// Resets, and thus invalidates, the cached index to -1, so that it will be recomputed by [#getIndex()].
        ///
        /// Needed because any structural change in [VFXTable#getColumns()] (additions, removals, permutations) is
        /// likely to shift the columns' indexes.
        private void invalidateIndex() {
            index = -1;
        }

        /// Calls [DoubleBinding#get()] on the column's width binding.
        public double getWidth() {
            return wBinding.get();
        }

        /// @return whether [DoubleBinding#isValid()] is true
        public boolean isWidthValid() {
            return wBinding.isValid();
        }

        /// Invalidates the width binding by calling [DoubleBinding#invalidate()].
        private void invalidateWidth() {
            wBinding.invalidate();
        }

        /// @return the stored column's x position
        public double getPos() {
            return pos;
        }

        /// Sets the column's x position.
        private void setPos(double pos) {
            this.pos = pos;
        }

        /// Resets, and thus invalidates, the column's x position to -1.0
        private void resetPos() {
            setPos(-1.0);
        }

        /// This is responsible for creating the [DoubleBinding] which computes the column's width by using
        /// the function set by [#setWidthFunction(BiFunction)].
        ///
        /// It returns an inline custom binding which depends on [VFXTableColumn#prefWidthProperty()]. When this
        /// property changes two things must happen:
        ///
        /// 1) obviously the binding must become invalid, because now the width function may return a different value
        ///
        /// 2) we must partially invalidate the positions. By partial, I mean only the columns starting from
        /// [#getIndex()] to the last one, since a width change can only move the columns that come after it. The
        /// walk also stops at the first already-invalid position, because everything past it is invalid too.
        private DoubleBinding createWidthBinding() {
            return new DoubleBinding() {
                {
                    bind(column.prefWidthProperty());
                }

                private void invalidatePartial() {
                    VFXTable<T> table = getTable();
                    ObservableList<VFXTableColumn<T, ? extends VFXTableCell<T>>> columns = table.getColumns();
                    int index = getIndex();
                    int size = columns.size();
                    for (int i = index; i < size; i++) {
                        if (cache.getPos(i) == -1.0) break;
                        cache.setPos(i, -1.0);
                    }
                }

                @Override
                protected double computeValue() {
                    return widthFn.apply(column, column == lColumn);
                }

                @Override
                protected void onInvalidating() {
                    invalidatePartial();
                    invalidatingAction.accept(column == lColumn);
                }

                @Override
                public void dispose() {
                    unbind(column.prefWidthProperty());
                }
            };
        }

        /// Calls [DoubleBinding#dispose()] and sets both the binding and the column instances to `null`.
        private void dispose() {
            wBinding.dispose();
            wBinding = null;
            column = null;
        }

        //================================================================================
        // Overridden Methods
        //================================================================================
        @Override
        public int compareTo(LayoutInfo o) {
            return Integer.compare(getIndex(), o.getIndex());
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LayoutInfo that = (LayoutInfo) o;
            return Objects.equals(getColumn(), that.getColumn());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getColumn());
        }
    }
}
