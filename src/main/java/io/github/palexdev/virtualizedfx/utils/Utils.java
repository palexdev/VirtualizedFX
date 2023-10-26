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
	 * The {@code min} is given by {@code Math.max(r1Min, r2Min}, while the {@code max} is given by {@code Math.min(r1Ma, r2Max}.
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
