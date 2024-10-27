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

import io.github.palexdev.mfxcore.behavior.BehaviorBase;
import io.github.palexdev.mfxcore.enums.Zone;
import io.github.palexdev.mfxcore.utils.resize.RegionDragResizer;
import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.enums.ColumnsLayoutMode;
import io.github.palexdev.virtualizedfx.table.VFXTable;
import io.github.palexdev.virtualizedfx.table.VFXTableColumn;
import javafx.scene.input.MouseEvent;

import static io.github.palexdev.virtualizedfx.table.defaults.VFXDefaultTableColumn.DRAGGED;

/**
 * This is the default behavior implementation for {@link VFXTableColumn}. This basic behavior instantiates a
 * {@link RegionDragResizer} which allows you to resize the column with the mouse cursor at runtime.
 * <p>
 * For the resizer to work, a series of conditions must be met:
 * <p> 1) the feature must be enabled by the {@link VFXTableColumn#gestureResizableProperty()}
 * <p> 2) the table's instance must not be {@code null}
 * <p> 3) the table's layout mode must be set to {@link ColumnsLayoutMode#VARIABLE}.
 */
public class VFXTableColumnBehavior<T, C extends VFXTableCell<T>> extends BehaviorBase<VFXTableColumn<T, C>> {
    //================================================================================
    // Properties
    //================================================================================
    protected RegionDragResizer resizer;

    //================================================================================
    // Constructors
    //================================================================================
    public VFXTableColumnBehavior(VFXTableColumn<T, C> column) {
        super(column);
    }

    //================================================================================
    // Methods
    //================================================================================

    /**
     * This method is responsible for enabling/disabling the {@link RegionDragResizer} by using {@link RegionDragResizer#makeResizable()}
     * or {@link RegionDragResizer#uninstall()}.
     * <p>
     * Beware, this is automatically called by the default skin when needed. Neither the resizer nor this class check whether
     * it is already enabled, which means that additional calls may add the same handlers multiple times, causing potential
     * memory leaks!
     */
    protected void onResizableChanged() {
        VFXTableColumn<T, C> column = getNode();
        boolean resizable = column.isGestureResizable();
        if (!resizable && resizer != null) {
            resizer.uninstall();
            return;
        }
        if (resizer != null) resizer.makeResizable();
    }

    /**
     * The {@link RegionDragResizer} used here is an inline custom extension which uses this method to determine whether
     * the column can be resized or not.
     */
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
        resizer = new RegionDragResizer(column) {
            @Override
            protected void handleDragged(MouseEvent event) {
                if (canResize()) {
                    super.handleDragged(event);
                    column.pseudoClassStateChanged(DRAGGED, true);
                }
            }

            @Override
            protected void handleMoved(MouseEvent event) {
                if (canResize()) super.handleMoved(event);
            }

            @Override
            protected void handlePressed(MouseEvent event) {
                if (canResize()) super.handlePressed(event);
            }

            @Override
            protected void handleReleased(MouseEvent event) {
                super.handleReleased(event);
                column.pseudoClassStateChanged(DRAGGED, false);
            }
        };
        resizer.setMinWidthFunction(r -> column.getTable().getColumnsSize().getWidth());
        resizer.setAllowedZones(Zone.CENTER_RIGHT);
        resizer.setResizeHandler((node, x, y, w, h) -> column.resize(w));
        if (column.isGestureResizable()) resizer.makeResizable();
    }

    /**
     * {@inheritDoc}
     * <p></p>
     * Also disposed the {@link RegionDragResizer}.
     */
    @Override
    public void dispose() {
        resizer.dispose();
        resizer = null;
        super.dispose();
    }
}
