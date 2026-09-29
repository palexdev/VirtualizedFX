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

package io.github.palexdev.virtualizedfx.enums;

import io.github.palexdev.virtualizedfx.table.VFXTable;
import io.github.palexdev.virtualizedfx.table.VFXTableColumn;
import io.github.palexdev.virtualizedfx.table.VFXTableHelper.VFXDefaultTableHelper;

/// Enumeration to specify how a [VFXTable] distributes the **leftover width** among its columns, see [VFXTable#columnsFillPolicyProperty()].
///
/// The leftover width is the space that would be left empty when the columns, all together, are narrower than the table.
/// When they are as wide as the table or wider, there is nothing to distribute, and the policy has no effect.<br >
/// A column that gets part of the leftover width grows by that amount. A policy never shrinks a column.
///
/// These are the semantics implemented by the default helper, [VFXDefaultTableHelper].
///
/// ## Absorbers on the left
///
/// I know this one looks weird the first time, so it's worth explaining. Let's call **absorbers** the columns that get
/// part of the leftover width. While the table is filled, changing a column's width changes the leftover width, and the
/// absorbers take the difference. If all the absorbers are on the left of the column, the column's right edge cannot move:
/// widening it shrinks them, so the column grows towards the left. When resizing with the mouse, this looks inverted,
/// the edge does not follow the cursor.<br >
/// It can only happen with [#WEIGHTED], since with [#LAST] the only absorber is the last column.
///
/// It is the correct result though: the table stays filled, and every width change, be it a gesture, code or an autosize,
/// goes through the same rule, with no history involved.
///
/// ## Comparison with AG Grid
///
/// The weights idea comes from AG Grid's flex columns, see [Column Sizing](https://www.ag-grid.com/javascript-data-grid/column-sizing/).<br >
/// The two work differently, though:
/// - **What a weight distributes.** In AG Grid, a flex column's width is its share of the space left by the other columns,
///   its own width is ignored (only its min and max apply). Here, an absorber keeps its width, and its share of the
///   leftover width goes on top of it.
/// - **Width changes.** During a drag, AG Grid freezes the flex columns on the left of the dragged one at their current
///   width, and only those on the right share the space. So, the edge always follows the cursor, but the table can be
///   left with a gap, or overflow. The gap is filled all at once the next time the grid is resized, with a jump.
///   Autosize, instead, redistributes on all the flex columns, so the same width change gives different results
///   depending on how it's made.<br >
///   Here, there is one rule for every change: nothing jumps, and the table never shows a gap while something can absorb
///   it. The price is the inversion described above.
public enum ColumnsFillPolicy {

    /// The last column takes all the leftover width. Weights are ignored.
    LAST,

    /// The leftover width is split among the columns proportionally to their weights, see [VFXTable#setWeight(VFXTableColumn,int)].<br >
    /// For example, with two columns of weights 1 and 2, the first one gets a third of the space, the second one two thirds,
    /// and every other column (weight 0) gets nothing.
    WEIGHTED;

    /// The key under which a column's weight is stored in its properties map, see [VFXTable#setWeight(VFXTableColumn,int)].
    public static final String WEIGHT_KEY = "COLUMN_FILL_WEIGHT";

    /// The weight of a column that never had one set, 0: the column does not take any leftover width with [#WEIGHTED].
    public static final int DEFAULT_WEIGHT = 0;

    /// The policy tables use by default, [#LAST].
    public static final ColumnsFillPolicy DEFAULT_POLICY = LAST;
}
