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

import io.github.palexdev.mfxcore.controls.MFXBehavior;
import io.github.palexdev.mfxcore.enums.Zone;
import io.github.palexdev.mfxcore.utils.fx.resize.Resizer;
import io.github.palexdev.mfxcore.utils.fx.resize.targets.RegionTarget;
import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.table.VFXTableColumn;
import io.github.palexdev.virtualizedfx.table.VFXTableHelper;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import static io.github.palexdev.virtualizedfx.enums.ColumnsFillPolicy.DEFAULT_WEIGHT;
import static io.github.palexdev.virtualizedfx.table.VFXTable.getWeight;
import static io.github.palexdev.virtualizedfx.table.VFXTable.setWeight;
import static java.util.Optional.ofNullable;

public class VFXTableColumnBehavior<T, C extends VFXTableCell<T>> extends MFXBehavior<VFXTableColumn<T, C>> {

    //================================================================================
    // Properties
    //================================================================================

    private Resizer<VFXTableColumn<?, ?>> resizer;

    //================================================================================
    // Constructors
    //================================================================================

    public VFXTableColumnBehavior(VFXTableColumn<T, C> column) {
        super(column);
    }

    //================================================================================
    // Methods
    //================================================================================

    protected Resizer<VFXTableColumn<?, ?>> createResizer() {
        return new ColumnResizer(getNode());
    }

    public Resizer<VFXTableColumn<?, ?>> getResizer() {
        return resizer;
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    @Override
    public void install() {
        if ((resizer = createResizer()) != null)
            resizer.install();
    }

    @Override
    public void dispose() {
        if (resizer != null) resizer.dispose();
        super.dispose();
    }

    //================================================================================
    // Inner Classes
    //================================================================================

    protected static class ColumnResizer extends Resizer<VFXTableColumn<?, ?>> {
        private double prefAtPress;
        private int weightAtPress = DEFAULT_WEIGHT;

        public ColumnResizer(VFXTableColumn<?, ?> column) {
            super(new RegionTarget<>(column));
            hitSourceProperty().bind(column.tableProperty());
            condition((_, _) -> column().isGestureResizable());
            allowedZones(Zone.CENTER_RIGHT);
            resizeHandler((c, _, _, w, _) -> onResize(c, w));
        }

        protected <T> void onResize(VFXTableColumn<T, ?> column, double width) {
            VFXTableHelper<T> helper = column.getTable().getHelper();
            if (helper.isSharedAbsorber(column)) setWeight(column, DEFAULT_WEIGHT);
            column.setUserPrefWidth(width);
        }

        @Override
        protected void onMousePressed(MouseEvent me) {
            super.onMousePressed(me);
            if (!isResizing()) return;

            VFXTableColumn<?, ?> column = column();
            if (me.getButton() == MouseButton.PRIMARY && me.getClickCount() % 2 == 0) {
                column.sizeToContent();
                return;
            }

            prefAtPress = column.getUserPrefWidth();
            weightAtPress = getWeight(column);
        }

        @Override
        public boolean cancel() {
            if (!isResizing()) return false;
            VFXTableColumn<?, ?> column = column();
            setWeight(column, weightAtPress);
            column.setUserPrefWidth(prefAtPress);
            ofNullable(onCancelled()).ifPresent(Runnable::run);
            resetState();
            return true;
        }

        public VFXTableColumn<?, ?> column() {
            return target().target();
        }
    }
}
