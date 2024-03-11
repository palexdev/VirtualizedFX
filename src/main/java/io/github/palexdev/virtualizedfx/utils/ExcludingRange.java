package io.github.palexdev.virtualizedfx.utils;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;

import java.util.*;

/**
 * A utility class which, given a starting {@link IntegerRange}, allows to exclude some or all of the values for the given
 * range.
 * <p>
 * This basically offers a much faster alternative to {@link IntegerRange#expandRangeToSet(IntegerRange)}. Let's see an example:
 * <pre>
 * {@code
 * // Let's say you have a range of [3, 7] and you want to perform some checks on each value in the range
 * // You want to perform a certain operation on every value for which the check fails
 * // Before this class you could do something like this...
 * IntegerRange range = IntegerRange.of(3, 7);
 * Set<Integer> expanded = IntegerRange.expandRangeToSet(range);
 * for (Integer val : range) {
 *     if (check(val)) {
 *         expanded.remove(val);
 *     }
 * }
 * // At the end of the for you have a Set of values for which the check failed, at this point...
 * for (Integer failingVal : expanded) {
 *     process(failingVal)
 * }
 *
 * //____________________________________________________________________________________________________//
 *
 * // Now, the issue here is that the `expandRangeToSet(...)` operation can be costly, and we don't really need it
 * // By using an ExcludingRange the code becomes like this...
 * IntegerRange range = IntegerRange.of(3, 7);
 * ExcludingRange eRange = ExcludingRange.of(range);
 * for (Integer val : range) {
 *     if (check(val)) {
 *         eRange.exclude(val);
 *     }
 * }
 *
 * for (Integer failingVal : eRange) {
 *     process(failingVal);
 * }
 * // There's no need to expand the whole range to a Set anymore and ExcludingRange also implements Iterable thus offering
 * // a pretty efficient Iterator which allows to use enhanced for loops too.
 * }
 * </pre>
 */
public class ExcludingRange implements Iterable<Integer> {
	//================================================================================
	// Properties
	//================================================================================
	private final IntegerRange range;
	private final Set<Integer> excluded;

	//================================================================================
	// Constructors
	//================================================================================
	public ExcludingRange(IntegerRange range) {
		this.range = range;
		this.excluded = new HashSet<>(range.diff() + 1);
	}

	public ExcludingRange(int min, int max) {
		this(IntegerRange.of(min, max));
	}

	public static ExcludingRange of(IntegerRange range) {
		return new ExcludingRange(range);
	}

	public static ExcludingRange of(int min, int max) {
		return new ExcludingRange(min, max);
	}

	//================================================================================
	// Overridden Methods
	//================================================================================
	@Override
	public Iterator<Integer> iterator() {
		return new ExcludingIterator();
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * Adds the given value to the set of excluded values.
	 * <p>
	 * If it is out of range, nothing is done.
	 *
	 * @see IntegerRange#inRangeOf(int, IntegerRange)
	 */
	public ExcludingRange exclude(int val) {
		if (IntegerRange.inRangeOf(val, range)) {
			excluded.add(val);
		}
		return this;
	}

	/**
	 * Iterates over the given values and delegates to {@link #exclude(int)}
	 */
	public ExcludingRange excludeAll(int... vals) {
		for (int val : vals) exclude(val);
		return this;
	}

	/**
	 * Iterates over the given range and delegates to {@link #exclude(int)}
	 */
	public ExcludingRange excludeAll(IntegerRange range) {
		range.forEach(this::exclude);
		return this;
	}

	/**
	 * @return whether the given value was excluded or outside the starting range
	 */
	public boolean isExcluded(int val) {
		return excluded.contains(val) || !IntegerRange.inRangeOf(val, range);
	}

	/**
	 * @return the set of excluded values, unmodifiable
	 */
	public Set<Integer> getExcluded() {
		return Collections.unmodifiableSet(excluded);
	}

	//================================================================================
	// Internal Classes
	//================================================================================

	/**
	 * A very simple iterator loop on the elements of an {@link ExcludingRange}.
	 * <p>
	 * Of course, it takes into account the values that were excluded from the range, {@link ExcludingRange#isExcluded(int)}.
	 */
	private class ExcludingIterator implements Iterator<Integer> {
		private int current = range.getMin();

		@Override
		public boolean hasNext() {
			while (current <= range.getMax() && isExcluded(current)) current++;
			return current <= range.getMax();
		}

		@Override
		public Integer next() {
			if (!hasNext()) throw new NoSuchElementException();
			return current++;
		}
	}
}
