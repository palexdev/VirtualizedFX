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
import io.github.palexdev.virtualizedfx.table.ColumnsLayoutCache;
import io.github.palexdev.virtualizedfx.table.VFXTable;
import io.github.palexdev.virtualizedfx.table.VFXTableColumn;
import io.github.palexdev.virtualizedfx.enums.ColumnsFillPolicy;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import static io.github.palexdev.virtualizedfx.enums.ColumnsFillPolicy.DEFAULT_WEIGHT;
import static io.github.palexdev.virtualizedfx.table.VFXTable.getWeight;
import static io.github.palexdev.virtualizedfx.table.VFXTable.setWeight;
import static java.util.Optional.ofNullable;

/// This is the default behavior implementation for [VFXTableColumn]. It instantiates a [Resizer], a [ColumnResizer] to
/// be precise, which allows you to resize the column with the mouse cursor at runtime by dragging its right edge.
///
/// ## Resizing with the mouse
///
/// The gesture is available only if the [VFXTableColumn#gestureResizableProperty()] is `true`, and it sets the column's
/// [VFXTableColumn#userPrefWidthProperty()]. Pressing [KeyCode#ESCAPE] during the gesture cancels it, restoring the
/// column as it was before, see [ColumnResizer#cancel()].
///
/// When the table has leftover width (see [ColumnsFillPolicy]), resizing a column that absorbs part of it clears its
/// weight, so the column keeps the width you give it, and the other absorbers take the difference. The only exception
/// is a column that absorbs all the leftover width. See [ColumnResizer#onResize(VFXTableColumn,double)] for the whole rule.
///
/// Beware that when all the absorbers are on the left of the column you resize, the gesture looks inverted, see [ColumnsFillPolicy]
///
/// ## Autosize
///
/// Double-clicking the right edge sizes the column to fit its content, [VFXTableColumn#sizeToContent()].
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

    /// Creates the [Resizer] used by this behavior, a [ColumnResizer] by default. Override this to customize the
    /// gesture, or return `null` to disable it altogether.
    protected Resizer<VFXTableColumn<?, ?>> createResizer() {
        return new ColumnResizer(getNode());
    }

    /// @return the [Resizer] created by [#createResizer()], `null` before [#install()] or if none was created
    public Resizer<VFXTableColumn<?, ?>> getResizer() {
        return resizer;
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    /// {@inheritDoc}
    ///
    /// Creates the [Resizer], [#createResizer()], and installs it, if any.
    @Override
    public void install() {
        if ((resizer = createResizer()) != null)
            resizer.install();
    }

    /// {@inheritDoc}
    ///
    /// Also disposes the [Resizer], if any.
    @Override
    public void dispose() {
        if (resizer != null) resizer.dispose();
        super.dispose();
    }

    //================================================================================
    // Inner Classes
    //================================================================================

    /// The [Resizer] used by default by [VFXTableColumnBehavior]. It holds the whole default gesture: when it's allowed,
    /// what it does on the column's width and weight, the double-click autosize, and how it's canceled.
    ///
    /// The handlers are registered on the column's table, and follow it if the column moves to another one, see
    /// [Resizer#hitSourceProperty()]. Only the right edge can be grabbed, [Zone#CENTER_RIGHT], and only if
    /// [VFXTableColumn#gestureResizableProperty()] is `true`.
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

        /// The resize handler, called on every drag event of the gesture. Sets the column's
        /// [VFXTableColumn#userPrefWidthProperty()] to the given width, but first it may clear the column's weight.
        ///
        /// While the table has leftover width, a column's width is its [natural width][ColumnsLayoutCache] plus its share of the leftover,
        /// see [ColumnsFillPolicy]. Setting the pref of an absorber would not give it the width you drag to, its share
        /// would come on top. So:
        ///
        /// | the dragged column | its weight | the result |
        /// |---|---|---|
        /// | absorbs part of the leftover width, with other columns | cleared | the column takes the dragged width, the other absorbers keep the table filled |
        /// | absorbs all of the leftover width | kept | see below |
        /// | absorbs nothing | untouched | the column takes the dragged width |
        ///
        /// The check is [VFXTable#isSharedAbsorber(VFXTableColumn)], so it's done on the effective weights. With
        /// [ColumnsFillPolicy#LAST] no column is a shared absorber, and the weights declared on the other columns are not
        /// cleared, they still apply if the policy is switched to [ColumnsFillPolicy#WEIGHTED].
        ///
        /// #### Why the sole absorber keeps its weight
        ///
        /// Clearing it would leave the leftover width to nobody, and the table would not be filled anymore. Keeping it
        /// needs no special handling either. The column's width is `natural + max(0, tableWidth - allNaturals)`, which is
        /// the same as `max(natural, tableWidth - othersNaturals)`. The second term is the width at which the table is
        /// exactly filled, so the column cannot be shrunk below it, the edge simply stops following the cursor there.
        /// Widening it past that works, the leftover width goes to 0 and the table starts to scroll horizontally.
        ///
        /// A click on the edge does not clear anything, since this runs only on drag events. Once the weight is cleared,
        /// the next events skip the check, the column is not an absorber anymore. The width the gesture computes starts
        /// from the column's bounds at press, which include the absorbed width, so the edge does not jump.
        protected <T> void onResize(VFXTableColumn<T, ?> column, double width) {
            if (column.getTable().isSharedAbsorber(column)) setWeight(column, DEFAULT_WEIGHT);
            column.setUserPrefWidth(width);
        }

        /// {@inheritDoc}
        ///
        /// If the press starts a gesture, it's a primary button press and the click count is even (a double click), the
        /// column is autosized with [VFXTableColumn#sizeToContent()], which keeps the weight, and the method returns
        /// before saving anything. The gesture stays active until the release.
        ///
        /// Otherwise, if the press starts a gesture, the column's [VFXTableColumn#userPrefWidthProperty()] and weight are
        /// saved, so that [#cancel()] can restore them.
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

        /// Restores the column's [VFXTableColumn#userPrefWidthProperty()] and weight saved at press, then runs the
        /// [Resizer#onCancelled(Runnable)] action and resets the gesture's state.
        ///
        /// It does not call the super method, because that one replays the width at press through the resize handler.
        /// Here, it would clear the weight right after restoring it, and turn a pref that was never set (-1) into a number.
        ///
        /// @return whether there was anything to cancel
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

        /// @return the column this resizer works on
        public VFXTableColumn<?, ?> column() {
            return target().target();
        }
    }
}
