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

package io.github.palexdev.virtualizedfx.utils;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;

public class Utils {
	//================================================================================
	// Static Properties
	//================================================================================
	/**
	 * Special instance of {@link IntegerRange} with both {@code min} and {@code max} set to -1.
	 * This value is in fact invalid as indexes.
	 * <p>
	 * Avoids having to instantiate a new range everytime such values are needed.
	 */
	public static final IntegerRange INVALID_RANGE = IntegerRange.of(-1);

	//================================================================================
	// Constructors
	//================================================================================
	private Utils() {}

	//================================================================================
	// Static Methods
	//================================================================================

	/**
	 * Finds the {@link IntegerRange} which is the intersection between the two given ranges.
	 * <p>
	 * The {@code min} is given by {@code Math.max(r1Min, r2Min}, while the {@code max} is given by {@code Math.min(r1Max, r2Max}.
	 */
	public static IntegerRange intersection(IntegerRange r1, IntegerRange r2) {
		int min = Math.max(r1.getMin(), r2.getMin());
		int max = Math.min(r1.getMax(), r2.getMax());
		try {
			return IntegerRange.of(min, max);
		} catch (Exception ex) {
			return INVALID_RANGE;
		}
	}
}
