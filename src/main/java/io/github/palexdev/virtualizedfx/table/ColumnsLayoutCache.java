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

import javafx.beans.binding.DoubleBinding;
import javafx.collections.ListChangeListener;

public class ColumnsLayoutCache<T> extends DoubleBinding {

    //================================================================================
    // Properties
    //================================================================================

    private VFXTable<T> table;
    private int size;

    private double baseline;
    private double[] widths;
    private int overrides;
    private double sumOverrides;

    private double[] positions;
    private int validUpTo;

    private double slack;

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
        size = table.columns().size();
        baseline = table.getColumnsSize().width();
        widths = new double[size];
        Arrays.fill(widths, -1.0);
        overrides = 0;
        sumOverrides = 0.0;
        positions = new double[size + 1];
        validUpTo = 0;
        slack = Double.NaN;
    }

    private double naturalTotal() {
        return baseline * (size - overrides) + sumOverrides;
    }

    public double widthAt(int index) {
        double w = Math.max(widths[index], baseline);
        if (index != size - 1) return w;
        return w + Math.max(0.0, table.getWidth() - naturalTotal());
    }

    public double posAt(int index) {
        if (index > validUpTo) {
            for (int i = validUpTo + 1; i <= index; i++) positions[i] = positions[i - 1] + widthAt(i - 1);
            validUpTo = index;
        }
        return positions[index];
    }

    protected void onColumnsSizeChanged() {
        // assume not-null
        double newBaseline = table.getColumnsSize().width();
        if (newBaseline == baseline) return;

        baseline = newBaseline;
        overrides = 0;
        sumOverrides = 0.0;
        for (int i = 0; i < size; i++) {
            double pref = widths[i];
            if (pref > newBaseline) {
                overrides++;
                sumOverrides += pref;
            }
        }
        validUpTo = 0;
        invalidate();
    }

    protected int onColumnResized(VFXTableColumn<T, ?> column) {
        int index = column.getIndex();
        double old = widths[index];
        double pref = column.getUserPrefWidth();
        widths[index] = pref;
        if (Math.max(old, baseline) == Math.max(pref, baseline)) return -1;

        if (old > baseline) {
            overrides--;
            sumOverrides -= old;
        }
        if (pref > baseline) {
            overrides++;
            sumOverrides += pref;
        }

        if (index < validUpTo) validUpTo = index;
        invalidate();
        return index;
    }

    protected int onWeightsChanged() {
        // TODO could be optimized for LAST policy, low priority
        double newSlack = Math.max(0.0, table.getWidth() - naturalTotal());
        if (newSlack == slack) return -1;

        slack = newSlack;
        validUpTo = 0;
        invalidate();
        return size - 1;
    }

    protected void onColumnsChanged(ListChangeListener.Change<? extends VFXTableColumn<T, ?>> change) {
        // TODO can be optimized
        rebuild();
        for (int i = 0; i < size; i++) {
            double pref = table.columns().get(i).getUserPrefWidth();
            widths[i] = pref;
            if (pref > baseline) {
                overrides++;
                sumOverrides += pref;
            }
        }
        invalidate();
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    @Override
    protected double computeValue() {
        if (size == 0) return 0.0;
        return Math.max(naturalTotal(), table.getWidth());
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
