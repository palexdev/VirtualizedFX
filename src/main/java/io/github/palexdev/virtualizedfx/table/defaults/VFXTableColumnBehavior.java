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

package io.github.palexdev.virtualizedfx.table.defaults;

import io.github.palexdev.mfxcore.behavior.MFXBehavior;
import io.github.palexdev.mfxcore.enums.Zone;
import io.github.palexdev.mfxcore.utils.fx.resize.Resizer;
import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.table.VFXTableColumn;

import static io.github.palexdev.mfxcore.utils.fx.resize.Resizer.resizer;

public class VFXTableColumnBehavior<T, C extends VFXTableCell<T>> extends MFXBehavior<VFXTableColumn<T, C>> {

    //================================================================================
    // Properties
    //================================================================================

    private Resizer<VFXTableColumn<T, C>> resizer;

    //================================================================================
    // Constructors
    //================================================================================

    public VFXTableColumnBehavior(VFXTableColumn<T, C> column) {
        super(column);
    }

    //================================================================================
    // Methods
    //================================================================================

    protected Resizer<VFXTableColumn<T, C>> createResizer() {
        // TODO for debug purposes
        VFXTableColumn<T, C> column = getNode();
        if (column.getTable() == null) throw new NullPointerException("Table is null, resizer won't work properly!");

        return resizer(column)
            .hitSource(column.getTable())
            .condition((_, _) -> column.isGestureResizable())
            .allowedZones(Zone.CENTER_RIGHT)
            .resizeHandler((c, _, _, w, _) -> c.setUserPrefWidth(w));
    }

    public Resizer<VFXTableColumn<T, C>> getResizer() {
        return resizer;
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    @Override
    public void init() {
        if ((resizer = createResizer()) != null) resizer.install();
    }

    @Override
    public void dispose() {
        if (resizer != null) resizer.dispose();
        super.dispose();
    }
}
