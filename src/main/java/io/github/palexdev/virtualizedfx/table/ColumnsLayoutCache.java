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
import javafx.beans.binding.DoubleBinding;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

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

    public ColumnsLayoutCache<T> init() {
        rebuild();
        return this;
    }

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

    private double naturalWidthAt(int index) {
        return Math.max(userPrefWidths[index], minColumnsWidth);
    }

    private double totalNaturalWidth() {
        return minColumnsWidth * (columnsCount - overridesCount) + overridesWidth;
    }

    private double leftoverWidth() {
        if (Double.isNaN(leftoverWidth))
            leftoverWidth = Math.max(0.0, table.getWidth() - totalNaturalWidth());
        return leftoverWidth;
    }

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

    private int firstAbsorber() {
        totalWeight();
        return firstAbsorber;
    }

    public double widthAt(int index) {
        double natural = naturalWidthAt(index);
        double leftover = leftoverWidth();
        if (leftover == 0.0) return natural;

        int total = totalWeight();
        if (total == 0) return natural;

        int weight = cumulativeWeights[index + 1] - cumulativeWeights[index];
        return (weight == 0) ? natural : natural + leftover * weight / total;
    }

    public double posAt(int index) {
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

    protected int onTableWidthChanged() {
        double oldLeftoverWidth = leftoverWidth;
        leftoverWidth = Double.NaN;
        if (leftoverWidth() == oldLeftoverWidth) return -1;

        invalidate();
        return firstAbsorber();
    }

    protected void onFillPolicyChanged() {
        totalWeight = Integer.MIN_VALUE;
        invalidate();
    }

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

    protected void onColumnsChanged(ListChangeListener.Change<? extends VFXTableColumn<T, ?>> change) {
        // TODO can be optimized, but it's not worth it right now
        rebuild();
        invalidate();
    }

    public boolean isSharedAbsorber(int columnIndex) {
        int total = totalWeight();
        int weight = cumulativeWeights[columnIndex + 1] - cumulativeWeights[columnIndex];
        return weight > 0 && weight < total;
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    @Override
    protected double computeValue() {
        double natural = totalNaturalWidth();
        double leftover = leftoverWidth();
        return (leftover == 0.0 || totalWeight() == 0) ? natural : natural + leftover;
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
