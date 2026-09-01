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

package io.github.palexdev.virtualizedfx.utils;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;

/// Miscellaneous helpers shared by the virtualized containers.
public class Utils {
    //================================================================================
    // Static Properties
    //================================================================================
    /// Special instance of [IntegerRange] with both `min` and `max` set to -1.
    /// This value is in fact invalid as indexes.
    ///
    /// Avoids having to instantiate a new range every time such values are needed.
    public static final IntegerRange INVALID_RANGE = IntegerRange.of(-1);

    //================================================================================
    // Constructors
    //================================================================================
    private Utils() {}

    //================================================================================
    // Static Methods
    //================================================================================

    /// Finds the [IntegerRange] which is the intersection between the two given ranges.
    ///
    /// The `min` is given by `Math.max(r1Min, r2Min)`, while the `max` is given by `Math.min(r1Max, r2Max)`.
    ///
    /// When the two ranges do not overlap that yields `min > max`, which [IntegerRange] refuses to build. Callers rely
    /// on this rather than on an exception, so the failure is caught and [#INVALID_RANGE] is returned instead: "no
    /// intersection" is a normal answer here, not an error.
    public static IntegerRange intersection(IntegerRange r1, IntegerRange r2) {
        int min = Math.max(r1.getMin(), r2.getMin());
        int max = Math.min(r1.getMax(), r2.getMax());
        try {
            return IntegerRange.of(min, max);
        } catch (Exception ex) {
            return INVALID_RANGE;
        }
    }

    /// The indexes which are in `range` but not in `other`.
    ///
    /// ```
    /// difference([1, 7],   [0, 6])  -> [7, 7]     // shifted right by one, only 7 is new
    /// difference([0, 6],   [4, 10]) -> [0, 3]     // shifted left
    /// difference([10, 16], [2, 8])  -> [10, 16]   // no overlap, everything is new
    /// difference([3, 5],   [0, 10]) -> INVALID    // nothing is new
    /// difference([0, 10],  [3, 5])  -> [0, 10]    // grew at both ends, see below
    /// ```
    ///
    /// The answer is a single range, so it is exact only when `other` overlaps `range` at one end, or not at all.
    /// When `other` sits strictly inside `range` the difference is two separate blocks, and the whole `range` is
    /// returned instead, covering more than it should.
    ///
    /// [#INVALID_RANGE] means "nothing", as it does for [#intersection]. An invalid `other` means nothing was covered
    /// to begin with, so the whole `range` comes back.
    public static IntegerRange difference(IntegerRange range, IntegerRange other) {
        if (INVALID_RANGE.equals(range)) return INVALID_RANGE;
        if (INVALID_RANGE.equals(other)) return range;

        boolean left = range.getMin() < other.getMin();
        boolean right = range.getMax() > other.getMax();
        if (left && right) return range;
        if (left) return IntegerRange.of(range.getMin(), Math.min(range.getMax(), other.getMin() - 1));
        if (right) return IntegerRange.of(Math.max(range.getMin(), other.getMax() + 1), range.getMax());
        return INVALID_RANGE;
    }
}
