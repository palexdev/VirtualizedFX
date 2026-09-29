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

import java.util.Arrays;

import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.enums.ColumnsFillPolicy;
import io.github.palexdev.virtualizedfx.table.VFXTableHelper.VFXDefaultTableHelper;
import io.github.palexdev.virtualizedfx.table.defaults.VFXTableColumnBehavior;
import javafx.beans.binding.DoubleBinding;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/// Cache mechanism to simplify and vastly improve the columns' layout performance, used by the [VFXDefaultTableHelper].
///
/// Columns may have different widths, which makes some computations way more expensive. A column's position cannot be
/// determined by a simple multiplication, but it's the sum of all previous columns' widths ([a prefix sum](https://www.geeksforgeeks.org/dsa/understanding-prefix-sums/)).
/// This cache computes positions and widths on request, and keeps them until a change invalidates them, which in other
/// words means that the cache is 'lazy'. Positions being a prefix sum also means they are monotonic, therefore
/// binary-searchable, which is how the helper finds the columns that fall in the viewport in `O(log n)`.
///
/// ## The width model
///
/// Every column has a **natural width**: the greater between its [VFXTableColumn#userPrefWidthProperty()] and the width
/// specified by the [VFXTable#columnsSizeProperty()], which acts as the minimum. A pref below the minimum is kept anyway,
/// and it comes back into play if the minimum shrinks.
///
/// When the natural widths don't cover the table, the leftover width is distributed as specified by the
/// [VFXTable#columnsFillPolicyProperty()]. Both policies are implemented as a vector of **effective weights**:
/// [ColumnsFillPolicy#LAST] is weight 1 on the last column and 0 on every other, [ColumnsFillPolicy#WEIGHTED] uses the
/// columns' weights ([VFXTable#getWeight(VFXTableColumn)]). A column with a positive effective weight is an **absorber**.
/// So, the whole model is:
///
/// ```
/// leftover = max(0, tableWidth - sum(natural widths))
/// width(i) = natural(i) + leftover * weight(i)/ totalWeight
///```
///
/// If the total weight is 0, which can only happen with `WEIGHTED` and no positive weight, nothing absorbs the leftover
/// width and the columns simply do not fill the table.
///
/// The `max(0, ...)` puts the table in one of two regimes. When the columns are wider than the table, there is no
/// leftover width, the weights play no role, and the table scrolls horizontally. Otherwise, the columns fill the table
/// (if something absorbs), and there is nothing to scroll.
///
/// ## How data is stored
///
/// Two prefix arrays, both one element longer than the columns, where index `i` holds the sum over the columns before `i`:
/// - the natural positions, valid up to a certain index and computed further only when a position past it is requested.
///   A change to column `k` invalidates only the positions after `k`, the ones before stay valid.
/// - the cumulative effective weights, rebuilt as a whole when stale.
///
/// A column's position is its natural position plus the leftover width absorbed by the columns before it,
/// `naturalPos(i) + leftover * cumulativeWeight(i)/ totalWeight`. The sum of the natural widths, the leftover width
/// and the total weight are cached aggregates, so none of them requires iterating over the columns.
///
/// ## Snapping
///
/// Nothing is stored snapped. Positions and widths are snapped on read, see [#posAt(int)] and [#widthAt(int)], in pairs:
/// a column's width is the difference between two snapped positions, so its right edge **is** the next column's left edge,
/// on a whole pixel, with no seams or overlaps. A change of the render scale requires no invalidation.
///
/// ## Why this extends [DoubleBinding]
///
/// The cache also computes the [VFXTable#virtualMaxXProperty()], because the two are tightly coupled: `virtualMaxX` is
/// where the columns end, the position after the last column. See [#computeValue()].
///
/// The binding has no dependencies. The helper forwards the relevant changes to the `on...` handlers, which invalidate
/// only what the change affects. Those that return an `int` also tell which columns need to be laid out again: the index
/// of the first column whose position or width changed, or -1 if none did.
public class ColumnsLayoutCache<T> extends DoubleBinding {

    //================================================================================
    // Properties
    //================================================================================

    private VFXTable<T> table;
    private int columnsCount;
    private double minColumnsWidth; // From VFXTable#getColumnsSize().width()

    // Raw VFXTableColumn#getUserPrefWidth() values, -1 when unset
    // How many userPrefWidths are greater than minColumnsWidth and their sum
    private double[] userPrefWidths;
    private int overridesCount;
    private double overridesWidth;

    // [i] = sum of the natural widths of the columns before i
    // Entries past `posValidUpTo` are stale
    private double[] naturalPositions;
    private int posValidUpTo;

    // [i] = sum of the effective weights of the columns before i
    private int[] cumulativeWeights;
    private int totalWeight = Integer.MIN_VALUE;
    // First column with a positive effective weight, -1 when none. Stale together with totalWeight
    private int firstAbsorber = -1;

    private double leftoverWidth = Double.NaN;

    //================================================================================
    // Constructors
    //================================================================================

    public ColumnsLayoutCache(VFXTable<T> table) {
        this.table = table;
    }

    //================================================================================
    // Methods
    //================================================================================

    /// Builds the cache from the table's current state, see [#rebuild()]. Must be called before using the cache.
    public ColumnsLayoutCache<T> init() {
        rebuild();
        return this;
    }

    /// Reads everything from scratch: the number of columns, the minimum width and every column's pref, from which the
    /// aggregates are computed. The arrays are re-allocated, and all the lazy data is marked stale.
    private void rebuild() {
        ObservableList<VFXTableColumn<T, ? extends VFXTableCell<T>>> columns = table.columns();
        columnsCount = columns.size();
        minColumnsWidth = table.getColumnsSize().width();
        userPrefWidths = new double[columnsCount];
        overridesCount = 0;
        overridesWidth = 0.0;
        for (int i = 0; i < columnsCount; i++) {
            double pref = columns.get(i).getUserPrefWidth();
            userPrefWidths[i] = pref;
            if (pref > minColumnsWidth) {
                overridesCount++;
                overridesWidth += pref;
            }
        }
        naturalPositions = new double[columnsCount + 1];
        posValidUpTo = 0;
        cumulativeWeights = new int[columnsCount + 1];
        totalWeight = Integer.MIN_VALUE;
        leftoverWidth = Double.NaN;
    }

    /// @return the natural width of the column at the given index, the greater between its pref and the minimum width
    private double naturalWidthAt(int index) {
        return Math.max(userPrefWidths[index], minColumnsWidth);
    }

    /// @return the sum of all the natural widths. Computed in `O(1)`: the columns at the minimum width count as the minimum,
    /// the others as the sum of their prefs.
    private double totalNaturalWidth() {
        return minColumnsWidth * (columnsCount - overridesCount) + overridesWidth;
    }

    /// Computes the leftover width, `max(0, tableWidth - totalNaturalWidth)`, if stale.
    ///
    /// @return the leftover width, 0 when the columns are as wide as the table or wider
    private double leftoverWidth() {
        if (Double.isNaN(leftoverWidth))
            leftoverWidth = Math.max(0.0, table.getWidth() - totalNaturalWidth());
        return leftoverWidth;
    }

    /// Rebuilds the effective weights, and finds the first absorber, if stale.
    ///
    /// With [ColumnsFillPolicy#LAST] the whole weight, 1, goes to the last column. With [ColumnsFillPolicy#WEIGHTED] the
    /// columns' weights are read, see [VFXTable#getWeight(VFXTableColumn)], with negative ones counting as 0.
    ///
    /// @return the sum of the effective weights
    private int totalWeight() {
        if (totalWeight < 0) {
            Arrays.fill(cumulativeWeights, 0);
            firstAbsorber = -1;
            if (columnsCount > 0) {
                if (table.getColumnsFillPolicy() == ColumnsFillPolicy.LAST) {
                    cumulativeWeights[columnsCount] = 1;
                    firstAbsorber = columnsCount - 1;
                } else {
                    ObservableList<VFXTableColumn<T, ? extends VFXTableCell<T>>> columns = table.columns();
                    for (int i = 0; i < columnsCount; i++) {
                        int weight = Math.max(0, VFXTable.getWeight(columns.get(i)));
                        if (weight > 0 && firstAbsorber < 0) firstAbsorber = i;
                        cumulativeWeights[i + 1] = cumulativeWeights[i] + weight;
                    }
                }
            }
            totalWeight = cumulativeWeights[columnsCount];
        }
        return totalWeight;
    }

    /// @return the index of the first column with a positive effective weight, -1 if there is none. The effective
    /// weights are rebuilt first, if stale.
    private int firstAbsorber() {
        totalWeight();
        return firstAbsorber;
    }

    /// Computes the position of the column at the given index, not snapped. The natural positions are extended up to the
    /// index if needed, then the leftover width absorbed by the previous columns is added.
    ///
    /// The index can be equal to the number of columns, in which case the result is where the columns end.
    private double rawPosAt(int index) {
        if (index > posValidUpTo) {
            for (int i = posValidUpTo + 1; i <= index; i++)
                naturalPositions[i] = naturalPositions[i - 1] + naturalWidthAt(i - 1);
            posValidUpTo = index;
        }

        double natural = naturalPositions[index];
        double leftover = leftoverWidth();
        if (leftover == 0.0) return natural;

        int total = totalWeight();
        return (total == 0) ? natural : natural + leftover * cumulativeWeights[index] / total;
    }

    /// @return the width of the column at the given index, snapped. It is the difference between the snapped positions
    /// of the next column and of this one, so that adjacent columns always meet on the same pixel.
    public double widthAt(int index) {
        return table.snapPositionX(rawPosAt(index + 1)) - table.snapPositionX(rawPosAt(index));
    }

    /// @return the position of the column at the given index, snapped
    public double posAt(int index) {
        return table.snapPositionX(rawPosAt(index));
    }

    /// Handles changes of the columns' minimum width, [VFXTable#columnsSizeProperty()].<br >
    /// Does nothing if the width did not change (e.g. only the height did). Otherwise, the aggregates are recomputed from
    /// the stored prefs, since the columns above the minimum may be different ones, and every position is marked stale.
    protected void onColumnsSizeChanged() {
        // assume not-null
        double newColumnsWidth = table.getColumnsSize().width();
        if (newColumnsWidth == minColumnsWidth) return;

        minColumnsWidth = newColumnsWidth;
        overridesCount = 0;
        overridesWidth = 0.0;
        for (int i = 0; i < columnsCount; i++) {
            double pref = userPrefWidths[i];
            if (pref > newColumnsWidth) {
                overridesCount++;
                overridesWidth += pref;
            }
        }
        posValidUpTo = 0;
        leftoverWidth = Double.NaN;
        invalidate();
    }

    /// Handles changes of a column's [VFXTableColumn#userPrefWidthProperty()].
    ///
    /// The new pref is stored in any case, but if the natural width did not change (e.g. both the old and the new pref are
    /// below the minimum) nothing else happens. Otherwise, the aggregates are updated and the positions from the column on
    /// are marked stale.
    ///
    /// @return -1 if the natural width did not change. The column's index if the leftover width did not change (so only
    /// the columns from this one on moved). Otherwise, the absorbers changed width too, so the smaller between the column's
    /// index and the first absorber's.
    protected int onColumnResized(VFXTableColumn<T, ?> column) {
        int index = column.getIndex();
        double old = userPrefWidths[index];
        double pref = column.getUserPrefWidth();
        userPrefWidths[index] = pref;
        if (Math.max(old, minColumnsWidth) == Math.max(pref, minColumnsWidth)) return -1;

        if (old > minColumnsWidth) {
            overridesCount--;
            overridesWidth -= old;
        }
        if (pref > minColumnsWidth) {
            overridesCount++;
            overridesWidth += pref;
        }

        if (index < posValidUpTo) posValidUpTo = index;
        double oldLeftoverWidth = leftoverWidth;
        leftoverWidth = Double.NaN;
        invalidate();
        if (leftoverWidth() == oldLeftoverWidth) return index;

        int first = firstAbsorber();
        return first < 0 ? index : Math.min(index, first);
    }

    /// Handles changes of the table's width, which only ever affect the leftover width.
    ///
    /// @return -1 if the leftover width did not change (e.g. the columns are wider than the table before and after).
    /// Otherwise, the index of the first absorber, since it and all the columns after it changed (-1 if there is none).
    protected int onTableWidthChanged() {
        double oldLeftoverWidth = leftoverWidth;
        leftoverWidth = Double.NaN;
        if (leftoverWidth() == oldLeftoverWidth) return -1;

        invalidate();
        return firstAbsorber();
    }

    /// Handles changes of the [VFXTable#columnsFillPolicyProperty()] by marking the effective weights stale.
    protected void onFillPolicyChanged() {
        totalWeight = Integer.MIN_VALUE;
        invalidate();
    }

    /// Handles changes of a column's weight, see [VFXTable#setWeight(VFXTableColumn,int)].
    ///
    /// @return -1 if the change has no effect: the policy is [ColumnsFillPolicy#LAST] (the columns' weights are not used),
    /// the effective weight is the same, or there's no leftover width to distribute. Otherwise, the smaller between the
    /// column's index and the first absorber's.
    protected int onColumnWeightChanged(VFXTableColumn<T, ?> column) {
        if (table.getColumnsFillPolicy() == ColumnsFillPolicy.LAST) return -1;

        int index = column.getIndex();
        int weight = Math.max(0, VFXTable.getWeight(column));
        if (totalWeight >= 0 && cumulativeWeights[index + 1] - cumulativeWeights[index] == weight) return -1;

        totalWeight = Integer.MIN_VALUE;
        invalidate();
        if (leftoverWidth() == 0.0) return -1;

        int first = firstAbsorber();
        return first < 0 ? index : Math.min(index, first);
    }

    /// Handles changes in the table's columns' list by rebuilding the whole cache, see [#rebuild()].
    ///
    /// The change itself is not inspected for now. It is possible to optimize this by partially invalidating the cache,
    /// but the gains are not worth the complexity.
    protected void onColumnsChanged(ListChangeListener.Change<? extends VFXTableColumn<T, ?>> change) {
        // TODO can be optimized, but it's not worth it right now
        rebuild();
        invalidate();
    }

    /// Checks whether the column at the given index is a **shared absorber**: it takes part of the leftover width, but not
    /// all of it, since other columns absorb too. In other words, its effective weight is positive and less than the total.
    ///
    /// The check is on the effective weights rather than on [VFXTable#getWeight(VFXTableColumn)] because the two can
    /// differ. With [ColumnsFillPolicy#LAST], the last column absorbs everything whatever its declared weight is, and the
    /// weights declared on the other columns absorb nothing. So, under `LAST` no column is a shared absorber.
    ///
    /// Note that the check does not care whether there is leftover width at the moment.
    ///
    /// This exists for the resize rule of the default column behavior, see [VFXTableColumnBehavior].
    public boolean isSharedAbsorber(int columnIndex) {
        int total = totalWeight();
        int weight = cumulativeWeights[columnIndex + 1] - cumulativeWeights[columnIndex];
        return weight > 0 && weight < total;
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    /// Computes where the columns end, which is the table's [VFXTable#virtualMaxXProperty()].
    ///
    /// When the columns fill the table, this is the table's width, exactly and not snapped. Snapping rounds to the nearest
    /// pixel, so the result could exceed the table's width by half a pixel, and make the horizontal scroll bar appear with
    /// nothing to scroll. Otherwise, it's the sum of the natural widths, snapped.
    @Override
    protected double computeValue() {
        double leftover = leftoverWidth();
        boolean fills = leftover != 0.0 && totalWeight() != 0;
        return fills ? table.getWidth() : table.snapPositionX(totalNaturalWidth());
    }

    @Override
    public void dispose() {
        table = null;
        super.dispose();
    }

    //================================================================================
    // Getters/Setters
    //================================================================================

    public VFXTable<T> getTable() {
        return table;
    }
}
