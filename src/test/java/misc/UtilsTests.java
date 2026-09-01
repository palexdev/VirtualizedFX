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

package misc;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.virtualizedfx.utils.Utils;
import org.junit.jupiter.api.Test;

import static io.github.palexdev.virtualizedfx.utils.Utils.INVALID_RANGE;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class UtilsTests {

    @Test
    public void testDifferenceShiftRight() {
        assertEquals(IntegerRange.of(7, 7), Utils.difference(IntegerRange.of(1, 7), IntegerRange.of(0, 6)));
        assertEquals(IntegerRange.of(7, 10), Utils.difference(IntegerRange.of(0, 10), IntegerRange.of(0, 6)));
        assertEquals(IntegerRange.of(5, 5), Utils.difference(IntegerRange.of(5, 5), IntegerRange.of(4, 4)));
    }

    @Test
    public void testDifferenceShiftLeft() {
        assertEquals(IntegerRange.of(0, 3), Utils.difference(IntegerRange.of(0, 6), IntegerRange.of(4, 10)));
        assertEquals(IntegerRange.of(0, 2), Utils.difference(IntegerRange.of(0, 10), IntegerRange.of(3, 10)));
    }

    @Test
    public void testDifferenceDisjoint() {
        assertEquals(IntegerRange.of(10, 16), Utils.difference(IntegerRange.of(10, 16), IntegerRange.of(2, 8)));
        assertEquals(IntegerRange.of(0, 6), Utils.difference(IntegerRange.of(0, 6), IntegerRange.of(10, 16)));
        assertEquals(IntegerRange.of(7, 10), Utils.difference(IntegerRange.of(7, 10), IntegerRange.of(0, 6)));
    }

    @Test
    public void testDifferenceCovered() {
        assertEquals(INVALID_RANGE, Utils.difference(IntegerRange.of(3, 5), IntegerRange.of(0, 10)));
        assertEquals(INVALID_RANGE, Utils.difference(IntegerRange.of(0, 6), IntegerRange.of(0, 6)));
        assertEquals(INVALID_RANGE, Utils.difference(IntegerRange.of(0, 6), IntegerRange.of(0, 10)));
        assertEquals(INVALID_RANGE, Utils.difference(IntegerRange.of(4, 10), IntegerRange.of(0, 10)));
    }

    @Test
    public void testDifferenceGrownBothEnds() {
        // Not exact: the real difference is [0, 2] and [6, 10], the bounding range covers 3..5 too
        assertEquals(IntegerRange.of(0, 10), Utils.difference(IntegerRange.of(0, 10), IntegerRange.of(3, 5)));
    }

    @Test
    public void testDifferenceInvalidOperands() {
        assertEquals(IntegerRange.of(0, 6), Utils.difference(IntegerRange.of(0, 6), INVALID_RANGE));
        assertEquals(INVALID_RANGE, Utils.difference(INVALID_RANGE, IntegerRange.of(0, 6)));
        assertEquals(INVALID_RANGE, Utils.difference(INVALID_RANGE, INVALID_RANGE));
    }
}
