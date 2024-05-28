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

package misc;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.virtualizedfx.utils.ExcludingRange;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

public class ExcludingRangeTests {

	@Test
	public void testIterator1() {
		ExcludingRange range = new ExcludingRange(3, 7);
		range.excludeAll(5, 7);
		Iterator<Integer> iterator = range.iterator();

		assertTrue(iterator.hasNext());
		assertEquals(3, iterator.next().intValue());
		assertTrue(iterator.hasNext());
		assertEquals(4, iterator.next().intValue());
		assertTrue(iterator.hasNext());
		assertEquals(6, iterator.next().intValue());
		assertFalse(iterator.hasNext());
		assertThrows(NoSuchElementException.class, iterator::next);
	}

	@Test
	public void testIterator2() {
		ExcludingRange range = new ExcludingRange(3, 7);
		range.excludeAll(3, 5);
		Iterator<Integer> iterator = range.iterator();

		assertTrue(iterator.hasNext());
		assertEquals(4, iterator.next().intValue());
		assertTrue(iterator.hasNext());
		assertEquals(6, iterator.next().intValue());
		assertTrue(iterator.hasNext());
		assertEquals(7, iterator.next().intValue());
		assertFalse(iterator.hasNext());
		assertThrows(NoSuchElementException.class, iterator::next);
	}

	@Test
	public void testIteratorWithNoExclusions() {
		ExcludingRange range = new ExcludingRange(3, 7);
		Iterator<Integer> iterator = range.iterator();

		assertTrue(iterator.hasNext());
		assertEquals(3, iterator.next().intValue());
		assertTrue(iterator.hasNext());
		assertEquals(4, iterator.next().intValue());
		assertTrue(iterator.hasNext());
		assertEquals(5, iterator.next().intValue());
		assertTrue(iterator.hasNext());
		assertEquals(6, iterator.next().intValue());
		assertTrue(iterator.hasNext());
		assertEquals(7, iterator.next().intValue());
		assertFalse(iterator.hasNext());
	}

	@Test
	public void testIteratorWithAllExclusions1() {
		ExcludingRange range = new ExcludingRange(3, 7);
		range.excludeAll(IntegerRange.of(3, 7));
		Iterator<Integer> iterator = range.iterator();

		assertFalse(iterator.hasNext());
		assertThrows(NoSuchElementException.class, iterator::next);
	}

	@Test
	public void testIteratorWithAllExclusions2() {
		ExcludingRange range = new ExcludingRange(0, 0);
		range.exclude(0);
		Iterator<Integer> iterator = range.iterator();

		assertFalse(iterator.hasNext());
		assertThrows(NoSuchElementException.class, iterator::next);
	}

	@Test
	public void testIteratorWithOutOfRangeExclusions() {
		ExcludingRange range = new ExcludingRange(3, 7);
		range.excludeAll(2, 8);
		Iterator<Integer> iterator = range.iterator();

		assertTrue(iterator.hasNext());
		assertEquals(3, iterator.next().intValue());
		assertTrue(iterator.hasNext());
		assertEquals(4, iterator.next().intValue());
		assertTrue(iterator.hasNext());
		assertEquals(5, iterator.next().intValue());
		assertTrue(iterator.hasNext());
		assertEquals(6, iterator.next().intValue());
		assertTrue(iterator.hasNext());
		assertEquals(7, iterator.next().intValue());
		assertFalse(iterator.hasNext());
	}
}
