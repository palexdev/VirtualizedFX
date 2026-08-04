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

package io.github.palexdev.virtualizedfx.table.defaults;

import io.github.palexdev.mfxcore.behavior.MFXBehavior;
import io.github.palexdev.mfxcore.enums.Zone;
import io.github.palexdev.mfxcore.utils.fx.resize.Resizer;
import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.enums.ColumnsLayoutMode;
import io.github.palexdev.virtualizedfx.table.VFXTable;
import io.github.palexdev.virtualizedfx.table.VFXTableColumn;

import static io.github.palexdev.mfxcore.utils.fx.resize.Resizer.resizer;

/// This is the default behavior implementation for [VFXTableColumn]. This basic behavior instantiates a
/// [Resizer] which allows you to resize the column with the mouse cursor at runtime.
///
/// For the resizer to work, a series of conditions must be met:
///
/// 1) the feature must be enabled by the [VFXTableColumn#gestureResizableProperty()]
///
/// 2) the table's instance must not be `null`
///
/// 3) the table's layout mode must be set to [ColumnsLayoutMode#VARIABLE].
public class VFXTableColumnBehavior<T, C extends VFXTableCell<T>> extends MFXBehavior<VFXTableColumn<T, C>> {
    //================================================================================
    // Properties
    //================================================================================

    protected Resizer<VFXTableColumn<T ,C>> resizer;

    //================================================================================
    // Constructors
    //================================================================================
    public VFXTableColumnBehavior(VFXTableColumn<T, C> column) {
        super(column);
    }

    //================================================================================
    // Methods
    //================================================================================

    /// This method is responsible for enabling/disabling the [Resizer] by using [Resizer#install()()]
    /// or [Resizer#uninstall()].
    protected void onResizableChanged() {
        VFXTableColumn<T, C> column = getNode();
        boolean resizable = column.isGestureResizable();
        if (!resizable && resizer != null) {
            resizer.uninstall();
            return;
        }
        if (resizer != null && !resizer.isInstalled()) resizer.install();
    }

    /// The [Resizer] checks for this condition to be `true` before attempting to resize the column.
    protected boolean canResize() {
        VFXTableColumn<T, C> column = getNode();
        VFXTable<T> table = column.getTable();
        return table != null && table.getColumnsLayoutMode() == ColumnsLayoutMode.VARIABLE;
    }

    //================================================================================
    // Overridden Methods
    //================================================================================
    @Override
    public void init() {
        VFXTableColumn<T, C> column = getNode();
        resizer = resizer(column)
            .hitSource(column.getTable())
            .condition((_, _) -> canResize())
            .allowedZones(Zone.CENTER_RIGHT)
            .resizeHandler((c, _, _, w, _) -> c.resize(w));
        if (column.isGestureResizable()) resizer.install();
    }

    /// {@inheritDoc}
    ///
    /// Also disposed the [Resizer] if active.
    @Override
    public void dispose() {
        if (resizer != null) {
            resizer.dispose();
            resizer = null;
        }
        super.dispose();
    }
}
